package com.predata.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag

import com.predata.backend.domain.OnChainRelayStatus
import com.predata.backend.domain.OnChainVoteRelay
import com.predata.backend.domain.VoteSummary
import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.repository.DailyVoteUsageRepository
import com.predata.backend.repository.OnChainVoteRelayRepository
import com.predata.backend.repository.VoteSummaryRepository
import com.predata.backend.service.relay.RelayAdminService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 투표 운영 관리자 컨트롤러
 *
 * VOTE-023: 운영 조회 API
 *   GET  /api/admin/vote-ops/usage    – 일일 투표 사용 현황
 *   GET  /api/admin/vote-ops/relay    – 온체인 릴레이 목록 (상태 필터)
 *   GET  /api/admin/vote-ops/summary/{questionId} – 질문별 투표 집계
 *
 * VOTE-024: 수동 재시도 API
 *   POST /api/admin/vote-ops/relay/{id}/retry – FAILED_FINAL → PENDING 강제 전환
 */
@RestController
@Tag(name = "voting", description = "Vote ops admin APIs")
@RequestMapping("/api/admin/vote-ops")
class VoteOpsAdminController(
    private val dailyVoteUsageRepository: DailyVoteUsageRepository,
    private val onChainVoteRelayRepository: OnChainVoteRelayRepository,
    private val voteSummaryRepository: VoteSummaryRepository,
    private val relayAdminService: RelayAdminService,
) {
    enum class SortDir { ASC, DESC }


    /**
     * 일일 투표 사용 현황 조회
     * GET /api/admin/vote-ops/usage?date=YYYY-MM-DD  (생략 시 오늘 UTC 기준)
     */
    @GetMapping("/usage")
    fun getUsage(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "usedCount") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ResponseEntity<ApiEnvelope<UsageSummaryResponse>> {
        val target = date ?: LocalDate.now(ZoneOffset.UTC)
        val usages = dailyVoteUsageRepository.findByUsageDateOrderByUsedCountDesc(target)
        val sorted = when (normalizeUsageSortBy(sortBy)) {
            "memberId" -> if (parseSortDir(sortDir) == SortDir.ASC) usages.sortedBy { it.memberId } else usages.sortedByDescending { it.memberId }
            else -> if (parseSortDir(sortDir) == SortDir.ASC) usages.sortedBy { it.usedCount } else usages.sortedByDescending { it.usedCount }
        }
        val pageSize = size.coerceIn(1, 200)
        val from = (page.coerceAtLeast(0) * pageSize).coerceAtMost(sorted.size)
        val to = (from + pageSize).coerceAtMost(sorted.size)
        val paged = sorted.subList(from, to)

        val response = UsageSummaryResponse(
            date = target,
            totalMembers = usages.size,
            totalVotes = usages.sumOf { it.usedCount },
            page = page.coerceAtLeast(0),
            size = pageSize,
            totalPages = if (usages.isEmpty()) 0 else ((usages.size - 1) / pageSize) + 1,
            entries = paged.map {
                UsageEntry(memberId = it.memberId, usedCount = it.usedCount)
            },
        )
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * 온체인 릴레이 목록 조회
     * GET /api/admin/vote-ops/relay?status=PENDING  (생략 시 전체)
     */
    @GetMapping("/relay")
    fun getRelayList(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String,
    ): ResponseEntity<ApiEnvelope<RelayListResponse>> {
        val pageable = buildPageable(page, size, normalizeRelaySortBy(sortBy), parseSortDir(sortDir))
        val relays = if (status != null) {
            val relayStatus = runCatching { OnChainRelayStatus.valueOf(status) }.getOrNull()
                ?: return ResponseEntity.badRequest().body(
                    ApiEnvelope.error(com.predata.backend.exception.ErrorCode.BAD_REQUEST, "Unknown status: $status")
                )
            onChainVoteRelayRepository.findByStatus(relayStatus, pageable)
        } else {
            onChainVoteRelayRepository.findAll(pageable)
        }

        val response = RelayListResponse(
            total = relays.totalElements.toInt(),
            page = relays.number,
            size = relays.size,
            totalPages = relays.totalPages,
            items = relays.content.map { it.toSummary() },
        )
        return ResponseEntity.ok(ApiEnvelope.ok(response))
    }

    /**
     * 질문별 투표 집계 조회
     * GET /api/admin/vote-ops/summary/{questionId}
     */
    @GetMapping("/summary/{questionId}")
    fun getSummary(
        @PathVariable questionId: Long,
    ): ResponseEntity<ApiEnvelope<VoteSummaryResponse>> {
        val summary = voteSummaryRepository.findById(questionId).orElse(null)
            ?: return ResponseEntity.ok(
                ApiEnvelope.ok(VoteSummaryResponse(questionId = questionId, yesCount = 0, noCount = 0, totalCount = 0))
            )

        return ResponseEntity.ok(ApiEnvelope.ok(summary.toResponse()))
    }

    /**
     * FAILED_FINAL 릴레이 수동 재시도
     * POST /api/admin/vote-ops/relay/{id}/retry
     */
    @PostMapping("/relay/{id}/retry")
    fun forceRetry(
        @PathVariable id: Long,
    ): ResponseEntity<ApiEnvelope<RelayItemSummary>> {
        val relay = relayAdminService.forceRetry(id)
        return ResponseEntity.ok(ApiEnvelope.ok(relay.toSummary()))
    }

    private fun OnChainVoteRelay.toSummary() = RelayItemSummary(
        id = id!!,
        voteId = voteId,
        memberId = memberId,
        questionId = questionId,
        choice = choice.name,
        status = status.name,
        retryCount = retryCount,
        txHash = txHash,
        errorMessage = errorMessage,
        nextRetryAt = nextRetryAt?.toString(),
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

    private fun VoteSummary.toResponse() = VoteSummaryResponse(
        questionId = questionId,
        yesCount = yesCount,
        noCount = noCount,
        totalCount = totalCount,
    )

    private fun normalizeUsageSortBy(raw: String): String {
        return when (raw.lowercase()) {
            "memberid", "member_id" -> "memberId"
            "usedcount", "used_count" -> "usedCount"
            else -> "usedCount"
        }
    }

    private fun normalizeRelaySortBy(raw: String): String {
        return when (raw.lowercase()) {
            "createdat", "created_at" -> "createdAt"
            "retrycount", "retry_count" -> "retryCount"
            else -> "createdAt"
        }
    }

    private fun parseSortDir(raw: String): SortDir {
        return when (raw.lowercase()) {
            "desc" -> SortDir.DESC
            else -> SortDir.ASC
        }
    }

    private fun buildPageable(page: Int, size: Int, sortBy: String, sortDir: SortDir): PageRequest {
        val direction = if (sortDir == SortDir.DESC) Sort.Direction.DESC else Sort.Direction.ASC
        return PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 200), Sort.by(direction, sortBy))
    }
}

data class UsageSummaryResponse(
    val date: LocalDate,
    val totalMembers: Int,
    val totalVotes: Int,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val entries: List<UsageEntry>,
)

data class UsageEntry(
    val memberId: Long,
    val usedCount: Int,
)

data class RelayListResponse(
    val total: Int,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val items: List<RelayItemSummary>,
)

data class RelayItemSummary(
    val id: Long,
    val voteId: Long,
    val memberId: Long,
    val questionId: Long,
    val choice: String,
    val status: String,
    val retryCount: Int,
    val txHash: String?,
    val errorMessage: String?,
    val nextRetryAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

data class VoteSummaryResponse(
    val questionId: Long,
    val yesCount: Long,
    val noCount: Long,
    val totalCount: Long,
)
