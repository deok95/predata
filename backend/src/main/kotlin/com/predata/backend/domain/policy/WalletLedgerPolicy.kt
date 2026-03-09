package com.predata.backend.domain.policy

import com.predata.backend.domain.WalletLedgerDirection
import java.math.BigDecimal

enum class WalletOperation {
    LOCK_WITHDRAWAL,
    UNLOCK_WITHDRAWAL,
    SETTLE_LOCKED_WITHDRAWAL,
    DEBIT,
    CREDIT,
}

object WalletLedgerPolicy {
    fun directionFor(operation: WalletOperation): WalletLedgerDirection {
        return when (operation) {
            WalletOperation.LOCK_WITHDRAWAL,
            WalletOperation.SETTLE_LOCKED_WITHDRAWAL,
            WalletOperation.DEBIT -> WalletLedgerDirection.DEBIT
            WalletOperation.UNLOCK_WITHDRAWAL,
            WalletOperation.CREDIT -> WalletLedgerDirection.CREDIT
        }
    }

    fun treasuryInflowAmount(amount: BigDecimal): BigDecimal = amount

    fun treasuryOutflowAmount(amount: BigDecimal): BigDecimal = amount.negate()
}
