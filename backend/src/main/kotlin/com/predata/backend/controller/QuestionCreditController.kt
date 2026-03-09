package com.predata.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.dto.CreditStatusResponse
import com.predata.backend.dto.DraftOpenResponse
import com.predata.backend.dto.DraftSubmitResponse
import com.predata.backend.dto.SubmitQuestionDraftRequest
import com.predata.backend.repository.ActivityRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.TransactionHistoryRepository
import com.predata.backend.service.QuestionDraftService
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@Tag(name = "question", description = "Question credit and draft APIs")
@RequestMapping("/api/questions")
class QuestionCreditController(
    private val questionDraftService: QuestionDraftService,
    private val questionRepository: QuestionRepository,
    private val activityRepository: ActivityRepository,
    private val transactionHistoryRepository: TransactionHistoryRepository,
) {

    /** 현재 크레딧 잔액 조회 */
    @GetMapping("/credits/status")
    fun getCreditStatus(request: HttpServletRequest): ApiEnvelope<CreditStatusResponse> {
        val memberId = request.authenticatedMemberId()
        return ApiEnvelope.ok(questionDraftService.getCreditStatus(memberId))
    }

    /** 질문 draft 세션 오픈 — 30분 유효 */
    @PostMapping("/drafts/open")
    @ResponseStatus(HttpStatus.CREATED)
    fun openDraft(request: HttpServletRequest): ApiEnvelope<DraftOpenResponse> {
        val memberId = request.authenticatedMemberId()
        return ApiEnvelope.ok(questionDraftService.openDraft(memberId))
    }

    /**
     * 질문 최종 제출
     * - X-Idempotency-Key: draft-open 응답의 submitIdempotencyKey 값
     */
    @PostMapping("/drafts/{draftId}/submit")
    @ResponseStatus(HttpStatus.CREATED)
    fun submitDraft(
        @PathVariable draftId: String,
        @RequestHeader("X-Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody body: SubmitQuestionDraftRequest,
        request: HttpServletRequest,
    ): ApiEnvelope<DraftSubmitResponse> {
        val memberId = request.authenticatedMemberId()
        return ApiEnvelope.ok(questionDraftService.submitDraft(memberId, draftId, idempotencyKey, body))
    }

    /** draft 취소 — 멱등 처리 */
    @PostMapping("/drafts/{draftId}/cancel")
    fun cancelDraft(
        @PathVariable draftId: String,
        request: HttpServletRequest,
    ): ApiEnvelope<Unit> {
        val memberId = request.authenticatedMemberId()
        questionDraftService.cancelDraft(memberId, draftId)
        return ApiEnvelope.ok(Unit)
    }

    /** 내가 생성한 질문 목록 */
    @GetMapping("/me/created")
    fun getMyCreatedQuestions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "30") size: Int,
        request: HttpServletRequest,
    ): ApiEnvelope<List<CreatedQuestionItemResponse>> {
        val memberId = request.authenticatedMemberId()
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val questionPage = questionRepository.findByCreatorMemberIdOrderByCreatedAtDesc(memberId, pageable)
        val questions = questionPage.content

        if (questions.isEmpty()) return ApiEnvelope.ok(emptyList())

        val questionIds = questions.mapNotNull { it.id }
        val voteCountMap = activityRepository.countVotesByQuestionIds(questionIds).associate {
            it.getQuestionId() to it.getVoteCount()
        }
        val earningsMap = transactionHistoryRepository
            .sumAmountByMemberIdAndTypeGroupedByQuestion(memberId, "CREATOR_FEE_DISTRIBUTION", questionIds)
            .associate { it.getQuestionId() to it.getTotalAmount() }

        val items = questions.map { q ->
            val qid = q.id ?: 0L
            CreatedQuestionItemResponse(
                questionId = qid,
                title = q.title,
                category = q.category,
                status = q.status.name,
                totalVotes = voteCountMap[qid] ?: 0L,
                earnings = earningsMap[qid] ?: BigDecimal.ZERO,
                createdAt = q.createdAt.toString(),
            )
        }
        return ApiEnvelope.ok(items)
    }
}

data class CreatedQuestionItemResponse(
    val questionId: Long,
    val title: String,
    val category: String?,
    val status: String,
    val totalVotes: Long,
    val earnings: BigDecimal,
    val createdAt: String,
)
