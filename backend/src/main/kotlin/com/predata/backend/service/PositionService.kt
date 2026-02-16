package com.predata.backend.service

import com.predata.backend.domain.OrderSide
import com.predata.backend.domain.MarketPosition
import com.predata.backend.repository.PositionRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class PositionService(
    private val positionRepository: PositionRepository,
    private val auditService: AuditService,
    private val orderBookService: OrderBookService,
    private val questionRepository: com.predata.backend.repository.QuestionRepository
) {

    /**
     * 포지션 생성 또는 업데이트
     * - 기존 포지션이 있으면 가중평균으로 업데이트
     * - 없으면 새로 생성
     */
    @Transactional
    fun createOrUpdatePosition(
        memberId: Long,
        questionId: Long,
        side: OrderSide,
        qty: BigDecimal,
        price: BigDecimal
    ): MarketPosition {
        val existingPosition = positionRepository.findByMemberIdAndQuestionIdAndSide(memberId, questionId, side)
        if (existingPosition != null) {
            return updatePosition(existingPosition, qty, price, memberId, side)
        }

        // 신규 insert 경합 시 UNIQUE 충돌이 날 수 있으므로 1회 재조회 후 update로 복구
        return try {
            val newPosition = MarketPosition(
                memberId = memberId,
                questionId = questionId,
                side = side,
                quantity = qty,
                avgPrice = price,
                settled = false
            )
            val savedPosition = positionRepository.save(newPosition)
            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.POSITION_UPDATE,
                entityType = "POSITION",
                entityId = savedPosition.id,
                detail = "Position created: ${side} qty=${qty} avgPrice=${price}"
            )
            savedPosition
        } catch (e: DataIntegrityViolationException) {
            val concurrentPosition = positionRepository.findByMemberIdAndQuestionIdAndSide(memberId, questionId, side)
                ?: throw e
            updatePosition(concurrentPosition, qty, price, memberId, side)
        }
    }

    private fun updatePosition(
        position: MarketPosition,
        qty: BigDecimal,
        price: BigDecimal,
        memberId: Long,
        side: OrderSide
    ): MarketPosition {
        val totalQty = position.quantity.add(qty)
        val totalCost = position.quantity.multiply(position.avgPrice).add(qty.multiply(price))
        val newAvgPrice = totalCost.divide(totalQty, 2, RoundingMode.HALF_UP)

        position.quantity = totalQty
        position.avgPrice = newAvgPrice
        position.updatedAt = LocalDateTime.now()

        val savedPosition = positionRepository.save(position)
        auditService.log(
            memberId = memberId,
            action = com.predata.backend.domain.AuditAction.POSITION_UPDATE,
            entityType = "POSITION",
            entityId = savedPosition.id,
            detail = "Position updated: ${side} qty=${totalQty} avgPrice=${newAvgPrice}"
        )
        return savedPosition
    }

    /**
     * 특정 멤버의 모든 포지션 조회
     */
    fun getPositions(memberId: Long): List<MarketPosition> {
        return positionRepository.findByMemberId(memberId)
    }

    /**
     * 특정 멤버의 특정 질문에 대한 포지션 조회
     */
    fun getPositionsByQuestion(memberId: Long, questionId: Long): List<MarketPosition> {
        return positionRepository.findByMemberIdAndQuestionId(memberId, questionId)
    }

    /**
     * 특정 질문의 모든 포지션 조회
     */
    fun getPositionsByQuestionId(questionId: Long): List<MarketPosition> {
        return positionRepository.findByQuestionId(questionId)
    }

    /**
     * 정산 완료 마킹
     */
    @Transactional
    fun markAsSettled(questionId: Long) {
        val positions = positionRepository.findByQuestionIdAndSettledFalse(questionId)
        positions.forEach { position ->
            position.settled = true
            position.updatedAt = LocalDateTime.now()
        }
        positionRepository.saveAll(positions)
    }

    /**
     * PnL을 포함한 포지션 조회
     * - currentMidPrice를 기반으로 unrealizedPnL 계산
     * - N+1 문제 해결: questionId IN 쿼리 및 price 배치 조회
     */
    fun getPositionsWithPnL(memberId: Long): List<com.predata.backend.dto.PositionResponse> {
        val positions = positionRepository.findByMemberId(memberId)
        if (positions.isEmpty()) return emptyList()

        // 1. questionId 배치 조회 (N+1 해결)
        val questionIds = positions.map { it.questionId }.distinct()
        val questionsMap = questionRepository.findAllById(questionIds).associateBy { it.id }

        // 2. mid-price 배치 조회 (캐시 또는 배치 조회)
        val priceInfoMap = questionIds.associateWith { questionId ->
            orderBookService.getPriceInfo(questionId)
        }

        return positions.map { position ->
            val question = questionsMap[position.questionId]
            val priceInfo = priceInfoMap[position.questionId]
            val currentMidPrice = priceInfo?.midPrice

            // PnL 계산
            // YES: unrealizedPnL = (currentMidPrice - avgPrice) * quantity
            // NO: unrealizedPnL = ((1 - currentMidPrice) - avgPrice) * quantity
            val unrealizedPnL = if (currentMidPrice != null) {
                val effectivePrice = if (position.side == OrderSide.YES) {
                    currentMidPrice
                } else {
                    BigDecimal.ONE.subtract(currentMidPrice)
                }
                (effectivePrice.subtract(position.avgPrice)).multiply(position.quantity)
            } else {
                BigDecimal.ZERO
            }

            com.predata.backend.dto.PositionResponse(
                positionId = position.id ?: 0L,
                questionId = position.questionId,
                questionTitle = question?.title ?: "Unknown",
                side = position.side,
                quantity = position.quantity,
                avgPrice = position.avgPrice,
                currentMidPrice = currentMidPrice,
                unrealizedPnL = unrealizedPnL,
                createdAt = position.createdAt,
                updatedAt = position.updatedAt
            )
        }
    }
}
