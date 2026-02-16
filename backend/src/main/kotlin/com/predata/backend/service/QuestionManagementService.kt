package com.predata.backend.service

import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.QuestionType
import com.predata.backend.repository.QuestionRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class QuestionManagementService(
    private val questionRepository: QuestionRepository,
    private val blockchainService: BlockchainService,
    private val jdbcTemplate: JdbcTemplate
) {

    companion object {
        const val INITIAL_LIQUIDITY = 500L
    }

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
        val votingEndAt = expiredAt.minusDays(1) // 마감 1일 전까지 투표
        val bettingStartAt = votingEndAt.plusMinutes(5) // 투표 마감 후 5분 뒤 베팅 시작

        val question = Question(
            title = request.title,
            category = request.category,
            categoryWeight = BigDecimal(request.categoryWeight ?: 1.0),
            status = QuestionStatus.VOTING,
            type = QuestionType.VERIFIABLE,
            resolutionRule = "Legacy question - manual resolution",
            votingEndAt = votingEndAt,
            bettingStartAt = bettingStartAt,
            bettingEndAt = expiredAt,
            totalBetPool = INITIAL_LIQUIDITY * 2,
            yesBetPool = INITIAL_LIQUIDITY,
            noBetPool = INITIAL_LIQUIDITY,
            initialYesPool = INITIAL_LIQUIDITY,
            initialNoPool = INITIAL_LIQUIDITY,
            finalResult = FinalResult.PENDING,
            expiredAt = expiredAt,
            createdAt = LocalDateTime.now()
        )

        val savedQuestion = questionRepository.save(question)

        // 3. 온체인에 질문 생성 (비동기)
        blockchainService.createQuestionOnChain(savedQuestion)

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

        // 정산된 질문은 수정 불가
        if (question.status == QuestionStatus.SETTLED) {
            throw IllegalStateException("정산된 질문은 수정할 수 없습니다.")
        }

        // 수정 가능한 필드만 업데이트
        request.title?.let { question.title = it }
        request.category?.let { question.category = it }
        request.expiredAt?.let {
            val newExpiredAt = LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
            if (newExpiredAt.isBefore(LocalDateTime.now())) {
                throw IllegalArgumentException("마감일은 현재 시간 이후여야 합니다.")
            }
            question.expiredAt = newExpiredAt
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

        // 정산된 질문은 삭제 불가
        if (question.status == QuestionStatus.SETTLED) {
            throw IllegalStateException("정산된 질문은 삭제할 수 없습니다.")
        }

        // 실제 베팅이 있는 질문은 삭제 불가 (초기 유동성만 있는 경우 삭제 가능)
        if (question.totalBetPool > INITIAL_LIQUIDITY * 2) {
            throw IllegalStateException("베팅이 있는 질문은 삭제할 수 없습니다.")
        }

        question.status = QuestionStatus.SETTLED
        questionRepository.save(question)

        return DeleteQuestionResponse(
            success = true,
            message = "질문이 삭제되었습니다."
        )
    }

    /**
     * 질문 목록 조회 (관리자용 - 모든 상태)
     * 페이지네이션 적용 및 N+1 쿼리 해결
     */
    @Transactional(readOnly = true)
    fun getAllQuestionsForAdmin(pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<QuestionAdminView> {
        return questionRepository.findAllQuestionsForAdmin(pageable).map { projection ->
            QuestionAdminView(
                id = projection.getId(),
                title = projection.getTitle(),
                category = projection.getCategory() ?: "",
                status = projection.getStatus().name,
                totalBetPool = projection.getTotalBetPool(),
                totalVotes = projection.getVoteCount(),
                expiredAt = projection.getExpiredAt().toString(),
                createdAt = projection.getCreatedAt().toString()
            )
        }
    }

    /**
     * 질문 목록 조회 (관리자용 - 모든 상태, 페이지네이션 없이 전체 조회)
     * 기존 호환성을 위해 유지
     */
    @Transactional(readOnly = true)
    fun getAllQuestionsForAdmin(): List<QuestionAdminView> {
        return getAllQuestionsForAdmin(org.springframework.data.domain.Pageable.unpaged()).content
    }

    /**
     * Duration 기반 질문 생성 (어드민용)
     * votingDuration, bettingDuration으로 자동 시간 계산
     */
    @Transactional
    fun createQuestionWithDuration(request: com.predata.backend.dto.AdminCreateQuestionRequest): QuestionCreationResponse {
        // OPINION 타입 질문 템플릿 검증
        if (request.marketType == com.predata.backend.domain.MarketType.OPINION) {
            if (!request.title.startsWith("시장은 ") || !request.title.endsWith("라고 생각할까요?")) {
                throw IllegalArgumentException("OPINION 타입 질문은 '시장은 ~라고 생각할까요?' 형식이어야 합니다")
            }
        }

        val voteResultSettlement = request.voteResultSettlement
            ?: (request.marketType == com.predata.backend.domain.MarketType.OPINION)
        if (request.marketType == com.predata.backend.domain.MarketType.OPINION && !voteResultSettlement) {
            throw IllegalArgumentException("OPINION 타입 질문은 voteResultSettlement=true 이어야 합니다")
        }

        val now = LocalDateTime.now()

        // 자동 시간 계산
        val votingEndAt = now.plusSeconds(request.votingDuration)
        val bettingStartAt = votingEndAt.plusMinutes(5) // 5분 휴식
        val bettingEndAt = bettingStartAt.plusSeconds(request.bettingDuration)

        // resolveAt, disputeUntil 파싱
        val resolveAt = request.resolveAt?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }
        val disputeUntil = request.disputeUntil?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }

        val question = Question(
            title = request.title,
            category = request.category,
            categoryWeight = BigDecimal.ONE,
            status = QuestionStatus.VOTING,
            type = request.type,
            marketType = request.marketType,
            resolutionRule = request.resolutionRule,
            resolutionSource = request.resolutionSource,
            resolveAt = resolveAt,
            disputeUntil = disputeUntil,
            voteResultSettlement = voteResultSettlement,
            votingEndAt = votingEndAt,
            bettingStartAt = bettingStartAt,
            bettingEndAt = bettingEndAt,
            totalBetPool = INITIAL_LIQUIDITY * 2,
            yesBetPool = INITIAL_LIQUIDITY,
            noBetPool = INITIAL_LIQUIDITY,
            initialYesPool = INITIAL_LIQUIDITY,
            initialNoPool = INITIAL_LIQUIDITY,
            finalResult = FinalResult.PENDING,
            expiredAt = bettingEndAt,
            createdAt = now
        )

        val savedQuestion = questionRepository.save(question)

        // 온체인에 질문 생성 (비동기)
        blockchainService.createQuestionOnChain(savedQuestion)

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
     * 질문 전체 하드 삭제 (연관 question_id 데이터 포함 정리)
     */
    @Transactional
    fun purgeAllQuestions(): PurgeQuestionsResponse {
        val existingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM questions", Int::class.java) ?: 0
        if (existingCount == 0) {
            return PurgeQuestionsResponse(
                success = true,
                deletedQuestions = 0,
                cleanedTables = emptyMap(),
                message = "삭제할 질문이 없습니다."
            )
        }

        val cleaned = linkedMapOf<String, Int>()

        // 생성 배치 히스토리에서 삭제된 질문 참조를 끊어 정합성 유지
        if (columnExists("question_generation_items", "published_question_id")) {
            val updated = jdbcTemplate.update(
                """
                UPDATE question_generation_items
                SET published_question_id = NULL,
                    status = CASE
                        WHEN status = 'PUBLISHED' THEN 'FAILED'
                        ELSE status
                    END,
                    reject_reason = CASE
                        WHEN reject_reason IS NULL OR reject_reason = '' THEN '질문 전체 초기화로 연결 해제'
                        ELSE reject_reason
                    END,
                    updated_at = NOW()
                WHERE published_question_id IS NOT NULL
                """.trimIndent()
            )
            if (updated > 0) cleaned["question_generation_items(published_link_reset)"] = updated
        }

        // 구형 sports_matches.question_id 연결 해제
        if (columnExists("sports_matches", "question_id")) {
            val updated = jdbcTemplate.update("UPDATE sports_matches SET question_id = NULL WHERE question_id IS NOT NULL")
            if (updated > 0) cleaned["sports_matches(question_link_reset)"] = updated
        }

        // question_id를 가진 모든 테이블의 연관 데이터 선삭제
        val dependentTables = jdbcTemplate.queryForList(
            """
            SELECT TABLE_NAME
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND COLUMN_NAME = 'question_id'
              AND TABLE_NAME <> 'questions'
            ORDER BY TABLE_NAME
            """.trimIndent(),
            String::class.java
        )

        dependentTables.forEach { table ->
            val affected = jdbcTemplate.update("DELETE FROM `$table` WHERE question_id IS NOT NULL")
            if (affected > 0) cleaned[table] = affected
        }

        val deletedQuestions = jdbcTemplate.update("DELETE FROM questions")
        cleaned["questions"] = deletedQuestions

        return PurgeQuestionsResponse(
            success = true,
            deletedQuestions = deletedQuestions,
            cleanedTables = cleaned,
            message = "질문 및 연관 데이터 초기화가 완료되었습니다."
        )
    }

    private fun columnExists(tableName: String, columnName: String): Boolean {
        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = ?
              AND COLUMN_NAME = ?
            """.trimIndent(),
            Int::class.java,
            tableName,
            columnName
        )
        return count > 0
    }
}

// ===== DTOs =====

data class CreateQuestionRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    val title: String,

    @field:NotBlank(message = "카테고리는 필수입니다.")
    val category: String,

    @field:NotBlank(message = "마감일은 필수입니다.")
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

data class PurgeQuestionsResponse(
    val success: Boolean,
    val deletedQuestions: Int,
    val cleanedTables: Map<String, Int>,
    val message: String
)
