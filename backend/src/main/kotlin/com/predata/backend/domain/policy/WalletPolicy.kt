package com.predata.backend.domain.policy

import java.math.BigDecimal

object WalletPolicy {
    fun ensurePositive(amount: BigDecimal, action: String) {
        if (amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("$action amount must be positive.")
        }
    }

    fun ensureEnoughAvailable(availableBalance: BigDecimal, requiredAmount: BigDecimal) {
        if (availableBalance < requiredAmount) {
            throw IllegalArgumentException("Insufficient wallet balance. available=$availableBalance, required=$requiredAmount")
        }
    }

    fun ensureEnoughLocked(lockedBalance: BigDecimal, requiredAmount: BigDecimal) {
        if (lockedBalance < requiredAmount) {
            throw IllegalArgumentException("Insufficient locked balance. locked=$lockedBalance, required=$requiredAmount")
        }
    }
}
