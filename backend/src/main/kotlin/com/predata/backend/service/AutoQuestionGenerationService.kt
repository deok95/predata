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
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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
    private val swapService: com.predata.backend.service.amm.SwapService,
    @Value("\${anthropic.api.key:}") private val anthropicApiKey: String,
    @Value("\${gemini.api.key:}") private val geminiApiKey: String,
    @Value("\${gemini.api.model:gemini-1.5-flash}") private val geminiModel: String,
    @Value("\${app.questiongen.daily-trend-geo:US}") private val dailyTrendGeo: String,
    @Value("\${app.questiongen.max-generation-attempts:3}") private val maxGenerationAttempts: Int
) {

    private val logger = LoggerFactory.getLogger(AutoQuestionGenerationService::class.java)
    private val objectMapper = ObjectMapper()
    private val restTemplate = RestTemplate()

    companion object {
        private const val ANTHROPIC_URL = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_MODEL = "claude-sonnet-4-20250514"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_REJECTED = "REJECTED"
        const val STATUS_PUBLISHED = "PUBLISHED"
        const val STATUS_FAILED = "FAILED"
    }

    @Transactional
    fun generateDailyTrendQuestion(): com.predata.backend.dto.QuestionGenerationResponse {
        val trendKeyword = fetchTopGoogleTrend() ?: run {
            logger.warn("[DailyTrend] Google Trends unavailable. Falling back to static keyword.")
            "US macro economy"
        }

        if (anthropicApiKey.isBlank() && geminiApiKey.isBlank()) {
            val demoQuestion = buildDemoTrendQuestion(trendKeyword)
            return createAndSeedTrendQuestion(
                generatedTitle = demoQuestion.title,
                generatedResolutionRule = demoQuestion.resolutionRule,
                generatedDuration = demoQuestion.bettingDuration,
                trendKeyword = trendKeyword,
                isDemoMode = true
            )
        }

        val attempts = maxGenerationAttempts.coerceAtLeast(1)
        repeat(attempts) { attempt ->
            val generated = generateQuestionFromTrend(trendKeyword)
            if (generated == null) {
                logger.warn("[DailyTrend] generation failed on attempt {}/{}", attempt + 1, attempts)
                return@repeat
            }

            val validation = validateQuestionQuality(generated.title, generated.resolutionRule)
            if (!validation.valid) {
                logger.warn(
                    "[DailyTrend] validation failed on attempt {}/{}: {}",
                    attempt + 1,
                    attempts,
                    validation.reason
                )
                return@repeat
            }

            val temporalValidation = validateTemporalSanity(generated.title)
            if (!temporalValidation.valid) {
                logger.warn(
                    "[DailyTrend] temporal validation failed on attempt {}/{}: {}",
                    attempt + 1,
                    attempts,
                    temporalValidation.reason
                )
                return@repeat
            }

            val eventValidation = validateEventDrivenQuestion(generated.title, generated.resolutionRule)
            if (!eventValidation.valid) {
                logger.warn(
                    "[DailyTrend] event validation failed on attempt {}/{}: {}",
                    attempt + 1,
                    attempts,
                    eventValidation.reason
                )
                return@repeat
            }

            val recentDup = questionRepository.findByCreatedAtAfter(LocalDateTime.now(ZoneOffset.UTC).minusDays(7))
                .any { it.title.equals(generated.title, ignoreCase = true) }
            if (recentDup) {
                logger.warn("[DailyTrend] duplicate title skipped: {}", generated.title)
                return@repeat
            }

            return createAndSeedTrendQuestion(
                generatedTitle = generated.title,
                generatedResolutionRule = generated.resolutionRule,
                generatedDuration = generated.bettingDuration,
                trendKeyword = trendKeyword,
                isDemoMode = false
            )
        }

        val demoQuestion = buildDemoTrendQuestion(trendKeyword)
        return createAndSeedTrendQuestion(
            generatedTitle = demoQuestion.title,
            generatedResolutionRule = demoQuestion.resolutionRule,
            generatedDuration = demoQuestion.bettingDuration,
            trendKeyword = trendKeyword,
            isDemoMode = true
        )
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
                return toResponse(existing, itemRepository.findByBatchId(existing.batchId), "Batch already created/published.")
            }
            // 기존 배치에 아이템이 이미 있으면 재생성 거부 (멱등성 보장)
            val existingItems = itemRepository.findByBatchId(existing.batchId)
            if (existingItems.isNotEmpty()) {
                throw IllegalStateException("Batch already created. Use retry (batchId: ${existing.batchId})")
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
                batch.message = "Validation failed: Need ${requiredTotal} (OPINION ${requiredOpinion}, VERIFIABLE ${requiredVerifiable}), got ${acceptedCount} (OPINION ${opinionCount}, VERIFIABLE ${verifiableCount})"
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
                "dryRun mode: Drafts created without DB save"
            )
        }

        val refreshed = batchRepository.findByBatchId(batch.batchId)!!
        val items = itemRepository.findByBatchId(batch.batchId)

        return toResponse(
            refreshed,
            items,
            publishMessage ?: "Batch creation completed"
        )
    }

    @Transactional(readOnly = true)
    fun getBatch(batchId: String): BatchGenerateQuestionsResponse {
        val batch = batchRepository.findByBatchId(batchId)
            ?: throw IllegalArgumentException("Batch not found: $batchId")
        val items = itemRepository.findByBatchId(batchId)
        return toResponse(batch, items, batch.message)
    }

    @Transactional
    fun publishBatch(batchId: String, request: PublishGeneratedBatchRequest): PublishResultResponse {
        val batch = batchRepository.findByBatchId(batchId)
            ?: throw IllegalArgumentException("Batch not found: $batchId")

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
            batch.message = "Publishing requirements not met: Need VERIFIABLE ${requiredVerifiable} + OPINION ${requiredOpinion}, got VERIFIABLE ${verifiableCount} + OPINION ${opinionCount}"
            batchRepository.save(batch)
            throw IllegalArgumentException("Publishing requirements not met: Need VERIFIABLE ${requiredVerifiable} + OPINION ${requiredOpinion}")
        }

        return publishBatchInternal(batchId, request.publishDraftIds)
    }

    @Transactional
    fun retryBatch(batchId: String, request: RetryFailedGenerationRequest): BatchGenerateQuestionsResponse {
        val batch = batchRepository.findByBatchId(batchId)
            ?: throw IllegalArgumentException("Batch not found: $batchId")

        val rejected = itemRepository.findByBatchIdAndStatus(batchId, STATUS_REJECTED)
        if (rejected.isEmpty() && !request.force) {
            return toResponse(batch, itemRepository.findByBatchId(batchId), "No items to retry")
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
        return toResponse(refreshed, itemRepository.findByBatchId(batchId), "Retry completed")
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
            "Generation completed"
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
            ?: throw IllegalArgumentException("Batch not found: $batchId")

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
                        ?: throw IllegalStateException("Existing question not found: ${draft.publishedQuestionId}")
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
                        logger.info("[AutoQuestionGeneration] Pool seed successful - questionId: ${newQuestion.id}")
                    } catch (e: Exception) {
                        logger.warn("[AutoQuestionGeneration] Pool seed failed (question created) - questionId: ${newQuestion.id}, error: ${e.message}")
                    }

                    newQuestion
                }

                if (blockchainEnabled) {
                    // 블록체인 시도 (실패하면 exception) - 30초 타임아웃으로 동기화
                    val txHash = try {
                        blockchainService.createQuestionOnChain(targetQuestion)
                            .get(30, TimeUnit.SECONDS)
                    } catch (e: Exception) {
                        throw RuntimeException("Blockchain publishing failed: ${e.message}", e)
                    }

                    // txHash가 null이면 블록체인 트랜잭션 실패로 처리
                    if (txHash == null) {
                        targetQuestion.status = QuestionStatus.CANCELLED
                        questionRepository.save(targetQuestion)
                        throw RuntimeException("Blockchain transaction failed: txHash is null")
                    }
                    logger.debug("[AutoQuestionGeneration] Blockchain publishing successful - draftId: ${draft.draftId}, txHash: $txHash")
                } else {
                    logger.info(
                        "[AutoQuestionGeneration] Blockchain disabled mode - Offchain publishing (draftId={})",
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
                draft.rejectReason = "Blockchain publishing failed: ${e.message}"
                draft.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
                itemRepository.save(draft)
                failed++
                logger.error("[AutoQuestionGeneration] Blockchain publishing failed - draftId: ${draft.draftId}, error: ${e.message}", e)
            }
        }

        batch.status = if (failed == 0) STATUS_PUBLISHED else STATUS_DRAFT
        batch.message = "Publishing complete: $published published, $failed failed"
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

    private fun createAndSeedTrendQuestion(
        generatedTitle: String,
        generatedResolutionRule: String,
        generatedDuration: String?,
        trendKeyword: String,
        isDemoMode: Boolean
    ): com.predata.backend.dto.QuestionGenerationResponse {
        val bettingDuration = parseDuration(generatedDuration)
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val votingDuration = java.time.Duration.ofHours(24)
        val breakDuration = java.time.Duration.ofMinutes(5)

        val votingEndAt = now.plus(votingDuration)
        val bettingStartAt = votingEndAt.plus(breakDuration)
        val bettingEndAt = bettingStartAt.plus(bettingDuration)

        val question = Question(
            title = generatedTitle,
            category = "TRENDING",
            categoryWeight = BigDecimal.ONE,
            status = QuestionStatus.VOTING,
            type = QuestionType.VERIFIABLE,
            marketType = MarketType.VERIFIABLE,
            resolutionRule = generatedResolutionRule,
            resolutionSource = "GOOGLE_TRENDS_RSS",
            resolveAt = bettingEndAt,
            disputeUntil = bettingEndAt.plusHours(24),
            voteResultSettlement = false,
            votingEndAt = votingEndAt,
            bettingStartAt = bettingStartAt,
            bettingEndAt = bettingEndAt,
            totalBetPool = 0,
            yesBetPool = 0,
            noBetPool = 0,
            finalResult = FinalResult.PENDING,
            sourceUrl = "https://trends.google.com/trending/rss?geo=$dailyTrendGeo",
            expiredAt = bettingEndAt,
            createdAt = now,
            executionModel = ExecutionModel.AMM_FPMM
        )

        val saved = questionRepository.save(question)

        try {
            swapService.seedPool(
                SeedPoolRequest(
                    questionId = saved.id!!,
                    seedUsdc = BigDecimal("1000"),
                    feeRate = BigDecimal("0.01")
                )
            )
        } catch (e: Exception) {
            logger.warn("[DailyTrend] seed pool failed for questionId={} : {}", saved.id, e.message)
        }

        logger.info("[DailyTrend] created questionId={}, trend={}", saved.id, trendKeyword)
        return com.predata.backend.dto.QuestionGenerationResponse(
            success = true,
            questionId = saved.id,
            title = saved.title,
            category = saved.category,
            message = "Daily trend question created.",
            isDemoMode = isDemoMode
        )
    }

    private fun fetchTopGoogleTrend(): String? {
        val url = "https://trends.google.com/trending/rss?geo=$dailyTrendGeo"
        return try {
            val headers = HttpHeaders().apply {
                set("User-Agent", "Mozilla/5.0")
                set("Accept", "application/rss+xml, application/xml, text/xml;q=0.9, */*;q=0.8")
            }
            val response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity<String>(headers), String::class.java)
            val xml = response.body.orEmpty()
            if (xml.isBlank()) return null

            // Try multiple title formats:
            // 1) <title><![CDATA[...]]></title>
            // 2) <title>...</title>
            val cdataMatch = Regex(
                "<item[^>]*>.*?<title>\\s*<!\\[CDATA\\[(.*?)]]>\\s*</title>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            ).find(xml)?.groupValues?.getOrNull(1)?.trim()

            if (!cdataMatch.isNullOrBlank()) {
                return cdataMatch
            }

            val plainMatch = Regex(
                "<item[^>]*>.*?<title>\\s*([^<]+?)\\s*</title>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            ).find(xml)?.groupValues?.getOrNull(1)?.trim()

            plainMatch?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            logger.warn("[DailyTrend] failed to fetch RSS: {}", e.message)
            null
        }
    }

    private fun generateQuestionFromTrend(keyword: String): DailyTrendGeneratedQuestion? {
        val today = LocalDate.now(ZoneOffset.UTC)
        val minDate = today.plusDays(7)
        val maxDate = today.plusDays(14)
        val prompt = """
            Today's #1 Google Trends topic in $dailyTrendGeo is: "$keyword"
            Today (UTC): ${today.format(DateTimeFormatter.ISO_DATE)}

            Create exactly ONE English prediction market question.

            Rules:
            - The title must be in English only.
            - It must be a clear YES/NO question.
            - It must be specific, verifiable, and resolvable.
            - It must be EVENT-DRIVEN and interesting for traders.
            - Do NOT ask whether this keyword stays/appears in Google Trends, search ranking, or Top N lists.
            - Do NOT ask generic "remain popular" style questions.
            - Include one concrete measurable condition (e.g., announce/release/win/reach/exceed/at least).
            - Include one clear evidence source in resolutionRule (official announcement, Reuters/AP/BBC, exchange/price API, sports official result).
            - The resolution date must be between ${minDate.format(DateTimeFormatter.ISO_DATE)} and ${maxDate.format(DateTimeFormatter.ISO_DATE)}.
            - Never use any past date or past year.
            - Include a concrete resolution rule in English.

            Return JSON only:
            {"title":"Will ...?","bettingDuration":"7d","resolutionRule":"Resolve YES if ... otherwise NO."}
        """.trimIndent()

        return try {
            val rawText = callLlm(prompt) ?: return null
            if (rawText.isBlank()) return null

            val jsonText = extractJson(rawText) ?: return null
            val parsed = objectMapper.readTree(jsonText)
            val title = parsed.path("title").asText("").trim()
            val bettingDuration = parsed.path("bettingDuration").asText("7d").trim()
            val resolutionRule = parsed.path("resolutionRule").asText("").trim()
            if (title.isBlank() || resolutionRule.isBlank()) return null
            DailyTrendGeneratedQuestion(
                title = title,
                bettingDuration = bettingDuration,
                resolutionRule = resolutionRule
            )
        } catch (e: Exception) {
            logger.warn("[DailyTrend] LLM call failed: {}", e.message)
            null
        }
    }

    private fun validateQuestionQuality(title: String, resolutionRule: String): DailyTrendValidationResult {
        val today = LocalDate.now(ZoneOffset.UTC)
        val prompt = """
            Evaluate this prediction market question.
            Today (UTC): ${today.format(DateTimeFormatter.ISO_DATE)}

            title: "$title"
            resolutionRule: "$resolutionRule"

            Validate all conditions:
            1) English-only title.
            2) Clear YES/NO framing.
            3) Verifiable with public information.
            4) Not trivial/self-evident.
            5) Appropriate content.
            6) Any date/year in the title must not be in the past.
            7) Not about Google Trends/search ranking/top keywords itself.
            8) Must include event + measurable condition.

            Return JSON only:
            {"valid":true,"reason":"ok"}
        """.trimIndent()

        return try {
            val rawText = callLlm(prompt) ?: return DailyTrendValidationResult(false, "Empty validation response")
            val jsonText = extractJson(rawText)
                ?: return DailyTrendValidationResult(false, "Invalid validation JSON")
            val parsed = objectMapper.readTree(jsonText)
            DailyTrendValidationResult(
                valid = parsed.path("valid").asBoolean(false),
                reason = parsed.path("reason").asText("Invalid question")
            )
        } catch (e: Exception) {
            logger.warn("[DailyTrend] validation call failed: {}", e.message)
            DailyTrendValidationResult(false, "Validation call failed")
        }
    }

    private fun validateTemporalSanity(title: String): DailyTrendValidationResult {
        val nowDate = LocalDate.now(ZoneOffset.UTC)

        val datePatterns = listOf(
            Regex("""\b(20\d{2})-(\d{1,2})-(\d{1,2})\b"""),
            Regex("""\b(20\d{2})/(\d{1,2})/(\d{1,2})\b"""),
            Regex("""\b(20\d{2})\.(\d{1,2})\.(\d{1,2})\b""")
        )

        for (pattern in datePatterns) {
            pattern.findAll(title).forEach { m ->
                val y = m.groupValues[1].toIntOrNull() ?: return@forEach
                val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
                val d = m.groupValues[3].toIntOrNull() ?: return@forEach
                val parsed = runCatching { LocalDate.of(y, mo, d) }.getOrNull() ?: return@forEach
                if (parsed.isBefore(nowDate)) {
                    return DailyTrendValidationResult(false, "Title contains a past date: $parsed")
                }
            }
        }

        val yearPattern = Regex("""\b(20\d{2})\b""")
        yearPattern.findAll(title).forEach { m ->
            val y = m.groupValues[1].toIntOrNull() ?: return@forEach
            if (y < nowDate.year) {
                return DailyTrendValidationResult(false, "Title contains a past year: $y")
            }
        }

        return DailyTrendValidationResult(true, "ok")
    }

    private fun validateEventDrivenQuestion(title: String, resolutionRule: String): DailyTrendValidationResult {
        val lowerTitle = title.lowercase()
        val lowerRule = resolutionRule.lowercase()

        val bannedTitlePatterns = listOf(
            "google trends",
            "trending topic",
            "top google trends",
            "top 25",
            "search ranking",
            "appear in google trends",
            "remain among",
            "stay in trends",
            "daily trend"
        )
        if (bannedTitlePatterns.any { lowerTitle.contains(it) }) {
            return DailyTrendValidationResult(false, "Title is trend-ranking meta question, not event-driven")
        }

        val measurableSignalKeywords = listOf(
            "announce", "announcement", "release", "launch", "win", "beat",
            "reach", "exceed", "above", "below", "at least", "more than", "less than",
            "price", "views", "downloads", "score", "goals", "vote", "approve", "approval",
            "file", "publish", "report", "official"
        )
        val hasMeasurableSignal = measurableSignalKeywords.any { lowerTitle.contains(it) || lowerRule.contains(it) }
        if (!hasMeasurableSignal) {
            return DailyTrendValidationResult(false, "Missing measurable event condition")
        }

        val hasIsoDate = Regex("""\b20\d{2}-\d{2}-\d{2}\b""").containsMatchIn(title)
        if (!hasIsoDate) {
            return DailyTrendValidationResult(false, "Title must include explicit ISO date (YYYY-MM-DD)")
        }

        val hasSourceHint = lowerRule.contains("source") ||
            lowerRule.contains("official") ||
            lowerRule.contains("reuters") ||
            lowerRule.contains("ap ") ||
            lowerRule.contains("bbc") ||
            lowerRule.contains("coingecko") ||
            lowerRule.contains("football-data") ||
            lowerRule.contains("http")
        if (!hasSourceHint) {
            return DailyTrendValidationResult(false, "Resolution rule must include a concrete source")
        }

        return DailyTrendValidationResult(true, "ok")
    }

    private fun parseDuration(raw: String?): java.time.Duration {
        if (raw.isNullOrBlank()) return java.time.Duration.ofDays(7)
        val candidate = raw.trim().lowercase()
        val match = Regex("""^(\d+)([hdm])$""").matchEntire(candidate) ?: return java.time.Duration.ofDays(7)
        val value = match.groupValues[1].toLong()
        val unit = match.groupValues[2]
        val parsed = when (unit) {
            "h" -> java.time.Duration.ofHours(value)
            "d" -> java.time.Duration.ofDays(value)
            "m" -> java.time.Duration.ofMinutes(value)
            else -> java.time.Duration.ofDays(7)
        }
        val minDuration = java.time.Duration.ofDays(7)
        val maxDuration = java.time.Duration.ofDays(14)
        return when {
            parsed < minDuration -> minDuration
            parsed > maxDuration -> maxDuration
            else -> parsed
        }
    }

    private fun extractJson(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return raw.substring(start, end + 1)
    }

    private fun callLlm(prompt: String): String? {
        if (anthropicApiKey.isNotBlank()) {
            callAnthropic(prompt)?.let { return it }
        }
        if (geminiApiKey.isNotBlank()) {
            callGemini(prompt)?.let { return it }
        }
        return null
    }

    private fun callAnthropic(prompt: String): String? {
        return try {
            val payload = mapOf(
                "model" to ANTHROPIC_MODEL,
                "max_tokens" to 500,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt))
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("x-api-key", anthropicApiKey)
                set("anthropic-version", ANTHROPIC_VERSION)
            }
            val response = restTemplate.exchange(
                ANTHROPIC_URL,
                HttpMethod.POST,
                HttpEntity(objectMapper.writeValueAsString(payload), headers),
                String::class.java
            )
            val root = objectMapper.readTree(response.body.orEmpty())
            val contentArray = root.path("content")
            if (!contentArray.isArray || contentArray.isEmpty) return null
            contentArray[0].path("text").asText("")
        } catch (e: Exception) {
            logger.warn("[DailyTrend] Anthropic call failed: {}", e.message)
            null
        }
    }

    private fun callGemini(prompt: String): String? {
        val modelCandidates = linkedSetOf(
            geminiModel,
            "gemini-1.5-flash-latest",
            "gemini-1.5-pro-latest",
            "gemini-2.0-flash"
        )

        val payload = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 0.2
            )
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        for (model in modelCandidates) {
            try {
                val url = "$GEMINI_URL/$model:generateContent?key=$geminiApiKey"
                val response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    HttpEntity(objectMapper.writeValueAsString(payload), headers),
                    String::class.java
                )
                val root = objectMapper.readTree(response.body.orEmpty())
                val text = root.path("candidates")
                    .takeIf { it.isArray && it.size() > 0 }
                    ?.get(0)
                    ?.path("content")
                    ?.path("parts")
                    ?.takeIf { it.isArray && it.size() > 0 }
                    ?.get(0)
                    ?.path("text")
                    ?.asText()
                    ?.takeIf { it.isNotBlank() }

                if (!text.isNullOrBlank()) {
                    logger.info("[DailyTrend] Gemini generation succeeded with model={}", model)
                    return text
                }
            } catch (e: Exception) {
                logger.warn("[DailyTrend] Gemini call failed for model={} : {}", model, e.message)
            }
        }

        return null
    }

    private fun buildDemoTrendQuestion(rawKeyword: String): DemoTrendQuestion {
        val keyword = normalizeKeyword(rawKeyword)
        val resolveDate = LocalDate.now(ZoneOffset.UTC).plusDays(7)
        val dateText = resolveDate.toString()

        val title = "Will \"$keyword\" have an official major announcement reported by Reuters or AP by $dateText?"
        val resolutionRule = """
            Resolve YES if Reuters or AP publishes at least one report by $dateText (UTC) confirming a major official event related to "$keyword" (e.g., announcement, release, filing, contract, or election decision). 
            Otherwise resolve NO. Source: Reuters/AP public reports.
        """.trimIndent().replace("\n", " ")

        return DemoTrendQuestion(
            title = title,
            resolutionRule = resolutionRule,
            bettingDuration = "7d"
        )
    }

    private fun normalizeKeyword(raw: String): String {
        val trimmed = raw.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isBlank()) return "Top US Trend Topic"

        // Keep acronyms as-is, otherwise title-case tokens for readability.
        return trimmed.split(" ").joinToString(" ") { token ->
            if (token.length <= 3 && token.all { it.isUpperCase() }) {
                token
            } else {
                token.lowercase().replaceFirstChar { ch -> ch.uppercase() }
            }
        }
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

data class DailyTrendGeneratedQuestion(
    val title: String,
    val bettingDuration: String,
    val resolutionRule: String
)

data class DailyTrendValidationResult(
    val valid: Boolean,
    val reason: String
)

data class DemoTrendQuestion(
    val title: String,
    val resolutionRule: String,
    val bettingDuration: String
)
