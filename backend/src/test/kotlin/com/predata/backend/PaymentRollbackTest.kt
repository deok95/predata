package com.predata.backend

import com.predata.backend.domain.Member
import com.predata.backend.domain.PaymentTransaction
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.PaymentTransactionRepository
import com.predata.backend.service.PaymentVerificationService
import com.predata.backend.service.WithdrawalService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentRollbackTest {

    @Autowired
    private lateinit var paymentVerificationService: PaymentVerificationService

    @Autowired
    private lateinit var withdrawalService: WithdrawalService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var paymentTransactionRepository: PaymentTransactionRepository

    private val walletA = "0x1111111111111111111111111111111111111111"
    private val walletB = "0x2222222222222222222222222222222222222222"

    @Test
    fun `1 - duplicate txHash deposit throws and balance unchanged`() {
        val member = createMember(balance = "100.00", wallet = walletA)
        val initialBalance = currentBalance(idOf(member))

        paymentTransactionRepository.save(
            PaymentTransaction(
                memberId = idOf(member),
                txHash = "0xabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcd",
                amount = BigDecimal("10.00"),
                type = "DEPOSIT",
                status = "CONFIRMED"
            )
        )

        assertThrows<IllegalArgumentException> {
            paymentVerificationService.verifyDeposit(
                memberId = idOf(member),
                txHash = "0xabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcd",
                amount = BigDecimal("10.00"),
                fromAddress = walletA
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `2 - deposit below minimum throws and balance unchanged`() {
        val member = createMember(balance = "100.00", wallet = walletA)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            paymentVerificationService.verifyDeposit(
                memberId = idOf(member),
                txHash = "0xdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdef1",
                amount = BigDecimal("0.5"),
                fromAddress = walletA
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `3 - deposit without fromAddress throws and balance unchanged`() {
        val member = createMember(balance = "100.00", wallet = walletA)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            paymentVerificationService.verifyDeposit(
                memberId = idOf(member),
                txHash = "0xdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdef2",
                amount = BigDecimal("10.00"),
                fromAddress = null
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `4 - deposit with mismatched wallet throws and balance unchanged`() {
        val member = createMember(balance = "100.00", wallet = walletA)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            paymentVerificationService.verifyDeposit(
                memberId = idOf(member),
                txHash = "0xdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdef3",
                amount = BigDecimal("10.00"),
                fromAddress = walletB
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `5 - deposit without registered wallet throws and balance unchanged`() {
        val member = createMember(balance = "100.00", wallet = null)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            paymentVerificationService.verifyDeposit(
                memberId = idOf(member),
                txHash = "0xdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdefdef4",
                amount = BigDecimal("10.00"),
                fromAddress = walletA
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `6 - withdraw with insufficient balance throws and balance unchanged`() {
        val member = createMember(balance = "0.50", wallet = walletA)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            withdrawalService.withdraw(
                memberId = idOf(member),
                amount = BigDecimal("1.00"),
                walletAddress = walletA
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `7 - banned user withdraw throws and balance unchanged`() {
        val member = createMember(balance = "100.00", wallet = walletA, banned = true)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            withdrawalService.withdraw(
                memberId = idOf(member),
                amount = BigDecimal("10.00"),
                walletAddress = walletA
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `8 - withdraw below minimum throws and balance unchanged`() {
        val member = createMember(balance = "100.00", wallet = walletA)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            withdrawalService.withdraw(
                memberId = idOf(member),
                amount = BigDecimal("0.50"),
                walletAddress = walletA
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `9 - withdraw above maximum throws and balance unchanged`() {
        val member = createMember(balance = "500.00", wallet = walletA)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            withdrawalService.withdraw(
                memberId = idOf(member),
                amount = BigDecimal("101.00"),
                walletAddress = walletA
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `10 - withdraw with invalid wallet format throws and balance unchanged`() {
        val member = createMember(balance = "100.00", wallet = walletA)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            withdrawalService.withdraw(
                memberId = idOf(member),
                amount = BigDecimal("10.00"),
                walletAddress = "invalid-wallet"
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `11 - withdraw with different wallet than registered throws and balance unchanged`() {
        val member = createMember(balance = "100.00", wallet = walletA)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            withdrawalService.withdraw(
                memberId = idOf(member),
                amount = BigDecimal("10.00"),
                walletAddress = walletB
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    @Test
    fun `12 - withdraw without registered wallet throws and balance unchanged`() {
        val member = createMember(balance = "100.00", wallet = null)
        val initialBalance = currentBalance(idOf(member))

        assertThrows<IllegalArgumentException> {
            withdrawalService.withdraw(
                memberId = idOf(member),
                amount = BigDecimal("10.00"),
                walletAddress = walletA
            )
        }

        assertBigDecimalEquals(initialBalance, currentBalance(idOf(member)))
    }

    private fun createMember(balance: String, wallet: String?, banned: Boolean = false): Member {
        return memberRepository.save(
            Member(
                email = "rollback-${System.nanoTime()}@example.com",
                countryCode = "KR",
                walletAddress = wallet,
                usdcBalance = BigDecimal(balance),
                isBanned = banned,
                banReason = if (banned) "E2E banned user test" else null
            )
        )
    }

    private fun currentBalance(memberId: Long): BigDecimal {
        return memberRepository.findById(memberId).orElseThrow().usdcBalance
    }

    private fun idOf(member: Member): Long {
        return member.id ?: throw IllegalStateException("member id is null")
    }

    private fun assertBigDecimalEquals(expected: BigDecimal, actual: BigDecimal) {
        assertEquals(0, expected.compareTo(actual), "balance must remain unchanged")
    }
}
