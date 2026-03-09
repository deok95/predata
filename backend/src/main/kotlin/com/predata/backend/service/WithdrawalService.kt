package com.predata.backend.service

import com.predata.backend.config.properties.PolygonProperties
import com.predata.backend.config.properties.FinanceProperties
import com.predata.backend.domain.PaymentTransaction
import com.predata.backend.domain.policy.WithdrawalPolicy
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.PaymentTransactionRepository
import jakarta.annotation.PostConstruct
import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigDecimal
import java.math.BigInteger

@Service
class WithdrawalService(
    private val polygonProperties: PolygonProperties,
    private val financeProperties: FinanceProperties,
    private val memberRepository: MemberRepository,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val transactionHistoryService: TransactionHistoryService,
    private val walletBalanceService: WalletBalanceService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val web3j: Web3j by lazy { Web3j.build(HttpService(polygonProperties.rpcUrl)) }

    companion object {
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
        val minWithdraw = financeProperties.withdrawal.minUsdc
        val maxWithdraw = financeProperties.withdrawal.maxUsdc
        val withdrawalFee = financeProperties.withdrawal.feeUsdc
        WithdrawalPolicy.validateAmount(amount, minWithdraw, maxWithdraw)
        WithdrawalPolicy.validateFee(withdrawalFee)

        // 3. 지갑 주소 검증
        WithdrawalPolicy.validateWalletAddressFormat(walletAddress)

        // 4. DB에 등록된 지갑 주소와 일치하는지 확인
        WithdrawalPolicy.validateRegisteredWallet(member.walletAddress, walletAddress)

        // 5. 잔액 확인
        val totalDebit = WithdrawalPolicy.totalDebit(amount, withdrawalFee)
        val availableBalance = walletBalanceService.getAvailableBalance(memberId)
        if (availableBalance < totalDebit) {
            throw IllegalArgumentException("Insufficient balance. (balance: \$$availableBalance, required: \$${totalDebit})")
        }

        // 6. 프라이빗 키 확인
        if (polygonProperties.senderPrivateKey.isBlank()) {
            throw IllegalStateException("Withdrawal feature not configured. Please contact administrator.")
        }

        // 7. 잔액 차감 (wallet ledger + member 잔액 동기화)
        walletBalanceService.lockForWithdrawal(
            memberId = memberId,
            amount = totalDebit,
            txType = "WITHDRAW_LOCK",
            referenceType = "WITHDRAWAL",
            referenceId = null,
            description = "USDC 출금 잠금 (amount=${amount}, fee=${withdrawalFee})",
        )

        // 8. USDC 전송
        val txHash: String
        try {
            txHash = sendUSDC(walletAddress, amount)
            log.info("USDC 전송 성공: memberId=$memberId, txHash=$txHash, amount=\$$amount")
        } catch (e: Exception) {
            // 전송 실패 시 잔액 복구
            walletBalanceService.unlockWithdrawal(
                memberId = memberId,
                amount = totalDebit,
                txType = "WITHDRAW_UNLOCK",
                referenceType = "WITHDRAWAL",
                referenceId = null,
                description = "출금 실패 잠금해제",
            )
            log.error("USDC 전송 실패, 잠금해제: memberId=$memberId, amount=$amount, fee=$withdrawalFee", e)
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
        val withdrawalFee = financeProperties.withdrawal.feeUsdc
        val totalDebit = WithdrawalPolicy.totalDebit(amount, withdrawalFee)

        if (newStatus == "CONFIRMED") {
            // CONFIRMED: 거래 내역 기록
            transaction.status = "CONFIRMED"
            paymentTransactionRepository.save(transaction)

            // 잠금 정산: 전송금액 + 수수료 모두 locked에서 차감
            val settled = walletBalanceService.settleLockedWithdrawal(
                memberId = memberId,
                amount = totalDebit,
                txType = "WITHDRAW_SETTLE",
                referenceType = "PAYMENT_TX",
                referenceId = transaction.id,
                description = "출금 확정 정산",
            )

            // 실제 온체인 출금분은 트레저리 아웃플로우
            walletBalanceService.recordTreasuryOutflow(
                amount = amount,
                txType = "WITHDRAW",
                referenceType = "PAYMENT_TX",
                referenceId = transaction.id,
                description = "USDC 출금 확정",
            )
            // 출금 수수료는 트레저리 인플로우
            if (withdrawalFee > BigDecimal.ZERO) {
                walletBalanceService.recordTreasuryInflow(
                    amount = withdrawalFee,
                    txType = "WITHDRAW_FEE",
                    referenceType = "PAYMENT_TX",
                    referenceId = transaction.id,
                    description = "출금 수수료 수익",
                )
            }
            transactionHistoryService.record(
                memberId = memberId,
                type = "WITHDRAW",
                amount = totalDebit.negate(),
                balanceAfter = settled.availableBalance,
                description = "USDC 출금 (수수료 ${withdrawalFee} 포함)",
                txHash = txHash
            )

            log.info("출금 완료: memberId=$memberId, amount=\$$amount, txHash=$txHash")

            return WithdrawResponse(
                success = true,
                txHash = txHash,
                amount = amount.toDouble(),
                fee = withdrawalFee.toDouble(),
                totalDebited = totalDebit.toDouble(),
                newBalance = settled.availableBalance.toDouble(),
                message = "Withdrawal of \$${amount} completed."
            )
        } else {
            // FAILED: 잠금 해제
            transaction.status = "FAILED"
            paymentTransactionRepository.save(transaction)

            val snapshot = walletBalanceService.unlockWithdrawal(
                memberId = memberId,
                amount = totalDebit,
                txType = "WITHDRAW_UNLOCK",
                referenceType = "PAYMENT_TX",
                referenceId = transaction.id,
                description = "출금 트랜잭션 실패 잠금 해제",
            )

            log.error("출금 트랜잭션 실패, 잠금 해제: memberId=$memberId, amount=$amount, fee=$withdrawalFee, txHash=$txHash")

            throw IllegalArgumentException("Blockchain transaction failed. Balance has been restored. current=\$${snapshot.availableBalance}")
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
        val credentials = Credentials.create(polygonProperties.senderPrivateKey)
        val transactionManager = RawTransactionManager(web3j, credentials, polygonProperties.chainId)

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
            polygonProperties.usdcContract,
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
    val fee: Double? = null,
    val totalDebited: Double? = null,
    val newBalance: Double? = null,
    val message: String
)
