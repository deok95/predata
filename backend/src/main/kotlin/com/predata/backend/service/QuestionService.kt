package com.predata.backend.service

import com.predata.backend.dto.QuestionResponse
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class QuestionService(
    private val questionRepository: QuestionRepository
) {

    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    /**
     * 모든 질문 조회
     */
    @Transactional(readOnly = true)
    fun getAllQuestions(): List<QuestionResponse> {
        return questionRepository.findAll().map { question ->
            val total = question.totalBetPool.toDouble()
            val yesPercentage = if (total > 0) (question.yesBetPool / total) * 100 else 0.0
            val noPercentage = if (total > 0) (question.noBetPool / total) * 100 else 0.0

            QuestionResponse(
                id = question.id!!,
                title = question.title,
                category = question.category,
                status = question.status,
                finalResult = question.finalResult.name,
                totalBetPool = question.totalBetPool,
                yesBetPool = question.yesBetPool,
                noBetPool = question.noBetPool,
                yesPercentage = yesPercentage,
                noPercentage = noPercentage,
                expiredAt = question.expiredAt.format(formatter),
                createdAt = question.createdAt.format(formatter)
            )
        }
    }

    /**
     * 특정 질문 조회
     */
    @Transactional(readOnly = true)
    fun getQuestion(id: Long): QuestionResponse? {
        val question = questionRepository.findById(id).orElse(null) ?: return null

        val total = question.totalBetPool.toDouble()
        val yesPercentage = if (total > 0) (question.yesBetPool / total) * 100 else 0.0
        val noPercentage = if (total > 0) (question.noBetPool / total) * 100 else 0.0

        return QuestionResponse(
            id = question.id!!,
            title = question.title,
            category = question.category,
            status = question.status,
            finalResult = question.finalResult.name,
            totalBetPool = question.totalBetPool,
            yesBetPool = question.yesBetPool,
            noBetPool = question.noBetPool,
            yesPercentage = yesPercentage,
            noPercentage = noPercentage,
            expiredAt = question.expiredAt.format(formatter),
            createdAt = question.createdAt.format(formatter)
        )
    }

    /**
     * 상태별 질문 조회
     */
    @Transactional(readOnly = true)
    fun getQuestionsByStatus(status: String): List<QuestionResponse> {
        return questionRepository.findByStatus(status).map { question ->
            val total = question.totalBetPool.toDouble()
            val yesPercentage = if (total > 0) (question.yesBetPool / total) * 100 else 0.0
            val noPercentage = if (total > 0) (question.noBetPool / total) * 100 else 0.0

            QuestionResponse(
                id = question.id!!,
                title = question.title,
                category = question.category,
                status = question.status,
                finalResult = question.finalResult.name,
                totalBetPool = question.totalBetPool,
                yesBetPool = question.yesBetPool,
                noBetPool = question.noBetPool,
                yesPercentage = yesPercentage,
                noPercentage = noPercentage,
                expiredAt = question.expiredAt.format(formatter),
                createdAt = question.createdAt.format(formatter)
            )
        }
    }
}
