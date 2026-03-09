package com.predata.backend

import com.predata.backend.domain.ExecutionModel
import com.predata.backend.domain.PoolStatus
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.policy.AmmTradePolicy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertTrue

class AmmTradePolicyTest {
    @Test
    fun `ensureAmmQuestion throws when execution model is not fpmm`() {
        val ex = assertThrows<IllegalArgumentException> {
            AmmTradePolicy.ensureAmmQuestion(ExecutionModel.ORDERBOOK_LEGACY)
        }
        assertTrue(ex.message!!.contains("AMM 실행 모델"))
    }

    @Test
    fun `ensurePoolActive throws when pool is not active`() {
        val ex = assertThrows<IllegalArgumentException> {
            AmmTradePolicy.ensurePoolActive(PoolStatus.SETTLED)
        }
        assertTrue(ex.message!!.contains("활성화"))
    }

    @Test
    fun `ensureSeedRequest validates bounds`() {
        val ex = assertThrows<IllegalArgumentException> {
            AmmTradePolicy.ensureSeedRequest(
                seedUsdc = BigDecimal.ZERO,
                feeRate = BigDecimal("0.01"),
                zero = BigDecimal.ZERO,
                one = BigDecimal.ONE
            )
        }
        assertTrue(ex.message!!.contains("시드"))
    }

    @Test
    fun `ensureSeedableQuestion blocks settled and cancelled`() {
        val ex = assertThrows<IllegalArgumentException> {
            AmmTradePolicy.ensureSeedableQuestion(QuestionStatus.SETTLED)
        }
        assertTrue(ex.message!!.contains("풀을 생성"))
    }
}
