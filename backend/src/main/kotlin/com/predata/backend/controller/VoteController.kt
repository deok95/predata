package com.predata.backend.controller

import com.predata.backend.config.properties.VoteProperties
import com.predata.backend.domain.OnChainRelayStatus
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.VoteVisibility
import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.dto.VoteCommitRequest
import com.predata.backend.dto.VoteCommitResponse
import com.predata.backend.dto.VoteRequest
import com.predata.backend.dto.VoteResponse
import com.predata.backend.dto.VoteRevealRequest
import com.predata.backend.dto.VoteRevealResponse
import com.predata.backend.dto.VoteStatusResponse
import com.predata.backend.exception.ErrorCode
import com.predata.backend.exception.ForbiddenException
import com.predata.backend.exception.NotFoundException
import com.predata.backend.exception.ServiceUnavailableException
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.VoteSummaryRepository
import com.predata.backend.service.VoteCommandService
import com.predata.backend.service.VoteCommitService
import com.predata.backend.service.VoteFeedService
import com.predata.backend.service.VoteQueryService
import com.predata.backend.util.authenticatedMemberId
import com.predata.backend.util.authenticatedMemberIdOrNull
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 투표 컨트롤러
 *
 * 표준 투표 경로 (OBJECTIVE_RULE·VOTE_RESULT 공통):
 *   POST /api/votes — 단일 투표. daily_vote_usage 한도 적용. hasVotingPass 불필요.
 *
 * commit/reveal 경로 (VOTE_RESULT 전용, feature flag 필요):
 *   POST /api/votes/commit  — 1단계: 투표 해시 제출 (app.voting.commit-reveal-enabled=true 시 활성)
 *   POST /api/votes/reveal  — 2단계: 선택 공개 및 검증
 *   두 경로 모두 hasVotingPass + 티켓 차감 정책을 강제한다.
 *   prod 환경에서는 commit-reveal-enabled=false이므로 503을 반환한다.
 *
 * 읽기 경로:
 *   GET /api/votes/results/{questionId}  — VoteSummary 기반 집계 반환
 *   GET /api/votes/status/{questionId}   — 투표 가능 여부 사전 조회
 */
