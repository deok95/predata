package com.predata.backend.repository

import com.predata.backend.domain.OrderSide
import com.predata.backend.domain.MarketPosition
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PositionRepository : JpaRepository<MarketPosition, Long> {

    /**
     * 특정 멤버의 특정 질문에 대한 특정 포지션 조회 (낙관적 락)
     */
    fun findByMemberIdAndQuestionIdAndSide(
        memberId: Long,
        questionId: Long,
        side: OrderSide
    ): MarketPosition?

    /**
     * 특정 멤버의 모든 포지션 조회
     */
    fun findByMemberId(memberId: Long): List<MarketPosition>

    /**
     * 특정 질문의 모든 포지션 조회
     */
    fun findByQuestionId(questionId: Long): List<MarketPosition>

    /**
     * 특정 멤버의 특정 질문에 대한 모든 포지션 조회
     */
    fun findByMemberIdAndQuestionId(memberId: Long, questionId: Long): List<MarketPosition>

    /**
     * 특정 질문의 정산되지 않은 포지션 조회
     */
    fun findByQuestionIdAndSettledFalse(questionId: Long): List<MarketPosition>

    /**
     * 특정 질문의 특정 side 포지션 조회 (정산용)
     */
    fun findByQuestionIdAndSideAndSettledFalse(questionId: Long, side: OrderSide): List<MarketPosition>
}
