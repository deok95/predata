package com.predata.backend.service

import com.predata.backend.domain.PaymentTransaction
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.PaymentTransactionRepository
import jakarta.annotation.PostConstruct
import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigDecimal
import java.math.BigInteger

@Service
class WithdrawalService(
    @Value("\${polygon.rpc-url}") private val rpcUrl: String,
    @Value("\${polygon.usdc-contract}") private val usdcContract: String,
    @Value("\${polygon.sender-private-key:}") private val senderPrivateKey: String,
    @Value("\${polygon.chain-id:80002}") private val chainId: Long,
    private val memberRepository: MemberRepository,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val transactionHistoryService: TransactionHistoryService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val web3j: Web3j by lazy { Web3j.build(HttpService(rpcUrl)) }

    companion object {
        val MIN_WITHDRAW = BigDecimal("1")    // 최소 $1
        val MAX_WITHDRAW = BigDecimal("100")  // 최대 $100
        const val USDC_DECIMALS = 6
    }

    /**
     * 서버 재시작 시 PENDING 상태 출금 트랜잭션 복구
     */
    @PostConstruct
    fun recoverPendingWithdrawals() {
        try {
            val pendingTxs = paymentTransactionRepository.findByStatusAndType("PENDING", "WITHDRAWAL")
            if (pendingTxs.isEmpty()) {
                log.info("복구할 PENDING 출금 트랜잭션이 없습니다.")
                return
            }

            log.info("PENDING 출금 트랜잭션 ${pendingTxs.size}건 복구 시작")
            pendingTxs.forEach { tx ->
                try {
                    confirmWithdrawalTransaction(tx)
                } catch (e: Exception) {
                    log.error("PENDING 출금 트랜잭션 복구 실패: txHash=${tx.txHash}", e)
                }
            }
            log.info("PENDING 출금 트랜잭션 복구 완료")
        } catch (e: Exception) {
            log.error("PENDING 출금 트랜잭션 복구 중 오류 발생", e)
        }
    }

    fun withdraw(memberId: Long, amount: BigDecimal, walletAddress: String): WithdrawResponse {
        // Phase 1: 잔액 차감 + USDC 전송 + PENDING 저장 (낙관적 락 재시도 포함)
        val pendingTx = withdrawWithRetry(memberId, amount, walletAddress)

        // Phase 2: receipt 확인 및 PENDING → CONFIRMED/FAILED 전환
        return confirmWithdrawalTransaction(pendingTx)
    }

    private fun withdrawWithRetry(memberId: Long, amount: BigDecimal, walletAddress: String): PaymentTransaction {
        var retries = 3
        while (retries > 0) {
            try {
                return withdrawInternal(memberId, amount, walletAddress)
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
        throw IllegalArgumentException("Withdrawal processing failed.")
    }

    /**
     * Phase 1: 잔액 차감 + USDC 전송 + PENDING 저장 (트랜잭션 내)
     */
    @Transactional
    private fun withdrawInternal(memberId: Long, amount: BigDecimal, walletAddress: String): PaymentTransaction {
        // 1. 회원 조회 + 밴 체크
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }

        if (member.isBanned) {
            throw IllegalArgumentException("Account has been suspended.")
        }

        // 2. 금액 검증
        if (amount < MIN_WITHDRAW || amount > MAX_WITHDRAW) {
            throw IllegalArgumentException("Withdrawal amount must be between \$${MIN_WITHDRAW} and \$${MAX_WITHDRAW}.")
        }

        // 3. 지갑 주소 검증
        if (!walletAddress.matches(Regex("^0x[a-fA-F0-9]{40}$"))) {
            throw IllegalArgumentException("Invalid wallet address.")
        }

        // 4. DB에 등록된 지갑 주소와 일치하는지 확인
        if (member.walletAddress != null) {
            if (!walletAddress.equals(member.walletAddress, ignoreCase = true)) {
                throw IllegalArgumentException("Wallet address does not match registered address. Please connect your wallet in My Page.")
            }
        } else {
            throw IllegalArgumentException("No wallet address registered. Please connect your wallet in My Page first.")
        }

        // 5. 잔액 확인
        if (member.usdcBalance < amount) {
            throw IllegalArgumentException("Insufficient balance. (balance: \$${member.usdcBalance}, requested: \$${amount})")
        }

        // 6. 프라이빗 키 확인
        if (senderPrivateKey.isBlank()) {
            throw IllegalStateException("Withdrawal feature not configured. Please contact administrator.")
        }

        // 7. 잔액 차감
        member.usdcBalance = member.usdcBalance.subtract(amount)
        memberRepository.save(member)

        // 8. USDC 전송
        val txHash: String
        try {
            txHash = sendUSDC(walletAddress, amount)
            log.info("USDC 전송 성공: memberId=$memberId, txHash=$txHash, amount=\$$amount")
        } catch (e: Exception) {
            // 전송 실패 시 잔액 복구
            member.usdcBalance = member.usdcBalance.add(amount)
            memberRepository.save(member)
            log.error("USDC 전송 실패, 잔액 복구: memberId=$memberId, amount=$amount", e)
            throw IllegalArgumentException("USDC transfer failed: ${e.message}")
        }

        // 9. PENDING 상태로 결제 기록 저장
        val payment = PaymentTransaction(
            memberId = memberId,
            txHash = txHash,
            amount = amount,
            type = "WITHDRAWAL",
            status = "PENDING"
        )
        paymentTransactionRepository.save(payment)
        log.info("출금 트랜잭션 PENDING 저장: memberId=$memberId, txHash=$txHash")

        return payment
    }

    /**
     * Phase 2: receipt 확인 및 PENDING → CONFIRMED/FAILED 전환
     */
    private fun confirmWithdrawalTransaction(pendingTx: PaymentTransaction): WithdrawResponse {
        val txHash = pendingTx.txHash
        val memberId = pendingTx.memberId
        val amount = pendingTx.amount

        log.info("출금 트랜잭션 receipt 확인 시작: txHash=$txHash")

        // receipt 확인 (최대 30초 대기)
        val txStatus = waitForTransactionReceipt(txHash)

        // 상태 전환
        return if (txStatus) {
            updateTransactionStatus(pendingTx, "CONFIRMED", memberId, amount)
        } else {
            updateTransactionStatus(pendingTx, "FAILED", memberId, amount)
        }
    }

    /**
     * 트랜잭션 상태 업데이트 및 응답 생성
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private fun updateTransactionStatus(
        transaction: PaymentTransaction,
        newStatus: String,
        memberId: Long,
        amount: BigDecimal
    ): WithdrawResponse {
        val txHash = transaction.txHash

        if (newStatus == "CONFIRMED") {
            // CONFIRMED: 거래 내역 기록
            transaction.status = "CONFIRMED"
            paymentTransactionRepository.save(transaction)

            val member = memberRepository.findById(memberId).orElseThrow()
            transactionHistoryService.record(
                memberId = memberId,
                type = "WITHDRAW",
                amount = amount.negate(),
                balanceAfter = member.usdcBalance,
                description = "USDC 출금",
                txHash = txHash
            )

            log.info("출금 완료: memberId=$memberId, amount=\$$amount, txHash=$txHash")

            return WithdrawResponse(
                success = true,
                txHash = txHash,
                amount = amount.toDouble(),
                newBalance = member.usdcBalance.toDouble(),
                message = "Withdrawal of \$${amount} completed."
            )
        } else {
            // FAILED: 잔액 복구
            transaction.status = "FAILED"
            paymentTransactionRepository.save(transaction)

            val member = memberRepository.findById(memberId).orElseThrow()
            member.usdcBalance = member.usdcBalance.add(amount)
            memberRepository.save(member)

            log.error("출금 트랜잭션 실패, 잔액 복구: memberId=$memberId, amount=$amount, txHash=$txHash")

            throw IllegalArgumentException("Blockchain transaction failed. Balance has been restored.")
        }
    }

    /**
     * 트랜잭션 receipt 확인 및 성공 여부 반환 (최대 30초 대기)
     */
    private fun waitForTransactionReceipt(txHash: String): Boolean {
        return try {
            var attempts = 30
            while (attempts > 0) {
                val receipt = web3j.ethGetTransactionReceipt(txHash).send()
                if (receipt.transactionReceipt.isPresent) {
                    val txReceipt = receipt.transactionReceipt.get()
                    val success = txReceipt.status == "0x1"
                    log.info("트랜잭션 receipt 확인: txHash=$txHash, status=${txReceipt.status}, success=$success")
                    return success
                }
                Thread.sleep(1000)
                attempts--
            }
            log.warn("트랜잭션 receipt 확인 시간 초과: txHash=$txHash")
            false
        } catch (e: Exception) {
            log.error("트랜잭션 receipt 확인 실패: txHash=$txHash", e)
            false
        }
    }

    private fun sendUSDC(toAddress: String, amount: BigDecimal): String {
        val credentials = Credentials.create(senderPrivateKey)
        val transactionManager = RawTransactionManager(web3j, credentials, chainId)

        val amountRaw = amount.multiply(BigDecimal.TEN.pow(USDC_DECIMALS)).toBigInteger()

        // ERC-20 transfer function
        val function = Function(
            "transfer",
            listOf(Address(toAddress), Uint256(amountRaw)),
            emptyList()
        )
        val encodedFunction = FunctionEncoder.encode(function)

        val gasPrice = web3j.ethGasPrice().send().gasPrice
        val gasLimit = BigInteger.valueOf(100000)

        val transaction = transactionManager.sendTransaction(
            gasPrice,
            gasLimit,
            usdcContract,
            encodedFunction,
            BigInteger.ZERO
        )

        if (transaction.hasError()) {
            throw RuntimeException("Transaction submission failed: ${transaction.error.message}")
        }

        return transaction.transactionHash
    }
}

data class WithdrawResponse(
    val success: Boolean,
    val txHash: String? = null,
    val amount: Double? = null,
    val newBalance: Double? = null,
    val message: String
)
