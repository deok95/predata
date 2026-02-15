package com.predata.backend.service

import com.predata.backend.domain.PaymentTransaction
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.PaymentTransactionRepository
import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
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

    fun withdraw(memberId: Long, amount: BigDecimal, walletAddress: String): WithdrawResponse {
        var retries = 3
        while (retries > 0) {
            try {
                return withdrawInternal(memberId, amount, walletAddress)
            } catch (e: OptimisticLockException) {
                retries--
                if (retries == 0) {
                    log.error("낙관적 락 재시도 실패: memberId=$memberId", e)
                    throw IllegalArgumentException("잔액 업데이트에 실패했습니다. 다시 시도해주세요.")
                }
                log.warn("낙관적 락 충돌 발생, 재시도 중... (남은 시도: $retries)")
                Thread.sleep(100)
            } catch (e: ObjectOptimisticLockingFailureException) {
                retries--
                if (retries == 0) {
                    log.error("낙관적 락 재시도 실패: memberId=$memberId", e)
                    throw IllegalArgumentException("잔액 업데이트에 실패했습니다. 다시 시도해주세요.")
                }
                log.warn("낙관적 락 충돌 발생, 재시도 중... (남은 시도: $retries)")
                Thread.sleep(100)
            }
        }
        throw IllegalArgumentException("출금 처리에 실패했습니다.")
    }

    @Transactional
    private fun withdrawInternal(memberId: Long, amount: BigDecimal, walletAddress: String): WithdrawResponse {
        // 1. 회원 조회 + 밴 체크
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }

        if (member.isBanned) {
            throw IllegalArgumentException("계정이 정지되었습니다.")
        }

        // 2. 금액 검증
        if (amount < MIN_WITHDRAW || amount > MAX_WITHDRAW) {
            throw IllegalArgumentException("출금 가능 금액은 \$${MIN_WITHDRAW}~\$${MAX_WITHDRAW}입니다.")
        }

        // 3. 지갑 주소 검증
        if (!walletAddress.matches(Regex("^0x[a-fA-F0-9]{40}$"))) {
            throw IllegalArgumentException("유효하지 않은 지갑 주소입니다.")
        }

        // 4. DB에 등록된 지갑 주소와 일치하는지 확인
        if (member.walletAddress != null) {
            if (!walletAddress.equals(member.walletAddress, ignoreCase = true)) {
                throw IllegalArgumentException("등록된 지갑 주소와 일치하지 않습니다. 마이페이지에서 지갑을 연결해주세요.")
            }
        } else {
            throw IllegalArgumentException("지갑 주소가 등록되지 않았습니다. 마이페이지에서 지갑을 먼저 연결해주세요.")
        }

        // 5. 잔액 확인
        if (member.usdcBalance < amount) {
            throw IllegalArgumentException("잔액이 부족합니다. (보유: \$${member.usdcBalance}, 요청: \$${amount})")
        }

        // 6. 프라이빗 키 확인
        if (senderPrivateKey.isBlank()) {
            throw IllegalStateException("출금 기능이 설정되지 않았습니다. 관리자에게 문의하세요.")
        }

        // 7. 잔액 차감 (먼저 차감)
        member.usdcBalance = member.usdcBalance.subtract(amount)
        memberRepository.save(member)

        // 8. USDC 전송
        val txHash: String
        try {
            txHash = sendUSDC(walletAddress, amount)
        } catch (e: Exception) {
            // 전송 실패 시 잔액 복구
            member.usdcBalance = member.usdcBalance.add(amount)
            memberRepository.save(member)
            log.error("USDC 전송 실패, 잔액 복구: memberId=$memberId, amount=$amount", e)
            throw IllegalArgumentException("USDC 전송에 실패했습니다: ${e.message}")
        }

        // 9. 트랜잭션 receipt 확인 및 상태 결정
        val txStatus = verifyTransactionReceipt(txHash)
        val paymentStatus = if (txStatus) "CONFIRMED" else "FAILED"

        // 10. 트랜잭션 실패 시 잔액 복구
        if (!txStatus) {
            member.usdcBalance = member.usdcBalance.add(amount)
            memberRepository.save(member)
            log.error("USDC 전송 트랜잭션 실패, 잔액 복구: memberId=$memberId, amount=$amount, txHash=$txHash")
        }

        // 11. 결제 기록 저장
        val payment = PaymentTransaction(
            memberId = memberId,
            txHash = txHash,
            amount = amount,
            type = "WITHDRAWAL",
            status = paymentStatus
        )
        paymentTransactionRepository.save(payment)

        // 12. 성공 시에만 거래 내역 기록
        if (txStatus) {
            transactionHistoryService.record(
                memberId = memberId,
                type = "WITHDRAW",
                amount = amount.negate(),
                balanceAfter = member.usdcBalance,
                description = "USDC 출금",
                txHash = txHash
            )
            log.info("출금 완료: memberId=$memberId, amount=\$$amount, wallet=$walletAddress, txHash=$txHash")
        } else {
            log.error("출금 트랜잭션 실패: memberId=$memberId, amount=\$$amount, wallet=$walletAddress, txHash=$txHash")
            throw IllegalArgumentException("블록체인 트랜잭션이 실패했습니다. 잔액이 복구되었습니다.")
        }

        return WithdrawResponse(
            success = true,
            txHash = txHash,
            amount = amount.toDouble(),
            newBalance = member.usdcBalance.toDouble(),
            message = "\$${amount} 출금이 완료되었습니다."
        )
    }

    /**
     * 트랜잭션 receipt 확인 및 성공 여부 반환
     */
    private fun verifyTransactionReceipt(txHash: String): Boolean {
        return try {
            // 트랜잭션이 블록에 포함될 때까지 대기 (최대 30초)
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
            throw RuntimeException("트랜잭션 전송 실패: ${transaction.error.message}")
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
