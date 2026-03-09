package com.predata.backend.domain.policy

import java.math.BigDecimal
import java.math.BigInteger

object DepositPolicy {
    private const val USDC_DECIMALS = 6

    fun ensureFromAddressProvided(fromAddress: String?) {
        if (fromAddress.isNullOrBlank()) {
            throw IllegalArgumentException("Sender wallet address (fromAddress) is required.")
        }
    }

    fun ensureRegisteredWallet(registeredWalletAddress: String?) {
        if (registeredWalletAddress == null) {
            throw IllegalArgumentException("No registered wallet address. Please connect your wallet in My Page first.")
        }
    }

    fun ensureWalletAddressMatches(registeredWalletAddress: String, requestWalletAddress: String) {
        if (!requestWalletAddress.equals(registeredWalletAddress, ignoreCase = true)) {
            throw IllegalArgumentException(
                "Wallet address does not match registered address. (registered: $registeredWalletAddress, requested: $requestWalletAddress)"
            )
        }
    }

    fun ensureMinimumDeposit(amount: BigDecimal) {
        if (amount < BigDecimal.ONE) {
            throw IllegalArgumentException("Deposit amount must be at least \$1.")
        }
    }

    fun expectedAmountRaw(amount: BigDecimal): BigInteger =
        amount.multiply(BigDecimal.TEN.pow(USDC_DECIMALS)).toBigInteger()
}
