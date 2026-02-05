package com.predata.backend.service

import com.predata.backend.domain.QuestionStatus
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
                status = question.status.name,
                type = question.type.name,
                finalResult = question.finalResult.name,
                totalBetPool = question.totalBetPool,
                yesBetPool = question.yesBetPool,
                noBetPool = question.noBetPool,
                yesPercentage = yesPercentage,
                noPercentage = noPercentage,
                sourceUrl = question.sourceUrl,
                disputeDeadline = question.disputeDeadline?.format(formatter),
                votingEndAt = question.votingEndAt.format(formatter),
                bettingStartAt = question.bettingStartAt.format(formatter),
                bettingEndAt = question.bettingEndAt.format(formatter),
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
            status = question.status.name,
            type = question.type.name,
            finalResult = question.finalResult.name,
            totalBetPool = question.totalBetPool,
            yesBetPool = question.yesBetPool,
            noBetPool = question.noBetPool,
            yesPercentage = yesPercentage,
            noPercentage = noPercentage,
            sourceUrl = question.sourceUrl,
            disputeDeadline = question.disputeDeadline?.format(formatter),
            votingEndAt = question.votingEndAt.format(formatter),
            bettingStartAt = question.bettingStartAt.format(formatter),
            bettingEndAt = question.bettingEndAt.format(formatter),
            expiredAt = question.expiredAt.format(formatter),
            createdAt = question.createdAt.format(formatter)
        )
    }

    /**
     * 상태별 질문 조회
     */
    @Transactional(readOnly = true)
    fun getQuestionsByStatus(status: QuestionStatus): List<QuestionResponse> {
        return questionRepository.findByStatus(status).map { question ->
            val total = question.totalBetPool.toDouble()
            val yesPercentage = if (total > 0) (question.yesBetPool / total) * 100 else 0.0
            val noPercentage = if (total > 0) (question.noBetPool / total) * 100 else 0.0

            QuestionResponse(
                id = question.id!!,
                title = question.title,
                category = question.category,
                status = question.status.name,
                type = question.type.name,
                finalResult = question.finalResult.name,
                totalBetPool = question.totalBetPool,
                yesBetPool = question.yesBetPool,
                noBetPool = question.noBetPool,
                yesPercentage = yesPercentage,
                noPercentage = noPercentage,
                sourceUrl = question.sourceUrl,
                disputeDeadline = question.disputeDeadline?.format(formatter),
                votingEndAt = question.votingEndAt.format(formatter),
                bettingStartAt = question.bettingStartAt.format(formatter),
                bettingEndAt = question.bettingEndAt.format(formatter),
                expiredAt = question.expiredAt.format(formatter),
                createdAt = question.createdAt.format(formatter)
            )
        }
    }
}
