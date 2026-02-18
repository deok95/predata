package com.predata.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.predata.backend.domain.ExecutionModel
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.MarketType
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionGenerationBatch
import com.predata.backend.domain.QuestionGenerationItem
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.QuestionType
import com.predata.backend.dto.BatchGenerateQuestionsRequest
import com.predata.backend.dto.BatchGenerateQuestionsResponse
import com.predata.backend.dto.GeneratedQuestionDraftDto
import com.predata.backend.dto.LlmQuestionGenerationRequest
import com.predata.backend.dto.PublishGeneratedBatchRequest
import com.predata.backend.dto.PublishResultResponse
import com.predata.backend.dto.RetryFailedGenerationRequest
import com.predata.backend.dto.amm.SeedPoolRequest
import com.predata.backend.repository.QuestionGenerationBatchRepository
import com.predata.backend.repository.QuestionGenerationItemRepository
import com.predata.backend.repository.QuestionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class AutoQuestionGenerationService(
    private val settingsService: QuestionGenerationSettingsService,
    private val trendSignalService: GoogleTrendSignalService,
    private val openAiBatchQuestionService: OpenAiBatchQuestionService,
    private val draftValidator: QuestionDraftValidator,
    private val batchRepository: QuestionGenerationBatchRepository,
    private val itemRepository: QuestionGenerationItemRepository,
    private val questionRepository: QuestionRepository,
    private val blockchainService: BlockchainService,
    private val swapService: com.predata.backend.service.amm.SwapService
) {

    private val logger = LoggerFactory.getLogger(AutoQuestionGenerationService::class.java)
    private val objectMapper = ObjectMapper()

    companion object {
        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_REJECTED = "REJECTED"
        const val STATUS_PUBLISHED = "PUBLISHED"
        const val STATUS_FAILED = "FAILED"
    }

    @Transactional
    fun generateBatch(request: BatchGenerateQuestionsRequest): BatchGenerateQuestionsResponse {
        settingsService.saveDefaultsIfMissing()
        val settings = settingsService.get()
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val targetDate = request.targetDate ?: LocalDate.now(ZoneOffset.UTC)
        val subcategory = normalizeSubcategory(request.subcategory, settings)

        val existing = batchRepository.findBySubcategoryAndTargetDate(subcategory, targetDate)
        if (existing != null && !request.dryRun) {
            // 이미 게시된 배치
            if (existing.status == STATUS_PUBLISHED) {
                return toResponse(existing, itemRepository.findByBatchId(existing.batchId), "이미 생성/게시된 배치입니다.")
            }
            // 기존 배치에 아이템이 이미 있으면 재생성 거부 (멱등성 보장)
            val existingItems = itemRepository.findByBatchId(existing.batchId)
            if (existingItems.isNotEmpty()) {
                throw IllegalStateException("이미 생성된 배치입니다. retry를 사용하세요 (batchId: ${existing.batchId})")
            }
        }

        val batch = if (request.dryRun) {
            // dryRun 모드: DB 저장하지 않고 메모리에서만 배치 생성
            QuestionGenerationBatch(
                batchId = UUID.randomUUID().toString().replace("-", ""),
                subcategory = subcategory,
                targetDate = targetDate,
                status = STATUS_DRAFT,
                requestedCount = 0,
                acceptedCount = 0,
                rejectedCount = 0,
                dryRun = true,
                createdAt = now,
                updatedAt = now
            )
        } else {
            existing ?: batchRepository.save(
                QuestionGenerationBatch(
                    batchId = UUID.randomUUID().toString().replace("-", ""),
                    subcategory = subcategory,
                    targetDate = targetDate,
                    status = STATUS_DRAFT,
                    requestedCount = 0,
                    acceptedCount = 0,
                    rejectedCount = 0,
                    dryRun = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val persistSummary = generateDrafts(batch, settings, targetDate, request.dryRun)

        // 검증 실패 배치 게시 차단: 정확히 VERIFIABLE 2개 + OPINION 1개 = 3개 통과해야 게시
        val requiredTotal = settings.dailyCount
        val requiredOpinion = settings.opinionCount
        val requiredVerifiable = requiredTotal - requiredOpinion

        val publishMessage = if (!request.dryRun && persistSummary.acceptedDraftIds.isNotEmpty()) {
            val acceptedCount = persistSummary.acceptedDraftIds.size
            val opinionCount = persistSummary.opinionCount
            val verifiableCount = persistSummary.verifiableCount

            if (acceptedCount == requiredTotal && opinionCount == requiredOpinion && verifiableCount == requiredVerifiable) {
                // 검증 통과: 모든 질문 게시
                publishBatchInternal(
                    batchId = batch.batchId,
                    draftIds = persistSummary.acceptedDraftIds
                ).message
            } else {
                // 검증 실패: 게시 차단
                batch.status = "VALIDATION_FAILED"
                batch.message = "검증 실패: 필요 ${requiredTotal}개(OPINION ${requiredOpinion}, VERIFIABLE ${requiredVerifiable}), 실제 ${acceptedCount}개(OPINION ${opinionCount}, VERIFIABLE ${verifiableCount})"
                if (!request.dryRun) {
                    batchRepository.save(batch)
                }
                batch.message
            }
        } else {
            null
        }

        // dryRun 모드일 경우 메모리의 배치와 아이템 사용
        if (request.dryRun) {
            return toResponse(
                batch,
                persistSummary.items,
                "dryRun 모드: DB 저장 없이 초안만 생성"
            )
        }

        val refreshed = batchRepository.findByBatchId(batch.batchId)!!
        val items = itemRepository.findByBatchId(batch.batchId)

        return toResponse(
            refreshed,
            items,
            publishMessage ?: "배치 생성 완료"
        )
    }

    @Transactional(readOnly = true)
    fun getBatch(batchId: String): BatchGenerateQuestionsResponse {
        val batch = batchRepository.findByBatchId(batchId)
            ?: throw IllegalArgumentException("배치를 찾을 수 없습니다: $batchId")
        val items = itemRepository.findByBatchId(batchId)
        return toResponse(batch, items, batch.message)
    }

    @Transactional
    fun publishBatch(batchId: String, request: PublishGeneratedBatchRequest): PublishResultResponse {
        val batch = batchRepository.findByBatchId(batchId)
            ?: throw IllegalArgumentException("배치를 찾을 수 없습니다: $batchId")

        // 수동 publish API 부분 게시 차단: VERIFIABLE 2개 + OPINION 1개 = 3개 필수
        val settings = settingsService.get()
        val requiredTotal = settings.dailyCount
        val requiredOpinion = settings.opinionCount
        val requiredVerifiable = requiredTotal - requiredOpinion

        val draftsToPublish = itemRepository.findByBatchIdAndDraftIdIn(batchId, request.publishDraftIds)
            .filter { it.status == STATUS_DRAFT }

        val opinionCount = draftsToPublish.count { it.marketType == "OPINION" }
        val verifiableCount = draftsToPublish.count { it.marketType == "VERIFIABLE" }
        val totalCount = draftsToPublish.size

        // 검증 실패: 개수 미충족
        if (totalCount != requiredTotal || opinionCount != requiredOpinion || verifiableCount != requiredVerifiable) {
            batch.status = "VALIDATION_FAILED"
            batch.message = "게시 조건 미충족: VERIFIABLE ${requiredVerifiable}개 + OPINION ${requiredOpinion}개 필요, 실제 VERIFIABLE ${verifiableCount}개 + OPINION ${opinionCount}개"
            batchRepository.save(batch)
            throw IllegalArgumentException("게시 조건 미충족: VERIFIABLE ${requiredVerifiable}개 + OPINION ${requiredOpinion}개 필요")
        }

        return publishBatchInternal(batchId, request.publishDraftIds)
    }

    @Transactional
    fun retryBatch(batchId: String, request: RetryFailedGenerationRequest): BatchGenerateQuestionsResponse {
        val batch = batchRepository.findByBatchId(batchId)
            ?: throw IllegalArgumentException("배치를 찾을 수 없습니다: $batchId")

        val rejected = itemRepository.findByBatchIdAndStatus(batchId, STATUS_REJECTED)
        if (rejected.isEmpty() && !request.force) {
            return toResponse(batch, itemRepository.findByBatchId(batchId), "재시도 대상이 없습니다")
        }

        // 재시도 멱등성 강화: 기존 아이템 삭제 후 새로 생성
        val existingItems = itemRepository.findByBatchId(batchId)
        itemRepository.deleteAll(existingItems)

        // 카운트 초기화 (누적 대신 실제 아이템 수로 재계산)
        batch.requestedCount = 0
        batch.acceptedCount = 0
        batch.rejectedCount = 0

        val settings = settingsService.get()
        generateDrafts(batch, settings, batch.targetDate, batch.dryRun)

        if (!batch.dryRun) {
            val draftIds = itemRepository.findByBatchIdAndStatus(batchId, STATUS_DRAFT).map { it.draftId }
            if (draftIds.isNotEmpty()) {
                publishBatchInternal(batchId, draftIds)
            }
        }

        val refreshed = batchRepository.findByBatchId(batchId)!!
        return toResponse(refreshed, itemRepository.findByBatchId(batchId), "재시도 완료")
    }

    private fun generateDrafts(
        batch: QuestionGenerationBatch,
        settings: QuestionGenerationSettings,
        targetDate: LocalDate,
        dryRun: Boolean
    ): PersistSummary {
        val signals = trendSignalService.fetchAndStoreSignals(
            subcategory = batch.subcategory,
            targetDate = targetDate,
            region = settings.region,
            maxSignals = 10
        )

        val llmRequest = LlmQuestionGenerationRequest(
            subcategory = batch.subcategory,
            region = settings.region,
            signals = signals,
            targetDate = targetDate,
            requiredQuestionCount = settings.dailyCount,
            requiredOpinionCount = settings.opinionCount,
            minResolveHours = settings.votingHours + settings.breakMinutes / 60 + settings.bettingHours,
            minVotingHours = settings.votingHours,
            bettingHours = settings.bettingHours,
            breakMinutes = settings.breakMinutes
        )

        val generated = openAiBatchQuestionService.generateQuestions(batch.batchId, llmRequest)

        // 중복검사 최적화: 전체 조회 대신 최근 30일 질문만 조회
        val thirtyDaysAgo = LocalDateTime.now(ZoneOffset.UTC).minusDays(30)
        val existingTitles = questionRepository.findByCreatedAtAfter(thirtyDaysAgo).map { it.title }
        val validationOutput = draftValidator.validate(
            batchId = batch.batchId,
            questions = generated,
            requiredCount = settings.dailyCount,
            requiredOpinionCount = settings.opinionCount,
            duplicateThreshold = settings.duplicateThreshold,
            existingTitles = existingTitles
        )

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val votingEndAt = now.plusHours(settings.votingHours.toLong())
        val bettingStartAt = votingEndAt.plusMinutes(settings.breakMinutes.toLong())
        val bettingEndAt = bettingStartAt.plusHours(settings.bettingHours.toLong())
        val revealStartAt = bettingEndAt
        val revealEndAt = revealStartAt.plusMinutes(settings.revealMinutes.toLong())

        val acceptedItems = validationOutput.accepted.map { accepted ->
            val q = accepted.draft
            val riskFlags = mutableListOf<String>()
            if (q.confidence < 0.55) riskFlags += "LOW_CONFIDENCE"

            QuestionGenerationItem(
                batchId = batch.batchId,
                draftId = UUID.randomUUID().toString().replace("-", ""),
                title = q.title,
                category = normalizeCategory(batch.subcategory),
                subcategory = batch.subcategory,
                marketType = q.marketType,
                questionType = q.questionType,
                voteResultSettlement = q.voteResultSettlement,
                resolutionRule = q.resolutionRule,
                resolutionSource = q.resolutionSource,
                resolveAt = if (q.resolveAt.isBefore(bettingEndAt)) bettingEndAt else q.resolveAt,
                votingEndAt = votingEndAt,
                breakMinutes = settings.breakMinutes,
                bettingStartAt = bettingStartAt,
                bettingEndAt = bettingEndAt,
                revealStartAt = revealStartAt,
                revealEndAt = revealEndAt,
                confidence = BigDecimal.valueOf(q.confidence),
                duplicateScore = BigDecimal.valueOf(accepted.duplicateScore),
                rationale = q.rationale,
                referencesJson = if (q.references.isEmpty()) null else objectMapper.writeValueAsString(q.references),
                riskFlagsJson = if (riskFlags.isEmpty()) null else objectMapper.writeValueAsString(riskFlags),
                status = STATUS_DRAFT,
                createdAt = now,
                updatedAt = now
            )
        }

        val rejectedItems = validationOutput.rejected.map { (q, reason) ->
            QuestionGenerationItem(
                batchId = batch.batchId,
                draftId = UUID.randomUUID().toString().replace("-", ""),
                title = q.title,
                category = normalizeCategory(batch.subcategory),
                subcategory = batch.subcategory,
                marketType = q.marketType,
                questionType = q.questionType,
                voteResultSettlement = q.voteResultSettlement,
                resolutionRule = q.resolutionRule,
                resolutionSource = q.resolutionSource,
                resolveAt = q.resolveAt,
                votingEndAt = votingEndAt,
                breakMinutes = settings.breakMinutes,
                bettingStartAt = bettingStartAt,
                bettingEndAt = bettingEndAt,
                revealStartAt = revealStartAt,
                revealEndAt = revealEndAt,
                confidence = BigDecimal.valueOf(q.confidence),
                duplicateScore = BigDecimal.ONE,
                rationale = q.rationale,
                referencesJson = if (q.references.isEmpty()) null else objectMapper.writeValueAsString(q.references),
                riskFlagsJson = null,
                status = STATUS_REJECTED,
                rejectReason = reason,
                createdAt = now,
                updatedAt = now
            )
        }

        // dryRun 모드일 경우 DB 저장하지 않음
        val allItems = acceptedItems + rejectedItems
        val persisted = if (!dryRun) {
            itemRepository.saveAll(allItems)
        } else {
            allItems  // dryRun: DB 저장 skip, 메모리 리스트 반환
        }

        batch.requestedCount = generated.size
        batch.acceptedCount = acceptedItems.size
        batch.rejectedCount = rejectedItems.size
        batch.status = when {
            acceptedItems.isNotEmpty() -> STATUS_DRAFT
            rejectedItems.isNotEmpty() -> STATUS_FAILED
            else -> STATUS_FAILED
        }
        batch.message = if (validationOutput.validation.errors.isEmpty()) {
            "생성 완료"
        } else {
            validationOutput.validation.errors.joinToString(" | ")
        }
        batch.updatedAt = now
        if (!dryRun) {
            batchRepository.save(batch)
        }

        val acceptedDrafts = persisted.filter { it.status == STATUS_DRAFT }
        return PersistSummary(
            acceptedDraftIds = acceptedDrafts.map { it.draftId },
            accepted = acceptedItems.size,
            rejected = rejectedItems.size,
            items = persisted,  // dryRun 모드에서 메모리 아이템을 응답에 포함하기 위해
            opinionCount = acceptedDrafts.count { it.marketType == "OPINION" },
            verifiableCount = acceptedDrafts.count { it.marketType == "VERIFIABLE" }
        )
    }

    private fun publishBatchInternal(batchId: String, draftIds: List<String>): PublishResultResponse {
        val batch = batchRepository.findByBatchId(batchId)
            ?: throw IllegalArgumentException("배치를 찾을 수 없습니다: $batchId")

        val drafts = itemRepository.findByBatchIdAndDraftIdIn(batchId, draftIds)
            .filter { it.status == STATUS_DRAFT }
        val blockchainEnabled = blockchainService.getBlockchainStatus().enabled

        var published = 0
        var failed = 0
        drafts.forEach { draft ->
            try {
                // 재시도 시 중복 질문 생성 방지: draft에 questionId가 있으면 기존 질문 재사용
                // 신규 draft는 먼저 DB 저장해서 questionId 확보 후 온체인 게시
                val targetQuestion = if (draft.publishedQuestionId != null) {
                    questionRepository.findById(draft.publishedQuestionId!!).orElse(null)
                        ?: throw IllegalStateException("기존 질문을 찾을 수 없습니다: ${draft.publishedQuestionId}")
                } else {
                    val newQuestion = questionRepository.save(buildQuestionFromDraft(draft))

                    // 자동 시드 풀 생성 (실패해도 질문 생성은 유지)
                    try {
                        swapService.seedPool(
                            SeedPoolRequest(
                                questionId = newQuestion.id!!,
                                seedUsdc = BigDecimal("1000"), // 기본 1000 USDC
                                feeRate = BigDecimal("0.01")   // 1% 수수료
                            )
                        )
                        logger.info("[AutoQuestionGeneration] 풀 시드 성공 - questionId: ${newQuestion.id}")
                    } catch (e: Exception) {
                        logger.warn("[AutoQuestionGeneration] 풀 시드 실패 (질문은 생성됨) - questionId: ${newQuestion.id}, error: ${e.message}")
                    }

                    newQuestion
                }

                if (blockchainEnabled) {
                    // 블록체인 시도 (실패하면 exception) - 30초 타임아웃으로 동기화
                    val txHash = try {
                        blockchainService.createQuestionOnChain(targetQuestion)
                            .get(30, TimeUnit.SECONDS)
                    } catch (e: Exception) {
                        throw RuntimeException("블록체인 게시 실패: ${e.message}", e)
                    }

                    // txHash가 null이면 블록체인 트랜잭션 실패로 처리
                    if (txHash == null) {
                        targetQuestion.status = QuestionStatus.CANCELLED
                        questionRepository.save(targetQuestion)
                        throw RuntimeException("블록체인 트랜잭션 실패: txHash가 null입니다")
                    }
                    logger.debug("[AutoQuestionGeneration] 블록체인 게시 성공 - draftId: ${draft.draftId}, txHash: $txHash")
                } else {
                    logger.info(
                        "[AutoQuestionGeneration] 블록체인 비활성화 모드 - 오프체인 게시 처리 (draftId={})",
                        draft.draftId
                    )
                }

                draft.status = STATUS_PUBLISHED
                draft.publishedQuestionId = targetQuestion.id
                draft.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
                itemRepository.save(draft)
                published++
            } catch (e: Exception) {
                // 블록체인 실패 시: draft를 FAILED로 마킹, DB question은 저장하지 않음
                draft.status = STATUS_FAILED
                draft.rejectReason = "블록체인 게시 실패: ${e.message}"
                draft.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
                itemRepository.save(draft)
                failed++
                logger.error("[AutoQuestionGeneration] 블록체인 게시 실패 - draftId: ${draft.draftId}, error: ${e.message}", e)
            }
        }

        batch.status = if (failed == 0) STATUS_PUBLISHED else STATUS_DRAFT
        batch.message = "게시 완료: $published, 실패: $failed"
        batch.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        batchRepository.save(batch)

        return PublishResultResponse(
            success = failed == 0,
            batchId = batchId,
            publishedCount = published,
            failedCount = failed,
            message = batch.message
        )
    }

    private fun buildQuestionFromDraft(draft: QuestionGenerationItem): Question {
        val marketType = runCatching { MarketType.valueOf(draft.marketType.uppercase()) }
            .getOrDefault(MarketType.VERIFIABLE)
        val questionType = runCatching { QuestionType.valueOf(draft.questionType.uppercase()) }
            .getOrDefault(QuestionType.VERIFIABLE)

        return Question(
            title = draft.title,
            category = draft.category,
            categoryWeight = BigDecimal.ONE,
            status = QuestionStatus.VOTING,
            type = questionType,
            marketType = marketType,
            resolutionRule = draft.resolutionRule,
            resolutionSource = draft.resolutionSource,
            resolveAt = draft.resolveAt,
            disputeUntil = draft.revealEndAt,
            voteResultSettlement = draft.voteResultSettlement,
            votingEndAt = draft.votingEndAt,
            bettingStartAt = draft.bettingStartAt,
            bettingEndAt = draft.bettingEndAt,
            totalBetPool = 0,
            yesBetPool = 0,
            noBetPool = 0,
            initialYesPool = QuestionManagementService.INITIAL_LIQUIDITY,
            initialNoPool = QuestionManagementService.INITIAL_LIQUIDITY,
            finalResult = FinalResult.PENDING,
            expiredAt = draft.bettingEndAt,
            createdAt = LocalDateTime.now(ZoneOffset.UTC),
            executionModel = ExecutionModel.AMM_FPMM
        )
    }

    private fun toResponse(
        batch: QuestionGenerationBatch,
        items: List<QuestionGenerationItem>,
        message: String?
    ): BatchGenerateQuestionsResponse {
        val drafts = items.sortedBy { it.createdAt }.map { item ->
            GeneratedQuestionDraftDto(
                draftId = item.draftId,
                title = item.title,
                category = item.category,
                subcategory = item.subcategory,
                marketType = item.marketType,
                questionType = item.questionType,
                voteResultSettlement = item.voteResultSettlement,
                resolutionRule = item.resolutionRule,
                resolutionSource = item.resolutionSource,
                resolveAt = item.resolveAt,
                votingEndAt = item.votingEndAt,
                breakMinutes = item.breakMinutes,
                bettingStartAt = item.bettingStartAt,
                bettingEndAt = item.bettingEndAt,
                revealStartAt = item.revealStartAt,
                revealEndAt = item.revealEndAt,
                duplicateScore = item.duplicateScore.toDouble(),
                riskFlags = parseStringList(item.riskFlagsJson),
                status = item.status
            )
        }

        val opinionCount = drafts.count { it.marketType == "OPINION" }
        val verifiableCount = drafts.count { it.marketType == "VERIFIABLE" }

        return BatchGenerateQuestionsResponse(
            success = batch.status != STATUS_FAILED,
            batchId = batch.batchId,
            generatedAt = batch.createdAt,
            subcategory = batch.subcategory,
            requestedCount = batch.requestedCount,
            acceptedCount = batch.acceptedCount,
            rejectedCount = batch.rejectedCount,
            opinionCount = opinionCount,
            verifiableCount = verifiableCount,
            drafts = drafts,
            message = message
        )
    }

    private fun parseStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val node = objectMapper.readTree(json)
            if (!node.isArray) return@runCatching emptyList()
            node.map { it.asText() }
        }.getOrDefault(emptyList())
    }

    private fun normalizeSubcategory(subcategory: String?, settings: QuestionGenerationSettings): String {
        val input = subcategory?.trim()?.uppercase()
        if (!input.isNullOrBlank()) return input
        return settings.subcategories.firstOrNull() ?: "ECONOMY"
    }

    private fun normalizeCategory(subcategory: String): String {
        return subcategory.substringBefore(":").substringBefore("/").uppercase()
    }
}

data class PersistSummary(
    val acceptedDraftIds: List<String>,
    val accepted: Int,
    val rejected: Int,
    val items: List<QuestionGenerationItem> = emptyList(),
    val opinionCount: Int = 0,
    val verifiableCount: Int = 0
)
