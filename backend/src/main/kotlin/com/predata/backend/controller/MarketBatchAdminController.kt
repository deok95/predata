package com.predata.backend.controller

import com.predata.backend.dto.*
import com.predata.backend.exception.ErrorCode
import com.predata.backend.repository.market.MarketOpenBatchRepository
import com.predata.backend.repository.market.QuestionMarketCandidateRepository
import com.predata.backend.service.market.MarketBatchService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * 마켓 배치 운영자 API
 *
 * GET  /api/admin/markets/batches                          – 배치 목록 조회
 * GET  /api/admin/markets/batches/{batchId}/candidates     – 후보 상세 조회
 * POST /api/admin/markets/batches/{cutoffSlot}/run         – 배치 수동 실행
 * POST /api/admin/markets/batches/{batchId}/retry-open     – 실패 건 재시도
 * GET  /api/admin/markets/batches/{batchId}/summary        – 집계 요약
 */
@RestController
@RequestMapping("/api/admin/markets/batches")
@Tag(name = "market-amm", description = "Market batch/open admin APIs")
class MarketBatchAdminController(
    private val marketBatchService: MarketBatchService,
    private val batchRepository: MarketOpenBatchRepository,
    private val candidateRepository: QuestionMarketCandidateRepository,
) {
    enum class SortDir { ASC, DESC }


    /**
     * 배치 목록 조회
     * GET /api/admin/markets/batches?from=&to=
     */
    @GetMapping
    fun listBatches(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "startedAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ResponseEntity<ApiEnvelope<List<MarketBatchSummaryResponse>>> {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val fromTime = from ?: now.minusDays(7)
        val toTime = to ?: now

        val batches = batchRepository.findByStartedAtBetweenOrderByStartedAtDesc(fromTime, toTime)
        val sorted = sortBatches(batches, normalizeBatchSortBy(sortBy), parseSortDir(sortDir))
        val paged = paginate(sorted, page, size)
        return ResponseEntity.ok(ApiEnvelope.ok(paged.map { it.toSummaryResponse() }))
    }

    /**
     * 후보 상세 조회
     * GET /api/admin/markets/batches/{batchId}/candidates
     */
    @GetMapping("/{batchId}/candidates")
    fun getCandidates(
        @PathVariable batchId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "rankInCategory") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String,
    ): ResponseEntity<ApiEnvelope<List<MarketCandidateResponse>>> {
        batchRepository.findById(batchId).orElse(null)
            ?: return ResponseEntity.status(404).body(
                ApiEnvelope.error(ErrorCode.BATCH_NOT_FOUND)
            )

        val candidates = candidateRepository.findByBatchId(batchId)
        val sorted = sortCandidates(candidates, normalizeCandidateSortBy(sortBy), parseSortDir(sortDir))
        val paged = paginate(sorted, page, size)
        return ResponseEntity.ok(ApiEnvelope.ok(paged.map { it.toResponse() }))
    }

    /**
     * 배치 수동 실행 (idempotent)
     * POST /api/admin/markets/batches/{cutoffSlot}/run
     * cutoffSlot 형식:
     *  - ISO_OFFSET_DATE_TIME (권장, 예: 2026-02-23T12:00:00Z)
     *  - ISO_LOCAL_DATE_TIME (예: 2026-02-23T12:00:00, UTC로 해석)
     */
    @PostMapping("/{cutoffSlot}/run")
    fun runBatch(
        @PathVariable cutoffSlot: String,
    ): ResponseEntity<ApiEnvelope<MarketBatchSummaryResponse>> {
        val slot = parseCutoffSlot(cutoffSlot)
            ?: return ResponseEntity.badRequest().body(
                ApiEnvelope.error(ErrorCode.BAD_REQUEST, "cutoffSlot 형식 오류. 예: 2026-02-23T12:00:00Z")
            )

        val batch = marketBatchService.forceRun(slot.truncatedTo(ChronoUnit.MINUTES))
        return ResponseEntity.ok(ApiEnvelope.ok(batch.toSummaryResponse()))
    }

    /**
     * OPEN_FAILED 후보만 재시도
     * POST /api/admin/markets/batches/{batchId}/retry-open
     */
    @PostMapping("/{batchId}/retry-open")
    fun retryOpen(
        @PathVariable batchId: Long,
    ): ResponseEntity<ApiEnvelope<RetryOpenResponse>> {
        val batch = marketBatchService.retryOpen(batchId)
        val response = RetryOpenResponse(
            batchId = batch.id!!,
            status = batch.status.name,
            openedCount = batch.openedCount,
            failedCount = batch.failedCount,
        )
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * 집계 요약 조회
     * GET /api/admin/markets/batches/{batchId}/summary
     */
    @GetMapping("/{batchId}/summary")
    fun getSummary(
        @PathVariable batchId: Long,
    ): ResponseEntity<ApiEnvelope<MarketBatchSummaryDetailResponse>> {
        val batch = batchRepository.findById(batchId).orElse(null)
            ?: return ResponseEntity.status(404).body(
                ApiEnvelope.error(ErrorCode.BATCH_NOT_FOUND)
            )

        val successRate = if (batch.selectedCount > 0) {
            batch.openedCount.toDouble() / batch.selectedCount
        } else 0.0

        val response = MarketBatchSummaryDetailResponse(
            batchId = batch.id!!,
            status = batch.status.name,
            totalCandidates = batch.totalCandidates,
            selectedCount = batch.selectedCount,
            openedCount = batch.openedCount,
            failedCount = batch.failedCount,
            successRate = successRate,
        )
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    private fun parseCutoffSlot(raw: String): LocalDateTime? {
        val normalized = raw.replace(" ", "T")

        return try {
            OffsetDateTime.parse(normalized)
                .withOffsetSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
        } catch (_: DateTimeParseException) {
            try {
                // 오프셋이 없으면 UTC 기준 LocalDateTime으로 해석
                LocalDateTime.parse(normalized)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun parseSortDir(raw: String): SortDir {
        return when (raw.lowercase()) {
            "asc" -> SortDir.ASC
            else -> SortDir.DESC
        }
    }

    private fun normalizeBatchSortBy(raw: String): String {
        return when (raw.lowercase()) {
            "startedat", "started_at" -> "startedAt"
            "createdat", "created_at" -> "startedAt"
            "openedcount", "opened_count" -> "openedCount"
            "failedcount", "failed_count" -> "failedCount"
            else -> "startedAt"
        }
    }

    private fun normalizeCandidateSortBy(raw: String): String {
        return when (raw.lowercase()) {
            "rankincategory", "rank_in_category" -> "rankInCategory"
            "votecount", "vote_count" -> "voteCount"
            "createdat", "created_at" -> "createdAt"
            else -> "rankInCategory"
        }
    }

    private fun sortBatches(
        list: List<com.predata.backend.domain.market.MarketOpenBatch>,
        sortBy: String,
        sortDir: SortDir,
    ): List<com.predata.backend.domain.market.MarketOpenBatch> {
        val comparator = when (sortBy) {
            "openedCount" -> compareBy<com.predata.backend.domain.market.MarketOpenBatch> { it.openedCount }
            "failedCount" -> compareBy<com.predata.backend.domain.market.MarketOpenBatch> { it.failedCount }
            else -> compareBy<com.predata.backend.domain.market.MarketOpenBatch> { it.startedAt }
        }
        return if (sortDir == SortDir.ASC) list.sortedWith(comparator) else list.sortedWith(comparator.reversed())
    }

    private fun sortCandidates(
        list: List<com.predata.backend.domain.market.QuestionMarketCandidate>,
        sortBy: String,
        sortDir: SortDir,
    ): List<com.predata.backend.domain.market.QuestionMarketCandidate> {
        val comparator = when (sortBy) {
            "voteCount" -> compareBy<com.predata.backend.domain.market.QuestionMarketCandidate> { it.voteCount }
            "createdAt" -> compareBy<com.predata.backend.domain.market.QuestionMarketCandidate> { it.createdAt }
            else -> compareBy<com.predata.backend.domain.market.QuestionMarketCandidate> { it.rankInCategory }
        }
        return if (sortDir == SortDir.ASC) list.sortedWith(comparator) else list.sortedWith(comparator.reversed())
    }

    private fun <T> paginate(list: List<T>, page: Int, size: Int): List<T> {
        val p = page.coerceAtLeast(0)
        val s = size.coerceIn(1, 200)
        val from = (p * s).coerceAtMost(list.size)
        val to = (from + s).coerceAtMost(list.size)
        return list.subList(from, to)
    }
}
