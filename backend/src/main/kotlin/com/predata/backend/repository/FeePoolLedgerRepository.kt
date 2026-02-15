package com.predata.backend.repository

import com.predata.backend.domain.FeePoolAction
import com.predata.backend.domain.FeePoolLedger
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FeePoolLedgerRepository : JpaRepository<FeePoolLedger, Long> {
    /**
     * 수수료 풀 ID로 원장 조회 (최신순)
     */
    fun findByFeePoolIdOrderByCreatedAtDesc(feePoolId: Long): List<FeePoolLedger>

    /**
     * 수수료 풀 ID와 액션으로 원장 조회
     */
    fun findByFeePoolIdAndActionOrderByCreatedAtDesc(
        feePoolId: Long,
        action: FeePoolAction
    ): List<FeePoolLedger>
}
