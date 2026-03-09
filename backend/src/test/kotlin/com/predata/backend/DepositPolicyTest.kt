package com.predata.backend

import com.predata.backend.domain.policy.DepositPolicy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class DepositPolicyTest {

    @Test
    fun `expectedAmountRaw converts usdc decimals correctly`() {
        val actual = DepositPolicy.expectedAmountRaw(BigDecimal("12.345678"))
        assertEquals(BigInteger("12345678"), actual)
    }

    @Test
    fun `ensureMinimumDeposit throws when amount is below one usdc`() {
        val ex = assertThrows<IllegalArgumentException> {
            DepositPolicy.ensureMinimumDeposit(BigDecimal("0.999999"))
        }
        kotlin.test.assertTrue(ex.message!!.contains("at least"))
    }

    @Test
    fun `ensureWalletAddressMatches accepts case-insensitive address`() {
        DepositPolicy.ensureWalletAddressMatches(
            registeredWalletAddress = "0xAbC0000000000000000000000000000000000001",
            requestWalletAddress = "0xabc0000000000000000000000000000000000001"
        )
    }

    @Test
    fun `ensureFromAddressProvided throws on blank input`() {
        val ex = assertThrows<IllegalArgumentException> {
            DepositPolicy.ensureFromAddressProvided(" ")
        }
        kotlin.test.assertTrue(ex.message!!.contains("required"))
    }
}
