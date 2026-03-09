package com.predata.backend.domain.policy

import java.math.BigDecimal

object WithdrawalPolicy {
    private val walletRegex = Regex("^0x[a-fA-F0-9]{40}$")

    fun validateAmount(amount: BigDecimal, minWithdraw: BigDecimal, maxWithdraw: BigDecimal) {
        if (amount < minWithdraw || amount > maxWithdraw) {
            throw IllegalArgumentException("Withdrawal amount must be between \$$minWithdraw and \$$maxWithdraw.")
        }
    }

    fun validateFee(fee: BigDecimal) {
        if (fee < BigDecimal.ZERO) {
            throw IllegalArgumentException("Withdrawal fee must be non-negative.")
        }
    }

    fun validateWalletAddressFormat(walletAddress: String) {
        if (!walletRegex.matches(walletAddress)) {
            throw IllegalArgumentException("Invalid wallet address.")
        }
    }

    fun validateRegisteredWallet(registeredWalletAddress: String?, requestWalletAddress: String) {
        if (registeredWalletAddress == null) {
            throw IllegalArgumentException("No wallet address registered. Please connect your wallet in My Page first.")
        }
        if (!requestWalletAddress.equals(registeredWalletAddress, ignoreCase = true)) {
            throw IllegalArgumentException("Wallet address does not match registered address. Please connect your wallet in My Page.")
        }
    }

    fun totalDebit(amount: BigDecimal, withdrawalFee: BigDecimal): BigDecimal = amount.add(withdrawalFee)
}
