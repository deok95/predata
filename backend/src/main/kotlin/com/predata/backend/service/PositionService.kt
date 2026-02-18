package com.predata.backend.service

import com.predata.backend.domain.OrderSide
import com.predata.backend.domain.MarketPosition
import com.predata.backend.repository.PositionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

// Removed: PositionUpdateResult (현금 이동 없으므로 불필요)

@Service
class PositionService(
    private val positionRepository: PositionRepository,
    private val auditService: AuditService,
    private val orderBookService: OrderBookService,
    private val questionRepository: com.predata.backend.repository.QuestionRepository,
    @Value("\${app.market-maker.member-id}") private val marketMakerMemberId: Long
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
                reservedQuantity = BigDecimal.ZERO,
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
     * Net Position: 양쪽 포지션 불허 정책 적용
     * - 한 질문당 유저는 YES 또는 NO 중 하나만 보유 가능
     * - 반대 체결 = 청산/리버설 (헤지 아님)
     * - 현금 이동 없음 (정산에서만 처리)
     * - 비관적 락으로 동시성 보장
     * - 예외: 마켓메이커는 양쪽 포지션 보유 가능
     */
    @Transactional
    fun netPosition(
        memberId: Long,
        questionId: Long,
        newSide: OrderSide,
        newQty: BigDecimal,
        newPrice: BigDecimal
    ) {
        // 마켓메이커는 양쪽 포지션 보유 허용 (netting 정책 예외)
        if (memberId == marketMakerMemberId) {
            createOrUpdatePosition(memberId, questionId, newSide, newQty, newPrice)
            return
        }

        // 1. Get opposite side position with pessimistic lock
        // Note: 현재는 opposite position 하나만 락. 향후 양쪽 락 필요 시 YES→NO 순서로 고정 필요
        val oppositeSide = if (newSide == OrderSide.YES) OrderSide.NO else OrderSide.YES
        val oppositePosition = positionRepository.findByMemberIdAndQuestionIdAndSideForUpdate(
            memberId, questionId, oppositeSide
        )

        // 2. If no opposite position, create/update normally
        if (oppositePosition == null) {
            createOrUpdatePosition(memberId, questionId, newSide, newQty, newPrice)
            return
        }

        // 3. Netting logic (수량만 조정)
        val oppositeQty = oppositePosition.quantity

        if (newQty <= oppositeQty) {
            // Case A: New quantity fully nets against opposite
            // Example: YES 10 보유, NO 3 매수 → YES 7 남음 (complete set 3개 소각)
            oppositePosition.quantity = oppositeQty.subtract(newQty)
            oppositePosition.updatedAt = LocalDateTime.now()

            if (oppositePosition.quantity == BigDecimal.ZERO) {
                // Close position if fully netted
                positionRepository.delete(oppositePosition)
                auditService.log(
                    memberId = memberId,
                    action = com.predata.backend.domain.AuditAction.POSITION_UPDATE,
                    entityType = "POSITION",
                    entityId = oppositePosition.id,
                    detail = "Position fully netted and closed: $oppositeSide qty=${oppositeQty} (complete set burned: $newQty)"
                )
            } else {
                positionRepository.save(oppositePosition)
                auditService.log(
                    memberId = memberId,
                    action = com.predata.backend.domain.AuditAction.POSITION_UPDATE,
                    entityType = "POSITION",
                    entityId = oppositePosition.id,
                    detail = "Position netted: $oppositeSide ${oppositeQty} → ${oppositePosition.quantity} (complete set burned: $newQty)"
                )
            }

        } else {
            // Case B: New quantity exceeds opposite
            // Example: YES 5 보유, NO 8 매수 → YES 0 (complete set 5개 소각), NO 3 신규
            val remainingQty = newQty.subtract(oppositeQty)

            // Close opposite position
            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.POSITION_UPDATE,
                entityType = "POSITION",
                entityId = oppositePosition.id,
                detail = "Position fully netted and closed: $oppositeSide qty=${oppositeQty} (complete set burned: $oppositeQty)"
            )
            positionRepository.delete(oppositePosition)

            // Create new position with remaining quantity
            createOrUpdatePosition(
                memberId, questionId, newSide, remainingQty, newPrice
            )
        }
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
     * SELL 체결 시 포지션 감소 (비관적 락 + 영속성 + 0 삭제)
     * - SELL 주문 체결 시 보유 포지션 감소
     * - quantity가 0이 되면 포지션 삭제
     */
    @Transactional
    fun decreasePosition(
        memberId: Long,
        questionId: Long,
        side: OrderSide,
        decreaseAmount: BigDecimal
    ) {
        // 비관적 락으로 포지션 조회
        val position = positionRepository.findByMemberIdAndQuestionIdAndSideForUpdate(
            memberId, questionId, side
        ) ?: throw IllegalStateException("Position not found for SELL execution")

        // SELL 체결: 예약 수량(reserved)과 실제 보유(quantity)를 함께 감소
        // reserved는 체결 수량만큼 우선 해제된다.
        if (position.reservedQuantity > BigDecimal.ZERO) {
            position.reservedQuantity = position.reservedQuantity.subtract(decreaseAmount)
            if (position.reservedQuantity < BigDecimal.ZERO) {
                position.reservedQuantity = BigDecimal.ZERO
            }
        }

        // 실제 보유 수량 감소
        position.quantity = position.quantity.subtract(decreaseAmount)
        position.updatedAt = LocalDateTime.now()

        if (position.quantity <= BigDecimal.ZERO) {
            // 포지션이 0 이하가 되면 삭제
            positionRepository.delete(position)
            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.POSITION_UPDATE,
                entityType = "POSITION",
                entityId = position.id,
                detail = "Position closed (SELL): $side quantity reduced to 0"
            )
        } else {
            // 포지션이 남아있으면 저장
            positionRepository.save(position)
            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.POSITION_UPDATE,
                entityType = "POSITION",
                entityId = position.id,
                detail = "Position decreased (SELL): $side quantity ${position.quantity.add(decreaseAmount)} → ${position.quantity}"
            )
        }
    }

    /**
     * SELL 주문 생성 시 포지션 예약(reserve)
     * - 초과판매 방지: (quantity - reservedQuantity) >= reserveAmount 이어야 함
     * - 비관적 락으로 동시성 정합성 확보
     */
    @Transactional
    fun reserveForSellOrder(
        memberId: Long,
        questionId: Long,
        side: OrderSide,
        reserveAmount: BigDecimal
    ) {
        val position = positionRepository.findByMemberIdAndQuestionIdAndSideForUpdate(memberId, questionId, side)
            ?: throw IllegalStateException("Position not found for SELL order")

        val available = position.quantity.subtract(position.reservedQuantity)
        if (available < reserveAmount) {
            throw IllegalStateException("INSUFFICIENT_AVAILABLE_TO_SELL:${available.setScale(0, RoundingMode.DOWN)}")
        }

        position.reservedQuantity = position.reservedQuantity.add(reserveAmount)
        position.updatedAt = LocalDateTime.now()
        positionRepository.save(position)
    }

    /**
     * SELL 주문 취소/IOC 미체결 시 예약 해제(release)
     */
    @Transactional
    fun releaseSellReservation(
        memberId: Long,
        questionId: Long,
        side: OrderSide,
        releaseAmount: BigDecimal
    ) {
        val position = positionRepository.findByMemberIdAndQuestionIdAndSideForUpdate(memberId, questionId, side)
            ?: return

        position.reservedQuantity = position.reservedQuantity.subtract(releaseAmount)
        if (position.reservedQuantity < BigDecimal.ZERO) {
            position.reservedQuantity = BigDecimal.ZERO
        }
        position.updatedAt = LocalDateTime.now()
        positionRepository.save(position)
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

    /**
     * 시스템 전용: 초기 포지션 부여 (마켓메이커 시딩용)
     * - 양쪽 포지션 정책 체크를 우회 (마켓메이커 전용)
     * - 비관적 락으로 동시성 보장
     * - avgPrice는 0으로 설정 (시딩용 포지션)
     */
    @Transactional
    fun grantInitialPosition(
        memberId: Long,
        questionId: Long,
        side: OrderSide,
        qty: BigDecimal
    ) {
        val existingPosition = positionRepository.findByMemberIdAndQuestionIdAndSideForUpdate(
            memberId, questionId, side
        )

        if (existingPosition != null) {
            // 기존 포지션이 있으면 수량만 증가 (평균 가격은 0 유지)
            existingPosition.quantity = existingPosition.quantity.add(qty)
            existingPosition.updatedAt = LocalDateTime.now()
            positionRepository.save(existingPosition)

            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.POSITION_UPDATE,
                entityType = "POSITION",
                entityId = existingPosition.id,
                detail = "Market maker initial position increased: ${side} qty=${existingPosition.quantity}"
            )
        } else {
            // 신규 포지션 생성 (평균 가격 0으로 시딩)
            val newPosition = MarketPosition(
                memberId = memberId,
                questionId = questionId,
                side = side,
                quantity = qty,
                reservedQuantity = BigDecimal.ZERO,
                avgPrice = BigDecimal.ZERO,
                settled = false
            )
            val savedPosition = positionRepository.save(newPosition)

            auditService.log(
                memberId = memberId,
                action = com.predata.backend.domain.AuditAction.POSITION_UPDATE,
                entityType = "POSITION",
                entityId = savedPosition.id,
                detail = "Market maker initial position granted: ${side} qty=${qty}"
            )
        }
    }
}
