package com.predata.backend

import com.predata.backend.domain.policy.OnChainTransferPolicy
import com.predata.backend.domain.policy.TransferEventCandidate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger
import kotlin.test.assertEquals

class OnChainTransferPolicyTest {
    @Test
    fun `ensureReceiptPresent throws when receipt missing`() {
        val ex = assertThrows<IllegalArgumentException> {
            OnChainTransferPolicy.ensureReceiptPresent(
                isPresent = false,
                txHash = "0xtxhash"
            )
        }
        kotlin.test.assertTrue(ex.message!!.contains("not found"))
    }

    @Test
    fun `ensureTransactionSucceeded throws on reverted status`() {
        val ex = assertThrows<IllegalArgumentException> {
            OnChainTransferPolicy.ensureTransactionSucceeded(
                status = "0x0",
                txHash = "0xtxhash"
            )
        }
        kotlin.test.assertTrue(ex.message!!.contains("failed"))
    }

    @Test
    fun `ensureTargetContract throws when contract mismatches`() {
        val ex = assertThrows<IllegalArgumentException> {
            OnChainTransferPolicy.ensureTargetContract(
                actualTo = "0x1111000000000000000000000000000000000000",
                expectedContract = "0x2222000000000000000000000000000000000000"
            )
        }
        kotlin.test.assertTrue(ex.message!!.contains("USDC contract"))
    }

    @Test
    fun `selectAndValidateTransfer returns amount when sender receiver amount match`() {
        val expected = BigInteger("1000000")
        val candidates = listOf(
            TransferEventCandidate(
                fromAddress = "0xabc0000000000000000000000000000000000001",
                toAddress = "0xdef0000000000000000000000000000000000002",
                amountRaw = expected
            )
        )

        val actual = OnChainTransferPolicy.selectAndValidateTransfer(
            candidates = candidates,
            expectedReceiver = "0xdef0000000000000000000000000000000000002",
            expectedSender = "0xabc0000000000000000000000000000000000001",
            expectedAmountRaw = expected
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `selectAndValidateTransfer throws when sender mismatches`() {
        val candidates = listOf(
            TransferEventCandidate(
                fromAddress = "0xabc0000000000000000000000000000000000009",
                toAddress = "0xdef0000000000000000000000000000000000002",
                amountRaw = BigInteger("1000000")
            )
        )

        val ex = assertThrows<IllegalArgumentException> {
            OnChainTransferPolicy.selectAndValidateTransfer(
                candidates = candidates,
                expectedReceiver = "0xdef0000000000000000000000000000000000002",
                expectedSender = "0xabc0000000000000000000000000000000000001",
                expectedAmountRaw = BigInteger("1000000")
            )
        }

        kotlin.test.assertTrue(ex.message!!.contains("sender address mismatch"))
    }

    @Test
    fun `selectAndValidateTransfer throws when amount is insufficient`() {
        val candidates = listOf(
            TransferEventCandidate(
                fromAddress = "0xabc0000000000000000000000000000000000001",
                toAddress = "0xdef0000000000000000000000000000000000002",
                amountRaw = BigInteger("999999")
            )
        )

        val ex = assertThrows<IllegalArgumentException> {
            OnChainTransferPolicy.selectAndValidateTransfer(
                candidates = candidates,
                expectedReceiver = "0xdef0000000000000000000000000000000000002",
                expectedSender = "0xabc0000000000000000000000000000000000001",
                expectedAmountRaw = BigInteger("1000000")
            )
        }

        kotlin.test.assertTrue(ex.message!!.contains("Insufficient transfer amount"))
    }

    @Test
    fun `selectAndValidateTransfer throws when receiver transfer is missing`() {
        val candidates = listOf(
            TransferEventCandidate(
                fromAddress = "0xabc0000000000000000000000000000000000001",
                toAddress = "0x9999000000000000000000000000000000000009",
                amountRaw = BigInteger("1000000")
            )
        )

        val ex = assertThrows<IllegalArgumentException> {
            OnChainTransferPolicy.selectAndValidateTransfer(
                candidates = candidates,
                expectedReceiver = "0xdef0000000000000000000000000000000000002",
                expectedSender = "0xabc0000000000000000000000000000000000001",
                expectedAmountRaw = BigInteger("1000000")
            )
        }

        kotlin.test.assertTrue(ex.message!!.contains("receiver wallet not found"))
    }
}
