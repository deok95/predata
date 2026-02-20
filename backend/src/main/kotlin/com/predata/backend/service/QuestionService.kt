package com.predata.backend.service

import com.predata.backend.domain.ExecutionModel
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.dto.QuestionResponse
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.amm.MarketPoolRepository
import com.predata.backend.service.amm.FpmmMathEngine
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter

@Service
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val marketPoolRepository: MarketPoolRepository
) {

    private val formatter = DateTimeFormatter.ISO_DATE_TIME
    private val HUNDRED = BigDecimal("100")

    private fun toLongFloor(value: BigDecimal): Long =
        value.setScale(0, RoundingMode.DOWN).toLong()

    private data class AmmDerived(
        val totalVolume: Long,
        val yesPool: Long,
        val noPool: Long,
        val yesPct: Double,
        val noPct: Double
    )

    private fun deriveFromAmmPool(totalVolume: Long, pYes: BigDecimal, pNo: BigDecimal): AmmDerived {
        if (totalVolume <= 0L) {
            return AmmDerived(
                totalVolume = 0,
                yesPool = 0,
                noPool = 0,
                yesPct = pYes.multiply(HUNDRED).toDouble(),
                noPct = pNo.multiply(HUNDRED).toDouble()
            )
        }

        // Create synthetic yes/no pools so existing UI ratio logic works.
        val yesPool = pYes.multiply(BigDecimal.valueOf(totalVolume)).setScale(0, RoundingMode.DOWN).toLong()
        val noPool = (totalVolume - yesPool).coerceAtLeast(0)

        return AmmDerived(
            totalVolume = totalVolume,
            yesPool = yesPool,
            noPool = noPool,
            yesPct = pYes.multiply(HUNDRED).toDouble(),
            noPct = pNo.multiply(HUNDRED).toDouble()
        )
    }

    /**
     * 모든 질문 조회
     */
    @Transactional(readOnly = true)
    fun getAllQuestions(): List<QuestionResponse> {
        val questions = questionRepository.findAll()

        // Prefetch pools to avoid N+1 when rendering lists.
        // We key off pool existence (not executionModel) because executionModel can drift in dev data.
        val nonVotingIds = questions
            .filter { it.status != QuestionStatus.VOTING }
            .mapNotNull { it.id }
        val poolsById = if (nonVotingIds.isNotEmpty()) {
            marketPoolRepository.findAllByQuestionIds(nonVotingIds).associateBy { it.questionId }
        } else {
            emptyMap()
        }

        return questions.map { question ->
            // VOTING 상태에서는 풀 데이터를 마스킹
            val isVoting = question.status == QuestionStatus.VOTING

            val (totalBetPool, yesBetPool, noBetPool, yesPercentage, noPercentage) = if (isVoting) {
                Quint(0L, 0L, 0L, 50.0, 50.0)
            } else {
                val pool = question.id?.let { poolsById[it] }
                if (pool != null) {
                    val price = FpmmMathEngine.calculatePrice(pool.yesShares, pool.noShares)
                    val totalVolume = toLongFloor(pool.totalVolumeUsdc)
                    val derived = deriveFromAmmPool(totalVolume, price.pYes, price.pNo)
                    Quint(derived.totalVolume, derived.yesPool, derived.noPool, derived.yesPct, derived.noPct)
                } else {
                    // Pool 없으면 레거시 필드 사용
                    val total = question.totalBetPool.toDouble()
                    val yesPct = if (total > 0) (question.yesBetPool / total) * 100 else 0.0
                    val noPct = if (total > 0) (question.noBetPool / total) * 100 else 0.0
                    Quint(question.totalBetPool, question.yesBetPool, question.noBetPool, yesPct, noPct)
                }
            }

            QuestionResponse(
                id = question.id!!,
                title = question.title,
                category = question.category,
                status = question.status.name,
                type = question.marketType.name,
                executionModel = question.executionModel.name,
                finalResult = question.finalResult.name,
                totalBetPool = totalBetPool,
                yesBetPool = yesBetPool,
                noBetPool = noBetPool,
                yesPercentage = yesPercentage,
                noPercentage = noPercentage,
                sourceUrl = question.sourceUrl,
                disputeDeadline = question.disputeDeadline?.format(formatter),
                votingEndAt = question.votingEndAt.format(formatter),
                bettingStartAt = question.bettingStartAt.format(formatter),
                bettingEndAt = question.bettingEndAt.format(formatter),
                expiredAt = question.expiredAt.format(formatter),
                createdAt = question.createdAt.format(formatter),
                viewCount = question.viewCount,
                matchId = question.match?.id
            )
        }
    }

    /**
     * 특정 질문 조회
     */
    @Transactional(readOnly = true)
    fun getQuestion(id: Long): QuestionResponse? {
        val question = questionRepository.findById(id).orElse(null) ?: return null

        // VOTING 상태에서는 풀 데이터를 마스킹
        val isVoting = question.status == QuestionStatus.VOTING

        val (totalBetPool, yesBetPool, noBetPool, yesPercentage, noPercentage) = if (isVoting) {
            Quint(0L, 0L, 0L, 50.0, 50.0)
        } else {
            val pool = marketPoolRepository.findById(id).orElse(null)
            if (pool != null) {
                val price = FpmmMathEngine.calculatePrice(pool.yesShares, pool.noShares)
                val totalVolume = toLongFloor(pool.totalVolumeUsdc)
                val derived = deriveFromAmmPool(totalVolume, price.pYes, price.pNo)
                Quint(derived.totalVolume, derived.yesPool, derived.noPool, derived.yesPct, derived.noPct)
            } else {
                val total = question.totalBetPool.toDouble()
                val yesPct = if (total > 0) (question.yesBetPool / total) * 100 else 0.0
                val noPct = if (total > 0) (question.noBetPool / total) * 100 else 0.0
                Quint(question.totalBetPool, question.yesBetPool, question.noBetPool, yesPct, noPct)
            }
        }

        return QuestionResponse(
            id = question.id!!,
            title = question.title,
            category = question.category,
            status = question.status.name,
            type = question.marketType.name,
            executionModel = question.executionModel.name,
            finalResult = question.finalResult.name,
            totalBetPool = totalBetPool,
            yesBetPool = yesBetPool,
            noBetPool = noBetPool,
            yesPercentage = yesPercentage,
            noPercentage = noPercentage,
            sourceUrl = question.sourceUrl,
            disputeDeadline = question.disputeDeadline?.format(formatter),
            votingEndAt = question.votingEndAt.format(formatter),
            bettingStartAt = question.bettingStartAt.format(formatter),
            bettingEndAt = question.bettingEndAt.format(formatter),
            expiredAt = question.expiredAt.format(formatter),
            createdAt = question.createdAt.format(formatter),
            viewCount = question.viewCount,
            matchId = question.match?.id
        )
    }

    /**
     * 상태별 질문 조회
     */
    @Transactional(readOnly = true)
    fun getQuestionsByStatus(status: QuestionStatus): List<QuestionResponse> {
        val questions = questionRepository.findByStatus(status)

        val nonVotingIds = questions
            .filter { it.status != QuestionStatus.VOTING }
            .mapNotNull { it.id }
        val poolsById = if (nonVotingIds.isNotEmpty()) {
            marketPoolRepository.findAllByQuestionIds(nonVotingIds).associateBy { it.questionId }
        } else {
            emptyMap()
        }

        return questions.map { question ->
            // VOTING 상태에서는 풀 데이터를 마스킹
            val isVoting = question.status == QuestionStatus.VOTING

            val (totalBetPool, yesBetPool, noBetPool, yesPercentage, noPercentage) = if (isVoting) {
                Quint(0L, 0L, 0L, 50.0, 50.0)
            } else {
                val pool = question.id?.let { poolsById[it] }
                if (pool != null) {
                    val price = FpmmMathEngine.calculatePrice(pool.yesShares, pool.noShares)
                    val totalVolume = toLongFloor(pool.totalVolumeUsdc)
                    val derived = deriveFromAmmPool(totalVolume, price.pYes, price.pNo)
                    Quint(derived.totalVolume, derived.yesPool, derived.noPool, derived.yesPct, derived.noPct)
                } else {
                    val total = question.totalBetPool.toDouble()
                    val yesPct = if (total > 0) (question.yesBetPool / total) * 100 else 0.0
                    val noPct = if (total > 0) (question.noBetPool / total) * 100 else 0.0
                    Quint(question.totalBetPool, question.yesBetPool, question.noBetPool, yesPct, noPct)
                }
            }

            QuestionResponse(
                id = question.id!!,
                title = question.title,
                category = question.category,
                status = question.status.name,
                type = question.marketType.name,
                executionModel = question.executionModel.name,
                finalResult = question.finalResult.name,
                totalBetPool = totalBetPool,
                yesBetPool = yesBetPool,
                noBetPool = noBetPool,
                yesPercentage = yesPercentage,
                noPercentage = noPercentage,
                sourceUrl = question.sourceUrl,
                disputeDeadline = question.disputeDeadline?.format(formatter),
                votingEndAt = question.votingEndAt.format(formatter),
                bettingStartAt = question.bettingStartAt.format(formatter),
                bettingEndAt = question.bettingEndAt.format(formatter),
                expiredAt = question.expiredAt.format(formatter),
                createdAt = question.createdAt.format(formatter),
                viewCount = question.viewCount,
                matchId = question.match?.id
            )
        }
    }

    /**
     * 조회수 증가
     */
    @Transactional
    fun incrementViewCount(id: Long): Boolean {
        val question = questionRepository.findById(id).orElse(null) ?: return false
        question.viewCount++
        questionRepository.save(question)
        return true
    }
}

// Simple tuple helper to keep mapping code readable (and avoid destructuring Pair nesting).
private data class Quint(
    val a: Long,
    val b: Long,
    val c: Long,
    val d: Double,
    val e: Double
)
