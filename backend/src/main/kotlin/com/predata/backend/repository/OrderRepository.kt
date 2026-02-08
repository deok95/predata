package com.predata.backend.repository

import com.predata.backend.domain.Order
import com.predata.backend.domain.OrderSide
import com.predata.backend.domain.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import jakarta.persistence.LockModeType
import java.math.BigDecimal

@Repository
interface OrderRepository : JpaRepository<Order, Long> {

    /**
     * 오더북 조회: YES 측 Bids (매수) - 높은 가격 우선
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.questionId = :questionId
        AND o.side = 'YES' AND o.orderType = 'BUY'
        AND o.status IN ('OPEN', 'PARTIAL')
        ORDER BY o.price DESC, o.createdAt ASC
    """)
    fun findYesBids(questionId: Long): List<Order>

    /**
     * 오더북 조회: NO 측 Asks (매도) - 낮은 가격 우선
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.questionId = :questionId
        AND o.side = 'NO' AND o.orderType = 'BUY'
        AND o.status IN ('OPEN', 'PARTIAL')
        ORDER BY o.price ASC, o.createdAt ASC
    """)
    fun findNoAsks(questionId: Long): List<Order>

    /**
     * 매칭 가능한 반대 주문 조회 (비관적 락)
     * YES 주문 시: NO 주문 중 (1 - price) 이상인 것들
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o FROM Order o
        WHERE o.questionId = :questionId
        AND o.side = :side AND o.orderType = 'BUY'
        AND o.status IN ('OPEN', 'PARTIAL')
        AND o.price >= :price
        ORDER BY o.price DESC, o.createdAt ASC
    """)
    fun findMatchableOrdersWithLock(
        questionId: Long,
        side: OrderSide,
        price: BigDecimal
    ): List<Order>

    /**
     * 회원의 활성 주문 조회
     */
    fun findByMemberIdAndStatusIn(memberId: Long, statuses: List<OrderStatus>): List<Order>

    /**
     * 질문의 활성 주문 조회
     */
    fun findByQuestionIdAndStatusIn(questionId: Long, statuses: List<OrderStatus>): List<Order>

    /**
     * 특정 주문 락
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    fun findByIdWithLock(id: Long): Order?

    /**
     * 회원의 특정 질문에 대한 주문 조회
     */
    fun findByMemberIdAndQuestionId(memberId: Long, questionId: Long): List<Order>
}
