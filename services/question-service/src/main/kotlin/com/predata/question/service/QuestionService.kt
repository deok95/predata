package com.predata.question.service

import com.predata.common.domain.QuestionStatus
import com.predata.common.domain.FinalResult
import com.predata.question.domain.Question
import com.predata.question.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class QuestionService(
    private val questionRepository: QuestionRepository
) {

    @Transactional
    fun createQuestion(request: CreateQuestionRequest): Question {
        val question = Question(
            title = request.title,
            category = request.category,
            expiresAt = request.expiresAt
        )
        return questionRepository.save(question)
    }

    @Transactional(readOnly = true)
    fun getQuestion(id: Long): Question {
        return questionRepository.findById(id)
            .orElseThrow { IllegalArgumentException("질문을 찾을 수 없습니다.") }
    }

    @Transactional(readOnly = true)
    fun getAllQuestions(): List<Question> {
        return questionRepository.findAll()
    }

    @Transactional
    fun updateBetPool(questionId: Long, choice: String, amount: Long): Question {
        val question = getQuestion(questionId)
        
        question.totalBetPool += amount
        if (choice == "YES") {
            question.yesBetPool += amount
        } else {
            question.noBetPool += amount
        }
        
        return questionRepository.save(question)
    }

    @Transactional
    fun updateStatus(questionId: Long, status: String) {
        val question = getQuestion(questionId)
        question.status = QuestionStatus.valueOf(status)
        questionRepository.save(question)
    }

    @Transactional
    fun updateFinalResult(questionId: Long, finalResult: String) {
        val question = getQuestion(questionId)
        question.finalResult = FinalResult.valueOf(finalResult)
        question.status = QuestionStatus.SETTLED
        questionRepository.save(question)
    }

    @Transactional
    fun settleQuestion(questionId: Long, finalResult: FinalResult): Question {
        val question = getQuestion(questionId)
        question.status = QuestionStatus.SETTLED
        question.finalResult = finalResult
        return questionRepository.save(question)
    }
}

data class CreateQuestionRequest(
    val title: String,
    val category: String?,
    val expiresAt: LocalDateTime?
)
