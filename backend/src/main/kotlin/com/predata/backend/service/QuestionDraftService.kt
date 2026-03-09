package com.predata.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.config.properties.QuestionCreditProperties
import com.predata.backend.config.properties.VoteProperties
import com.predata.backend.domain.*
import com.predata.backend.domain.policy.DraftAccessResult
import com.predata.backend.domain.policy.DraftLifecyclePolicy
import com.predata.backend.domain.policy.DraftQuestionPolicy
import com.predata.backend.domain.policy.DraftSubmitStateResult
import com.predata.backend.domain.policy.QuestionContentPolicy
import com.predata.backend.dto.CreditStatusResponse
import com.predata.backend.dto.DraftOpenResponse
import com.predata.backend.dto.DraftSubmitResponse
import com.predata.backend.dto.SubmitQuestionDraftRequest
import com.predata.backend.exception.*
import com.predata.backend.repository.QuestionCreditAccountRepository
import com.predata.backend.repository.QuestionCreditLedgerRepository
import com.predata.backend.repository.QuestionDraftSessionRepository
import com.predata.backend.repository.QuestionRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.math.BigDecimal
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@Service
class QuestionDraftService(
    private val creditAccountRepository: QuestionCreditAccountRepository,
    private val creditLedgerRepository: QuestionCreditLedgerRepository,
    private val draftSessionRepository: QuestionDraftSessionRepository,
    private val questionRepository: QuestionRepository,
    private val redisTemplate: StringRedisTemplate,
    private val creditProperties: QuestionCreditProperties,
    private val voteProperties: VoteProperties,
    private val objectMapper: ObjectMapper,
    private val categoryValidationService: CategoryValidationService,
    private val questionQuotaService: QuestionQuotaService,
    private val questionContentValidationService: QuestionContentValidationService,
) {
    private val logger = LoggerFactory.getLogger(QuestionDraftService::class.java)

    // ── Credit Status ────────────────────────────────────────────────────────

    fun getCreditStatus(memberId: Long): CreditStatusResponse {
        val cacheKey = "qcredit:status:$memberId"
        val cached = tryRedisGet { redisTemplate.opsForValue().get(cacheKey)?.toIntOrNull() }

        val credits = cached ?: run {
            val dbCredits = getOrCreateAccount(memberId).availableCredits
            tryRedis {
                redisTemplate.opsForValue().set(cacheKey, dbCredits.toString(), 15, TimeUnit.SECONDS)
            }
            dbCredits
        }
        val account = creditAccountRepository.findByMemberId(memberId)
        val yearlyBudget = account?.yearlyBudget ?: creditProperties.yearlyBudget
        val usedCredits = (yearlyBudget - credits).coerceAtLeast(0)
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val nextReset = LocalDateTime.of(now.year + 1, 1, 1, 0, 0)

        return CreditStatusResponse(
            yearlyBudget = yearlyBudget,
            usedCredits = usedCredits,
            availableCredits = credits,
            requiredCredits = 1,
            requiredCreditsByVoteWindow = mapOf(
                "H6" to 1,
                "D1" to 1,
                "D3" to 3,
            ),
            resetAtUtc = nextReset.toInstant(ZoneOffset.UTC).toString(),
        )
    }

    // ── Draft Open ───────────────────────────────────────────────────────────

    @Transactional
    fun openDraft(memberId: Long): DraftOpenResponse {
        checkRateLimit("qcreate:rl:open:$memberId", creditProperties.draftRateLimitPerMinute)

        val now = LocalDateTime.now(ZoneOffset.UTC)

        // 기존 OPEN draft 만료 — active_member_id UNIQUE 슬롯 먼저 해제
        // saveAndFlush로 즉시 DB에 반영해야 INSERT 전에 슬롯이 비워짐
        val previousDraft = draftSessionRepository.findByActiveMemberId(memberId)
        previousDraft?.let { prev ->
            prev.status = DraftStatus.EXPIRED
            prev.activeMemberId = null   // UNIQUE 슬롯 해제
            prev.updatedAt = now
            draftSessionRepository.saveAndFlush(prev)
        }

        val draftId = UUID.randomUUID().toString()
        val idempotencyKey = UUID.randomUUID().toString()
        val expiresAt = now.plusMinutes(creditProperties.draftExpireMinutes)

        val draft = QuestionDraftSession(
            draftId = draftId,
            memberId = memberId,
            activeMemberId = memberId,   // UNIQUE 슬롯 점유 — DB가 동시 OPEN 차단
            status = DraftStatus.OPEN,
            expiresAt = expiresAt,
            submitIdempotencyKey = idempotencyKey,
            createdAt = now,
            updatedAt = now,
        )

        // saveAndFlush: UNIQUE 위반 시 즉시 DataIntegrityViolationException 발생
        val savedDraft = try {
            draftSessionRepository.saveAndFlush(draft)
        } catch (e: DataIntegrityViolationException) {
            throw ConflictException("이미 진행 중인 질문 작성이 있습니다. 잠시 후 다시 시도해 주세요.")
        }

        // 커밋 후 Redis 캐시 (실패 허용)
        registerAfterCommit {
            previousDraft?.let { tryRedis { redisTemplate.delete("qdraft:${it.draftId}") } }
            tryRedis {
                redisTemplate.opsForValue().set(
                    "qdraft:${savedDraft.draftId}",
                    "$memberId:OPEN:$expiresAt",
                    creditProperties.draftExpireMinutes, TimeUnit.MINUTES,
                )
            }
        }

        return DraftOpenResponse(
            draftId = savedDraft.draftId,
            expiresAt = expiresAt.toInstant(ZoneOffset.UTC).toString(),
            submitIdempotencyKey = idempotencyKey,
        )
    }

    // ── Draft Submit ─────────────────────────────────────────────────────────

    @Transactional
    fun submitDraft(
        memberId: Long,
        draftId: String,
        idempotencyKey: String,
        request: SubmitQuestionDraftRequest,
    ): DraftSubmitResponse {
        checkRateLimit("qcreate:rl:submit:$memberId", creditProperties.submitRateLimitPerMinute)
        validateSubmitRequest(request)

        // 1. Redis idempotency 선체크 (빠른 경로 — TTL 10분)
        val idemCacheKey = "qsubmit:idem:$memberId:$idempotencyKey"
        val cachedResult = tryRedisGet { redisTemplate.opsForValue().get(idemCacheKey) }
        if (cachedResult != null) {
            return objectMapper.readValue(cachedResult, DraftSubmitResponse::class.java)
        }

        // 2. Draft 사전 유효성 확인 (Redis → DB fallback, 락 없는 빠른 검증)
        validateDraftAccess(memberId, draftId)

        // 3. 제목 정규화 해시 계산
        val normalizedHash = "${normalizeTitle(request.title)}:${request.category}"

        // 4. Draft 락 획득 (3s timeout → 409)
        val draft = acquireDraftLock(draftId)

        // 5. 락 내 소유권/키 검증
        validateDraftOwnershipAndKey(draft, memberId, idempotencyKey)

        // 6. 장애 복구: 이미 committed된 submit 재시도 감지
        draft.submittedQuestionId?.let { existingId ->
            return buildIdempotentResponse(existingId, memberId)
        }

        // 7. 락 내 draft 상태 재검증
        validateDraftStateForSubmit(draft)

        // 8. 크레딧 계정 락 획득 (3s timeout → 409)
        val creditAccount = acquireCreditLock(memberId)

        // 8-1. 생성 제한 검증 (UTC 하루 1개 + active 질문 잠금)
        questionQuotaService.validateCanCreate(memberId)

        // 9. 차감 크레딧 계산 및 잔액 확인
        val requiredCredits = DraftQuestionPolicy.requiredCredits(request.voteWindowType)
        if (creditAccount.availableCredits < requiredCredits) {
            throw ConflictException(
                ErrorCode.INSUFFICIENT_YEARLY_CREDITS.message,
                ErrorCode.INSUFFICIENT_YEARLY_CREDITS.name,
            )
        }

        // 10. 크레딧 차감
        creditAccount.availableCredits -= requiredCredits
        creditAccount.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        creditAccountRepository.save(creditAccount)

        // 11. 질문 생성
        val question = buildQuestion(request, normalizedHash, memberId)
        val savedQuestion = try {
            questionRepository.save(question)
        } catch (e: DataIntegrityViolationException) {
            throw ConflictException(
                ErrorCode.DUPLICATE_QUESTION.message,
                ErrorCode.DUPLICATE_QUESTION.name,
            )
        }

        // 12. 원장 기록 (questionId 확정 후)
        creditLedgerRepository.save(
            QuestionCreditLedger(
                memberId = memberId,
                questionId = savedQuestion.id,
                delta = -requiredCredits,
                reason = CreditLedgerReason.QUESTION_CREATED,
                balanceAfter = creditAccount.availableCredits,
            )
        )

        // 13. Draft CONSUMED 처리 — activeMemberId=null로 UNIQUE 슬롯 해제
        val now = LocalDateTime.now(ZoneOffset.UTC)
        draft.status = DraftStatus.CONSUMED
        draft.activeMemberId = null
        draft.submittedQuestionId = savedQuestion.id
        draft.consumedAt = now
        draft.updatedAt = now
        try {
            // optimistic lock 충돌 시 전체 트랜잭션 롤백되어 중복 차감/중복 질문 생성 방지
            draftSessionRepository.saveAndFlush(draft)
        } catch (e: ObjectOptimisticLockingFailureException) {
            throw ConflictException(ErrorCode.CREDIT_LOCK_TIMEOUT.message, ErrorCode.CREDIT_LOCK_TIMEOUT.name)
        }

        val response = DraftSubmitResponse(
            questionId = savedQuestion.id!!,
            remainingCredits = creditAccount.availableCredits,
            usedCredits = requiredCredits,
            votingEndAt = question.votingEndAt.toInstant(ZoneOffset.UTC).toString(),
        )

        // 14. 커밋 후 Redis 업데이트 (실패 허용)
        registerAfterCommit {
            tryRedis {
                redisTemplate.opsForValue().set(
                    idemCacheKey,
                    objectMapper.writeValueAsString(response),
                    10, TimeUnit.MINUTES,
                )
            }
            tryRedis { redisTemplate.delete("qdraft:$draftId") }
            tryRedis { redisTemplate.delete("qcredit:status:$memberId") }
        }

        return response
    }

    // ── Draft Cancel ─────────────────────────────────────────────────────────

    @Transactional
    fun cancelDraft(memberId: Long, draftId: String) {
        val draft = draftSessionRepository.findByDraftId(draftId) ?: return

        if (draft.memberId != memberId) {
            throw ConflictException(ErrorCode.DRAFT_NOT_FOUND.message, ErrorCode.DRAFT_NOT_FOUND.name)
        }
        if (draft.status != DraftStatus.OPEN) return // 이미 닫힘 — 멱등 처리

        val now = LocalDateTime.now(ZoneOffset.UTC)
        draft.status = DraftStatus.CANCELLED
        draft.activeMemberId = null   // UNIQUE 슬롯 해제
        draft.updatedAt = now
        draftSessionRepository.save(draft)

        registerAfterCommit {
            tryRedis { redisTemplate.delete("qdraft:$draftId") }
        }
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private fun checkRateLimit(key: String, limit: Int) {
        val epochMinute = Instant.now().epochSecond / 60
        val bucketKey = "$key:$epochMinute"
        val count = tryRedisGet {
            val c = redisTemplate.opsForValue().increment(bucketKey) ?: 0L
            if (c == 1L) redisTemplate.expire(bucketKey, 60, TimeUnit.SECONDS)
            c
        } ?: return // Redis 장애 시 rate-limit 스킵 (가용성 우선)

        if (count > limit) {
            throw RateLimitException()
        }
    }

    private fun validateSubmitRequest(request: SubmitQuestionDraftRequest) {
        questionContentValidationService.validateTitle(request.title)
        QuestionContentPolicy.validateDescription(request.description)?.let { throw BadRequestException(it) }
        QuestionContentPolicy.validateThumbnailUrl(request.thumbnailUrl)?.let { throw BadRequestException(it) }
        QuestionContentPolicy.validateTags(request.tags)?.let { throw BadRequestException(it) }
        QuestionContentPolicy.validateSourceLinks(request.sourceLinks)?.let { throw BadRequestException(it) }
        if (request.settlementMode == SettlementMode.OBJECTIVE_RULE) {
            if (request.resolutionRule.isNullOrBlank()) {
                throw BadRequestException("OBJECTIVE_RULE 정산 방식에는 resolutionRule이 필수입니다.")
            }
            questionContentValidationService.validateResolutionRule(request.resolutionRule)
            if (request.resolutionSource.isNullOrBlank()) {
                throw BadRequestException("OBJECTIVE_RULE 정산 방식에는 resolutionSource가 필수입니다.")
            }
        }
        if (request.voteWindowType !in setOf(VoteWindowType.H6, VoteWindowType.D1, VoteWindowType.D3)) {
            throw BadRequestException("투표 기간은 H6, D1, D3만 지원합니다.")
        }
        QuestionContentPolicy.validateCreatorSplitInPool(request.creatorSplitInPool)
            ?.let { throw BadRequestException(it) }
    }

    private fun validateDraftAccess(memberId: Long, draftId: String) {
        // Redis 빠른 경로
        val redisDraft = tryRedisGet { redisTemplate.opsForValue().get("qdraft:$draftId") }
        if (redisDraft != null) {
            val ownerMemberId = redisDraft.split(":").firstOrNull()?.toLongOrNull()
            if (ownerMemberId != null && ownerMemberId != memberId) {
                throw ConflictException(ErrorCode.DRAFT_NOT_FOUND.message, ErrorCode.DRAFT_NOT_FOUND.name)
            }
            return
        }

        // DB fallback
        val draft = draftSessionRepository.findByDraftId(draftId)
            ?: throw ConflictException(ErrorCode.DRAFT_NOT_FOUND.message, ErrorCode.DRAFT_NOT_FOUND.name)

        when (
            DraftLifecyclePolicy.evaluateAccess(
                ownerMatches = draft.memberId == memberId,
                status = draft.status,
                expiresAt = draft.expiresAt,
                now = LocalDateTime.now(ZoneOffset.UTC),
            )
        ) {
            DraftAccessResult.OK -> Unit
            DraftAccessResult.NOT_FOUND -> {
                throw ConflictException(ErrorCode.DRAFT_NOT_FOUND.message, ErrorCode.DRAFT_NOT_FOUND.name)
            }
            DraftAccessResult.ALREADY_CONSUMED -> {
                throw ConflictException(ErrorCode.DRAFT_ALREADY_CONSUMED.message, ErrorCode.DRAFT_ALREADY_CONSUMED.name)
            }
            DraftAccessResult.EXPIRED -> {
                throw ConflictException(ErrorCode.DRAFT_EXPIRED.message, ErrorCode.DRAFT_EXPIRED.name)
            }
        }
    }

    private fun validateDraftOwnershipAndKey(draft: QuestionDraftSession, memberId: Long, idempotencyKey: String) {
        if (draft.memberId != memberId) {
            throw ConflictException(ErrorCode.DRAFT_NOT_FOUND.message, ErrorCode.DRAFT_NOT_FOUND.name)
        }
        // open 시 발급한 키와 일치해야 submit 가능 — 타 draft의 idempotency key 도용 방지
        if (!DraftLifecyclePolicy.ownershipAndKeyValid(draft.memberId == memberId, draft.submitIdempotencyKey == idempotencyKey)) {
            throw BadRequestException("Idempotency key does not match the draft.")
        }
    }

    private fun validateDraftStateForSubmit(draft: QuestionDraftSession) {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        when (DraftLifecyclePolicy.evaluateSubmitState(draft.status, draft.expiresAt, now)) {
            DraftSubmitStateResult.OK -> Unit
            DraftSubmitStateResult.ALREADY_CONSUMED -> {
                throw ConflictException(ErrorCode.DRAFT_ALREADY_CONSUMED.message, ErrorCode.DRAFT_ALREADY_CONSUMED.name)
            }
            DraftSubmitStateResult.EXPIRED_OR_INVALID -> {
                throw ConflictException(ErrorCode.DRAFT_EXPIRED.message, ErrorCode.DRAFT_EXPIRED.name)
            }
            DraftSubmitStateResult.EXPIRED_NEEDS_CLOSE -> {
                draft.status = DraftStatus.EXPIRED
                draft.activeMemberId = null   // UNIQUE 슬롯 해제
                draft.updatedAt = now
                draftSessionRepository.save(draft)
                throw ConflictException(ErrorCode.DRAFT_EXPIRED.message, ErrorCode.DRAFT_EXPIRED.name)
            }
        }
    }

    private fun acquireDraftLock(draftId: String): QuestionDraftSession =
        try {
            draftSessionRepository.findByDraftIdWithLock(draftId)
                ?: throw ConflictException(ErrorCode.DRAFT_NOT_FOUND.message, ErrorCode.DRAFT_NOT_FOUND.name)
        } catch (e: PessimisticLockingFailureException) {
            throw ConflictException(ErrorCode.CREDIT_LOCK_TIMEOUT.message, ErrorCode.CREDIT_LOCK_TIMEOUT.name)
        }

    private fun acquireCreditLock(memberId: Long): QuestionCreditAccount =
        try {
            val locked = creditAccountRepository.findByMemberIdWithLock(memberId)
            val account = locked ?: run {
                creditAccountRepository.save(
                    QuestionCreditAccount(
                        memberId = memberId,
                        availableCredits = creditProperties.yearlyBudget,
                        yearlyBudget = creditProperties.yearlyBudget,
                        lastResetAt = LocalDateTime.now(ZoneOffset.UTC),
                    )
                )
            }
            applyAnnualResetIfNeeded(account)
        } catch (e: PessimisticLockingFailureException) {
            throw ConflictException(ErrorCode.CREDIT_LOCK_TIMEOUT.message, ErrorCode.CREDIT_LOCK_TIMEOUT.name)
        }

    private fun buildIdempotentResponse(questionId: Long, memberId: Long): DraftSubmitResponse {
        val account = creditAccountRepository.findByMemberId(memberId)
        val question = questionRepository.findById(questionId).orElse(null)
        return DraftSubmitResponse(
            questionId = questionId,
            remainingCredits = account?.availableCredits ?: 0,
            usedCredits = 1,
            votingEndAt = question?.votingEndAt?.toInstant(ZoneOffset.UTC)?.toString() ?: "",
        )
    }

    @Suppress("DEPRECATION")
    private fun buildQuestion(
        request: SubmitQuestionDraftRequest,
        normalizedHash: String,
        memberId: Long,
    ): Question {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val isVoteResult = request.settlementMode == SettlementMode.VOTE_RESULT
        val votingDuration = DraftQuestionPolicy.votingDuration(request.voteWindowType)
        val votingEndAt = now.plus(votingDuration)

        // 두 트랙 모두 투표 종료 후 5분 break → 베팅 시작
        val bettingStartAt = votingEndAt.plusMinutes(5)
        val bettingEndAt = bettingStartAt.plus(DraftQuestionPolicy.maxBettingDuration(request.voteWindowType))

        // VOTE_RESULT: 베팅 기간 = reveal 기간. 베팅 종료 시 투표 결과 공개 + 정산 동시 트리거
        // OBJECTIVE_RULE: reveal 없음 (외부 판정 기반 정산)
        val revealWindowEndAt: LocalDateTime? = if (isVoteResult) bettingEndAt else null
        val feeSplit = DraftQuestionPolicy.feeSplit(request.creatorSplitInPool)

        return Question(
            title = request.title,
            category = categoryValidationService.normalizeAndValidate(request.category),
            status = QuestionStatus.VOTING,
            type = if (isVoteResult) QuestionType.OPINION else QuestionType.VERIFIABLE,
            marketType = if (isVoteResult) MarketType.OPINION else MarketType.VERIFIABLE,
            voteResultSettlement = isVoteResult,
            resolutionRule = if (isVoteResult)
                "투표 마감 시점의 다수결 결과로 정산됩니다."
            else
                request.resolutionRule!!,
            resolutionSource = request.resolutionSource,
            description = request.description?.trim()?.takeIf { it.isNotBlank() },
            thumbnailUrl = request.thumbnailUrl?.trim()?.takeIf { it.isNotBlank() },
            tagsJson = if (request.tags.isNotEmpty()) objectMapper.writeValueAsString(request.tags.map { it.trim().lowercase() }) else null,
            sourceLinksJson = if (request.sourceLinks.isNotEmpty()) objectMapper.writeValueAsString(request.sourceLinks.map { it.trim() }) else null,
            boostEnabled = request.boostEnabled,
            votingEndAt = votingEndAt,
            voteWindowType = request.voteWindowType,
            revealWindowEndAt = revealWindowEndAt,
            bettingStartAt = bettingStartAt,
            bettingEndAt = bettingEndAt,
            expiredAt = bettingEndAt,
            votingPhase = VotingPhase.VOTING_COMMIT_OPEN,
            executionModel = ExecutionModel.AMM_FPMM,
            creatorMemberId = memberId,
            questionNormalizedHash = normalizedHash,
            platformFeeShare = feeSplit.platformFeeShare,
            creatorFeeShare = feeSplit.creatorFeeShare,
            voterFeeShare = feeSplit.voterFeeShare,
            creatorSplitInPool = request.creatorSplitInPool,
        )
    }

    /** title 정규화: trim + 소문자 + 연속 공백 단일화 */
    private fun normalizeTitle(title: String): String =
        title.trim().lowercase().replace(Regex("\\s+"), " ")

    /** 커밋 후 실행할 콜백 등록 */
    private fun registerAfterCommit(action: () -> Unit) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() = action()
        })
    }

    private fun tryRedis(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.warn("Redis operation failed (fallback to DB): ${e.message}")
        }
    }

    private fun <T> tryRedisGet(block: () -> T?): T? =
        try {
            block()
        } catch (e: Exception) {
            logger.warn("Redis read failed (fallback to DB): ${e.message}")
            null
        }

    private fun getOrCreateAccount(memberId: Long): QuestionCreditAccount {
        val existing = creditAccountRepository.findByMemberId(memberId)
        if (existing != null) return applyAnnualResetIfNeeded(existing)
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return creditAccountRepository.save(
            QuestionCreditAccount(
                memberId = memberId,
                availableCredits = creditProperties.yearlyBudget,
                yearlyBudget = creditProperties.yearlyBudget,
                lastResetAt = now,
                updatedAt = now,
            )
        )
    }

    private fun applyAnnualResetIfNeeded(account: QuestionCreditAccount): QuestionCreditAccount {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val accountYear = account.lastResetAt.atOffset(ZoneOffset.UTC).year
        val currentYear = now.atOffset(ZoneOffset.UTC).year
        if (accountYear >= currentYear) return account
        account.availableCredits = account.yearlyBudget
        account.lastResetAt = now.truncatedTo(ChronoUnit.SECONDS)
        account.updatedAt = now
        return creditAccountRepository.save(account)
    }

}