@RestController
@RequestMapping("/api/votes")
@Tag(name = "voting", description = "Vote command/query APIs")
class VoteController(
    private val voteCommandService: VoteCommandService,
    private val voteQueryService: VoteQueryService,
    private val voteCommitService: VoteCommitService,
    private val voteFeedService: VoteFeedService,
    private val memberRepository: MemberRepository,
    private val questionRepository: QuestionRepository,
    private val voteSummaryRepository: VoteSummaryRepository,
    private val voteProperties: VoteProperties,
) {
    private val logger = LoggerFactory.getLogger(VoteController::class.java)

    // ── 활성 write 경로 ────────────────────────────────────────────────────────

    /**
     * 투표 실행 (Phase 2 단일 경로)
     * 응답: {voteId, questionId, choice, remainingDailyVotes, onChainStatus}
     */
    @PostMapping
    fun vote(
        @Valid @RequestBody request: VoteRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<VoteResponse>> {
        val memberId = httpRequest.authenticatedMemberId()

        val member = memberRepository.findById(memberId)
            .orElseThrow { NotFoundException("Member not found.") }

        if (member.isBanned) {
            throw ForbiddenException(
                message = "Account has been suspended. Reason: ${member.banReason ?: "Terms of Service violation"}",
                code = ErrorCode.ACCOUNT_BANNED.name
            )
        }

        val savedActivity = voteCommandService.vote(memberId, request.questionId, request.choice)
        val remaining = voteQueryService.remainingDailyVotes(memberId)

        return ResponseEntity.ok(ApiEnvelope.ok(
            VoteResponse(
                voteId = savedActivity.id!!,
                questionId = request.questionId,
                choice = request.choice,
                remainingDailyVotes = remaining,
                onChainStatus = OnChainRelayStatus.PENDING.name
            )
        ))
    }

    // ── Commit-Reveal write 경로 (VOTE_RESULT 질문 전용) ──────────────────────

    /**
     * Commit 단계: VOTE_RESULT 질문에 투표 해시 제출 (Phase 1)
     * - feature flag: app.voting.commit-reveal-enabled=false 시 503 반환
     * - OBJECTIVE_RULE 질문에 사용 시 400 COMMIT_REVEAL_NOT_ELIGIBLE
     */
    @PostMapping("/commit")
    fun commit(
        @Valid @RequestBody request: VoteCommitRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<VoteCommitResponse>> {
        if (!voteProperties.commitRevealEnabled) {
            throw ServiceUnavailableException(
                ErrorCode.COMMIT_REVEAL_DISABLED.message,
                ErrorCode.COMMIT_REVEAL_DISABLED.name
            )
        }
        val memberId = httpRequest.authenticatedMemberId()
        val result = voteCommitService.commit(memberId, request)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    /**
     * Reveal 단계: VOTE_RESULT 질문에 실제 선택 공개 (Phase 2)
     * - feature flag: app.voting.commit-reveal-enabled=false 시 503 반환
     * - OBJECTIVE_RULE 질문에 사용 시 400 COMMIT_REVEAL_NOT_ELIGIBLE
     */
    @PostMapping("/reveal")
    fun reveal(
        @Valid @RequestBody request: VoteRevealRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<VoteRevealResponse>> {
        if (!voteProperties.commitRevealEnabled) {
            throw ServiceUnavailableException(
                ErrorCode.COMMIT_REVEAL_DISABLED.message,
                ErrorCode.COMMIT_REVEAL_DISABLED.name
            )
        }
        val memberId = httpRequest.authenticatedMemberId()
        val result = voteCommitService.reveal(memberId, request)
        return ResponseEntity.ok(ApiEnvelope.ok(result))
    }

    // ── 읽기 경로 ─────────────────────────────────────────────────────────────

    /**
     * 투표 결과 조회
     *
     * VoteSummary를 단일 집계 소스로 사용한다.
     * - /api/votes 경로: VoteCommandService가 VoteSummary를 즉시 업데이트
     * - commit/reveal 경로: VoteCommitService.reveal()이 VoteSummary를 업데이트
     * → 어느 경로로 투표해도 VoteSummary에 정확히 반영됨
     *
     * 가시성 규칙:
     * - OPEN (OBJECTIVE_RULE): yesCount/noCount 항상 공개
     * - HIDDEN_UNTIL_REVEAL (VOTE_RESULT reveal 전): totalCount만 공개
     * - REVEALED (VOTE_RESULT reveal 완료): yesCount/noCount 전체 공개
     */
    @GetMapping("/results/{questionId}")
    fun getResults(@PathVariable questionId: Long): ResponseEntity<ApiEnvelope<Map<String, Any?>>> {
        val question = questionRepository.findById(questionId)
            .orElseThrow { NotFoundException("Question not found.") }

        val summary = voteSummaryRepository.findById(questionId).orElse(null)

        if (question.voteVisibility == VoteVisibility.HIDDEN_UNTIL_REVEAL) {
            // reveal 전: 총 참여 수만 공개 (예측 방향 숨김)
            return ResponseEntity.ok(ApiEnvelope.ok(mapOf(
                "yesCount" to null,
                "noCount" to null,
                "totalCount" to (summary?.totalCount ?: 0L),
                "voteVisibility" to question.voteVisibility.name,
                "message" to "투표 결과는 reveal 마감 후 공개됩니다.",
            )))
        }

        // OPEN 또는 REVEALED: 전체 집계 공개
        val yesCount = summary?.yesCount ?: 0L
        val noCount  = summary?.noCount  ?: 0L
        return ResponseEntity.ok(ApiEnvelope.ok(mapOf(
            "success"        to true,
            "message"        to "Vote results retrieved successfully",
            "yesCount"       to yesCount,
            "noCount"        to noCount,
            "totalCount"     to (yesCount + noCount),
            "voteVisibility" to question.voteVisibility.name,
        )))
    }

    /**
     * 투표 가능 여부 사전 조회
     * - 미인증: 질문 오픈 여부만 반영
     * - 인증: 개인 투표 여부 + 일일 잔여 횟수까지 반영
     */
    @GetMapping("/status/{questionId}")
    fun getStatus(
        @PathVariable questionId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiEnvelope<VoteStatusResponse>> {
        val memberId = httpRequest.authenticatedMemberIdOrNull()
            ?: return ResponseEntity.ok(ApiEnvelope.ok(unauthenticatedStatus(questionId)))

        return ResponseEntity.ok(ApiEnvelope.ok(voteQueryService.canVote(memberId, questionId)))
    }

    /**
     * 투표 피드 조회 (For You / Following / Top10)
     * - mode=following 인 경우 인증 필요(미인증 시 빈 목록)
     * - mode=foryou 는 미인증 시 trending 기반 fallback
     */
    @GetMapping("/feed")
    fun getFeed(
        @RequestParam(defaultValue = "foryou") mode: String,
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "D1") window: String,
        @RequestParam(required = false) voteWindowType: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<List<VoteFeedItemResponse>>> {
        val requesterId = httpRequest.authenticatedMemberIdOrNull()
        val feedMode = parseFeedMode(mode)
        val feedWindow = parseFeedWindow(window)
        val items = voteFeedService.getFeed(
            mode = feedMode,
            requesterId = requesterId,
            category = category,
            window = feedWindow,
            voteWindowType = voteWindowType,
            limit = limit,
        ).map {
            VoteFeedItemResponse(
                questionId = it.questionId,
                title = it.title,
                category = it.category,
                yesVotes = it.yesVotes,
                noVotes = it.noVotes,
                totalVotes = it.totalVotes,
                yesPrice = it.yesPrice,
                commentCount = it.commentCount,
                submitterId = it.submitterId,
                submitterUsername = it.submitterUsername,
                submitterDisplayName = it.submitterDisplayName,
                submitterAvatarUrl = it.submitterAvatarUrl,
                isFollowingSubmitter = it.isFollowingSubmitter,
                submittedAt = it.submittedAt,
                lastVoteAt = it.lastVoteAt,
                votingEndAt = it.votingEndAt,
            )
        }
        return ResponseEntity.ok(ApiEnvelope.ok(items))
    }

    /**
     * 오늘 남은 일일 투표 횟수 조회 (인증 필요)
     */
    @GetMapping("/daily-remaining")
    fun getDailyRemaining(
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiEnvelope<Map<String, Int>>> {
        val memberId = httpRequest.authenticatedMemberId()
        val remaining = voteQueryService.remainingDailyVotes(memberId)
        return ResponseEntity.ok(ApiEnvelope.ok(mapOf("remainingVotes" to remaining)))
    }

    private fun unauthenticatedStatus(questionId: Long): VoteStatusResponse {
        val question = questionRepository.findById(questionId)
            .orElseThrow { NotFoundException("Question not found.") }

        val isVoting = question.status == QuestionStatus.VOTING
        return VoteStatusResponse(
            canVote = isVoting,
            alreadyVoted = false,
            remainingDailyVotes = if (isVoting) voteProperties.dailyLimit else 0,
            reason = if (!isVoting) ErrorCode.VOTING_CLOSED.name else null,
            voteVisibility = question.voteVisibility,
        )
    }

    private fun parseFeedMode(raw: String): VoteFeedService.FeedMode {
        return when (raw.uppercase()) {
            "FOLLOWING" -> VoteFeedService.FeedMode.FOLLOWING
            "TOP10" -> VoteFeedService.FeedMode.TOP10
            else -> VoteFeedService.FeedMode.FORYOU
        }
    }

    private fun parseFeedWindow(raw: String): VoteFeedService.Window {
        return when (raw.uppercase()) {
            "H6", "6H" -> VoteFeedService.Window.H6
            "H3" -> VoteFeedService.Window.H3
            "D3" -> VoteFeedService.Window.D3
            else -> VoteFeedService.Window.D1
        }
    }
}

data class VoteFeedItemResponse(
    val questionId: Long,
    val title: String,
    val category: String?,
    val yesVotes: Long,
    val noVotes: Long,
    val totalVotes: Long,
    val yesPrice: Double,
    val commentCount: Long,
    val submitterId: Long?,
    val submitterUsername: String?,
    val submitterDisplayName: String?,
    val submitterAvatarUrl: String?,
    val isFollowingSubmitter: Boolean,
    val submittedAt: String,
    val lastVoteAt: String,
    val votingEndAt: String,
)
