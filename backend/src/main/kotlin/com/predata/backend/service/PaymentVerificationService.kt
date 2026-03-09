package com.predata.backend.service

import com.predata.backend.domain.policy.DepositPolicy
import com.predata.backend.domain.policy.OnChainTransferPolicy
import com.predata.backend.domain.policy.TransferEventCandidate
import com.predata.backend.domain.PaymentTransaction
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.PaymentTransactionRepository
import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigDecimal
import java.math.BigInteger

@Service
class PaymentVerificationService(
    @Value("\${polygon.rpc-url}") private val rpcUrl: String,
    @Value("\${polygon.receiver-wallet}") private val receiverWallet: String,
    @Value("\${polygon.usdc-contract}") private val usdcContract: String,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val memberRepository: MemberRepository,
    private val transactionHistoryService: TransactionHistoryService,
    private val walletBalanceService: WalletBalanceService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val web3j: Web3j by lazy { Web3j.build(HttpService(rpcUrl)) }

    // ERC-20 Transfer event signature
    private val transferEvent = Event(
        "Transfer",
        listOf(
            TypeReference.create(Address::class.java, true),  // from (indexed)
            TypeReference.create(Address::class.java, true),  // to (indexed)
            TypeReference.create(Uint256::class.java)          // value
        )
    )
    private val transferEventSignature = EventEncoder.encode(transferEvent)

    companion object {
        const val USDC_DECIMALS = 6
    }

    /**
     * 잔액 충전 트랜잭션 검증
     */
    fun verifyDeposit(memberId: Long, txHash: String, amount: BigDecimal, fromAddress: String? = null): PaymentVerifyResponse {
        var retries = 3
        while (retries > 0) {
            try {
                return verifyDepositInternal(memberId, txHash, amount, fromAddress)
            } catch (e: OptimisticLockException) {
                retries--
                if (retries == 0) {
                    log.error("낙관적 락 재시도 실패: memberId=$memberId", e)
                    throw IllegalArgumentException("Failed to update balance. Please try again.")
                }
                log.warn("낙관적 락 충돌 발생, 재시도 중... (남은 시도: $retries)")
                Thread.sleep(100)
            } catch (e: ObjectOptimisticLockingFailureException) {
                retries--
                if (retries == 0) {
                    log.error("낙관적 락 재시도 실패: memberId=$memberId", e)
                    throw IllegalArgumentException("Failed to update balance. Please try again.")
                }
                log.warn("낙관적 락 충돌 발생, 재시도 중... (남은 시도: $retries)")
                Thread.sleep(100)
            }
        }
        throw IllegalArgumentException("Deposit processing failed.")
    }

    @Transactional
    private fun verifyDepositInternal(memberId: Long, txHash: String, amount: BigDecimal, fromAddress: String? = null): PaymentVerifyResponse {
        // 1. 회원 조회
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }

        // 2~4. 도메인 정책 검증
        DepositPolicy.ensureFromAddressProvided(fromAddress)
        val senderAddress = fromAddress!!
        DepositPolicy.ensureRegisteredWallet(member.walletAddress)
        DepositPolicy.ensureWalletAddressMatches(member.walletAddress!!, senderAddress)

        // 5. 중복 검증 방지
        if (paymentTransactionRepository.existsByTxHash(txHash)) {
            throw IllegalArgumentException("Transaction already processed: $txHash")
        }

        DepositPolicy.ensureMinimumDeposit(amount)

        val expectedAmountRaw = DepositPolicy.expectedAmountRaw(amount)

        // 6. 온체인 트랜잭션 검증 (실제 from 주소도 검증)
        verifyOnChainTransfer(txHash, expectedAmountRaw, senderAddress)

        // 7. 결제 기록 저장
        val payment = PaymentTransaction(
            memberId = memberId,
            txHash = txHash,
            amount = amount,
            type = "DEPOSIT",
            status = "CONFIRMED"
        )
        paymentTransactionRepository.save(payment)

        // 8. 잔액 충전 (wallet ledger + member 잔액 동기화)
        val wallet = walletBalanceService.credit(
            memberId = memberId,
            amount = amount,
            txType = "DEPOSIT",
            referenceType = "PAYMENT_TX",
            referenceId = payment.id,
            description = "USDC 충전",
            treasuryInflow = true,
        )

        transactionHistoryService.record(
            memberId = memberId,
            type = "DEPOSIT",
            amount = amount,
            balanceAfter = wallet.availableBalance,
            description = "USDC 충전",
            txHash = txHash
        )

        log.info("충전 완료: memberId=$memberId, amount=\$$amount, txHash=$txHash")

        return PaymentVerifyResponse(
            success = true,
            txHash = txHash,
            amount = amount.toDouble(),
            newBalance = wallet.availableBalance.toDouble(),
            message = "\$${amount} has been deposited."
        )
    }

    /**
     * Polygon 체인에서 USDC Transfer 트랜잭션 검증 (발신 주소 포함)
     */
    fun verifyOnChainTransfer(txHash: String, expectedAmountRaw: BigInteger, expectedFromAddress: String): BigInteger {
        val receipt = web3j.ethGetTransactionReceipt(txHash).send()

        OnChainTransferPolicy.ensureReceiptPresent(receipt.transactionReceipt.isPresent, txHash)

        val txReceipt = receipt.transactionReceipt.get()

        OnChainTransferPolicy.ensureTransactionSucceeded(txReceipt.status, txHash)

        OnChainTransferPolicy.ensureTargetContract(txReceipt.to, usdcContract)

        // Transfer 이벤트 로그 파싱
        val transferLogs = txReceipt.logs.filter { logEntry ->
            logEntry.topics.isNotEmpty() && logEntry.topics[0] == transferEventSignature
        }

        val candidates = transferLogs.mapNotNull { logEntry ->
            if (logEntry.topics.size < 3) return@mapNotNull null
            val fromAddress = "0x" + logEntry.topics[1].removePrefix("0x").takeLast(40)
            val toAddress = "0x" + logEntry.topics[2].removePrefix("0x").takeLast(40)
            val transferValue = BigInteger(logEntry.data.removePrefix("0x"), 16)
            TransferEventCandidate(
                fromAddress = fromAddress,
                toAddress = toAddress,
                amountRaw = transferValue,
            )
        }

        return OnChainTransferPolicy.selectAndValidateTransfer(
            candidates = candidates,
            expectedReceiver = receiverWallet,
            expectedSender = expectedFromAddress,
            expectedAmountRaw = expectedAmountRaw,
        )
    }
}

// ===== DTOs =====
data class PaymentVerifyResponse(
    val success: Boolean,
    val txHash: String,
    val amount: Double,
    val newBalance: Double? = null,
    val message: String? = null
)
