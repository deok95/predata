package com.predata.backend.repository.amm

import com.predata.backend.domain.MarketPool
import com.predata.backend.domain.PoolStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MarketPoolRepository : JpaRepository<MarketPool, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT mp FROM MarketPool mp WHERE mp.questionId = :questionId")
    fun findByIdWithLock(questionId: Long): Optional<MarketPool>

    fun findByStatus(status: PoolStatus): List<MarketPool>

    @Query("SELECT mp FROM MarketPool mp WHERE mp.questionId IN :questionIds")
    fun findAllByQuestionIds(questionIds: List<Long>): List<MarketPool>
}
