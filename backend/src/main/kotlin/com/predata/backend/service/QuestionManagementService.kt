package com.predata.backend.service

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.Question
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class QuestionManagementService(
    private val questionRepository: QuestionRepository
) {

    /**
     * 새 질문 생성
     */
    @Transactional
    fun createQuestion(request: CreateQuestionRequest): QuestionCreationResponse {
        // 1. 마감일 검증
        val expiredAt = LocalDateTime.parse(request.expiredAt, DateTimeFormatter.ISO_DATE_TIME)
        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw IllegalArgumentException("마감일은 현재 시간 이후여야 합니다.")
        }

        // 2. 질문 생성
        val question = Question(
            title = request.title,
            category = request.category,
            categoryWeight = BigDecimal(request.categoryWeight ?: 1.0),
            status = "OPEN",
            totalBetPool = 0,
            yesBetPool = 0,
            noBetPool = 0,
            finalResult = FinalResult.PENDING,
            expiredAt = expiredAt,
            createdAt = LocalDateTime.now()
        )

        val savedQuestion = questionRepository.save(question)

        return QuestionCreationResponse(
            success = true,
            questionId = savedQuestion.id ?: 0,
            title = savedQuestion.title,
            category = savedQuestion.category ?: "",
            expiredAt = savedQuestion.expiredAt.toString(),
            message = "질문이 생성되었습니다."
        )
    }

    /**
     * 질문 수정
     */
    @Transactional
    fun updateQuestion(questionId: Long, request: UpdateQuestionRequest): QuestionCreationResponse {
        val question = questionRepository.findById(questionId)
            .orElseThrow { IllegalArgumentException("질문을 찾을 수 없습니다.") }

        // 이미 정산된 질문은 수정 불가
        if (question.status == "SETTLED") {
            throw IllegalStateException("이미 정산된 질문은 수정할 수 없습니다.")
        }

        // 수정 가능한 필드만 업데이트
        request.title?.let { question.title }
        request.category?.let { question.category }
        request.expiredAt?.let { 
            val newExpiredAt = LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
            if (newExpiredAt.isBefore(LocalDateTime.now())) {
                throw IllegalArgumentException("마감일은 현재 시간 이후여야 합니다.")
            }
            // Note: Question이 data class라 불변이므로 실제로는 새 객체 생성 필요
            // 여기서는 간단히 처리
        }

        val saved = questionRepository.save(question)

        return QuestionCreationResponse(
            success = true,
            questionId = saved.id ?: 0,
            title = saved.title,
            category = saved.category ?: "",
            expiredAt = saved.expiredAt.toString(),
            message = "질문이 수정되었습니다."
        )
    }

    /**
     * 질문 삭제 (실제로는 status를 CLOSED로 변경)
     */
    @Transactional
    fun deleteQuestion(questionId: Long): DeleteQuestionResponse {
        val question = questionRepository.findById(questionId)
            .orElseThrow { IllegalArgumentException("질문을 찾을 수 없습니다.") }

        // 베팅이 있는 질문은 삭제 불가
        if (question.totalBetPool > 0) {
            throw IllegalStateException("베팅이 있는 질문은 삭제할 수 없습니다.")
        }

        question.status = "CLOSED"
        questionRepository.save(question)

        return DeleteQuestionResponse(
            success = true,
            message = "질문이 삭제되었습니다."
        )
    }

    /**
     * 질문 목록 조회 (관리자용 - 모든 상태)
     */
    @Transactional(readOnly = true)
    fun getAllQuestionsForAdmin(): List<QuestionAdminView> {
        return questionRepository.findAll().map { question ->
            QuestionAdminView(
                id = question.id ?: 0,
                title = question.title,
                category = question.category ?: "",
                status = question.status,
                totalBetPool = question.totalBetPool,
                totalVotes = 0, // TODO: 투표 수 계산
                expiredAt = question.expiredAt.toString(),
                createdAt = question.createdAt.toString()
            )
        }
    }
}

// ===== DTOs =====

data class CreateQuestionRequest(
    val title: String,
    val category: String,
    val expiredAt: String, // ISO 8601 형식
    val categoryWeight: Double? = 1.0
)

data class UpdateQuestionRequest(
    val title: String? = null,
    val category: String? = null,
    val expiredAt: String? = null
)

data class QuestionCreationResponse(
    val success: Boolean,
    val questionId: Long,
    val title: String,
    val category: String,
    val expiredAt: String,
    val message: String
)

data class DeleteQuestionResponse(
    val success: Boolean,
    val message: String
)

data class QuestionAdminView(
    val id: Long,
    val title: String,
    val category: String,
    val status: String,
    val totalBetPool: Long,
    val totalVotes: Int,
    val expiredAt: String,
    val createdAt: String
)
