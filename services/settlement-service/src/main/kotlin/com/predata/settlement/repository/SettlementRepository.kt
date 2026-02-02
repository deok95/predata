package com.predata.settlement.repository

import com.predata.settlement.domain.Settlement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun findByQuestionId(questionId: Long): Settlement?
    fun existsByQuestionId(questionId: Long): Boolean
}
