package com.predata.backend

import com.predata.backend.domain.WalletLedgerDirection
import com.predata.backend.domain.policy.WalletLedgerPolicy
import com.predata.backend.domain.policy.WalletOperation
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class WalletLedgerPolicyTest {

    @Test
    fun `direction mapping is deterministic`() {
        assertEquals(WalletLedgerDirection.DEBIT, WalletLedgerPolicy.directionFor(WalletOperation.LOCK_WITHDRAWAL))
        assertEquals(WalletLedgerDirection.CREDIT, WalletLedgerPolicy.directionFor(WalletOperation.UNLOCK_WITHDRAWAL))
        assertEquals(WalletLedgerDirection.DEBIT, WalletLedgerPolicy.directionFor(WalletOperation.SETTLE_LOCKED_WITHDRAWAL))
        assertEquals(WalletLedgerDirection.DEBIT, WalletLedgerPolicy.directionFor(WalletOperation.DEBIT))
        assertEquals(WalletLedgerDirection.CREDIT, WalletLedgerPolicy.directionFor(WalletOperation.CREDIT))
    }

    @Test
    fun `treasury amount sign follows inflow outflow policy`() {
        val amount = BigDecimal("12.34")
        assertEquals(BigDecimal("12.34"), WalletLedgerPolicy.treasuryInflowAmount(amount))
        assertEquals(BigDecimal("-12.34"), WalletLedgerPolicy.treasuryOutflowAmount(amount))
    }
}
