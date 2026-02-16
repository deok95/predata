package com.predata.backend.repository

import com.predata.backend.domain.FeePool
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FeePoolRepository : JpaRepository<FeePool, Long> {
    /**
     * 질문 ID로 수수료 풀 조회
     */
    fun findByQuestionId(questionId: Long): Optional<FeePool>

    /**
     * 질문 ID로 수수료 풀 존재 여부 확인
     */
    fun existsByQuestionId(questionId: Long): Boolean
}
