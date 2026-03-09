package com.predata.backend.domain.policy

import java.math.BigInteger

data class TransferEventCandidate(
    val fromAddress: String,
    val toAddress: String,
    val amountRaw: BigInteger,
)

object OnChainTransferPolicy {
    fun ensureReceiptPresent(isPresent: Boolean, txHash: String) {
        if (!isPresent) {
            throw IllegalArgumentException("Transaction not found. It may not be confirmed yet: $txHash")
        }
    }

    fun ensureTransactionSucceeded(status: String, txHash: String) {
        if (status != "0x1") {
            throw IllegalArgumentException("Transaction failed: $txHash")
        }
    }

    fun ensureTargetContract(actualTo: String?, expectedContract: String) {
        if (actualTo == null || !actualTo.equals(expectedContract, ignoreCase = true)) {
            throw IllegalArgumentException("Not a transaction to USDC contract.")
        }
    }

    fun selectAndValidateTransfer(
        candidates: List<TransferEventCandidate>,
        expectedReceiver: String,
        expectedSender: String,
        expectedAmountRaw: BigInteger,
    ): BigInteger {
        if (candidates.isEmpty()) {
            throw IllegalArgumentException("Transfer event not found.")
        }

        candidates.forEach { candidate ->
            if (!candidate.toAddress.equals(expectedReceiver, ignoreCase = true)) return@forEach

            if (!candidate.fromAddress.equals(expectedSender, ignoreCase = true)) {
                throw IllegalArgumentException(
                    "On-chain sender address mismatch. (expected: $expectedSender, actual: ${candidate.fromAddress})"
                )
            }

            if (candidate.amountRaw < expectedAmountRaw) {
                throw IllegalArgumentException(
                    "Insufficient transfer amount. (required: $expectedAmountRaw, actual: ${candidate.amountRaw})"
                )
            }
            return candidate.amountRaw
        }

        throw IllegalArgumentException("USDC transfer to receiver wallet not found.")
    }
}
