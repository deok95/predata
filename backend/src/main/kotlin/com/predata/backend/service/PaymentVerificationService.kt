package com.predata.backend.service

import com.predata.backend.domain.PaymentTransaction
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.PaymentTransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
    private val transactionHistoryService: TransactionHistoryService
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
    @Transactional
    fun verifyDeposit(memberId: Long, txHash: String, amount: BigDecimal, fromAddress: String? = null): PaymentVerifyResponse {
        // 1. 회원 조회
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }

        // 2. 지갑 주소 검증 (DB에 등록된 지갑 주소와 일치해야 함)
        if (fromAddress != null && member.walletAddress != null) {
            if (!fromAddress.equals(member.walletAddress, ignoreCase = true)) {
                throw IllegalArgumentException("등록된 지갑 주소와 일치하지 않습니다. 마이페이지에서 지갑을 연결해주세요.")
            }
        }

        // 3. 중복 검증 방지
        if (paymentTransactionRepository.existsByTxHash(txHash)) {
            throw IllegalArgumentException("이미 처리된 트랜잭션입니다: $txHash")
        }

        if (amount < BigDecimal.ONE || amount > BigDecimal(100)) {
            throw IllegalArgumentException("충전 가능 금액은 \$1~\$100입니다.")
        }

        val expectedAmountRaw = amount.multiply(BigDecimal.TEN.pow(USDC_DECIMALS)).toBigInteger()

        // 4. 온체인 트랜잭션 검증
        val verifiedAmount = verifyOnChainTransfer(txHash, expectedAmountRaw)

        // 5. 결제 기록 저장
        val payment = PaymentTransaction(
            memberId = memberId,
            txHash = txHash,
            amount = amount,
            type = "DEPOSIT",
            status = "CONFIRMED"
        )
        paymentTransactionRepository.save(payment)

        // 6. 잔액 충전
        member.usdcBalance = member.usdcBalance.add(amount)
        memberRepository.save(member)

        transactionHistoryService.record(
            memberId = memberId,
            type = "DEPOSIT",
            amount = amount,
            balanceAfter = member.usdcBalance,
            description = "USDC 충전",
            txHash = txHash
        )

        log.info("충전 완료: memberId=$memberId, amount=\$$amount, txHash=$txHash")

        return PaymentVerifyResponse(
            success = true,
            txHash = txHash,
            amount = amount.toDouble(),
            newBalance = member.usdcBalance.toDouble(),
            message = "\$${amount}가 충전되었습니다."
        )
    }

    /**
     * Polygon 체인에서 USDC Transfer 트랜잭션 검증
     */
    fun verifyOnChainTransfer(txHash: String, expectedAmountRaw: BigInteger): BigInteger {
        val receipt = web3j.ethGetTransactionReceipt(txHash).send()

        if (receipt.transactionReceipt.isEmpty) {
            throw IllegalArgumentException("트랜잭션을 찾을 수 없습니다. 아직 확인되지 않았을 수 있습니다: $txHash")
        }

        val txReceipt = receipt.transactionReceipt.get()

        // 트랜잭션 성공 여부 확인
        if (txReceipt.status != "0x1") {
            throw IllegalArgumentException("트랜잭션이 실패했습니다: $txHash")
        }

        // USDC 컨트랙트로의 트랜잭션인지 확인
        if (!txReceipt.to.equals(usdcContract, ignoreCase = true)) {
            throw IllegalArgumentException("USDC 컨트랙트로의 트랜잭션이 아닙니다.")
        }

        // Transfer 이벤트 로그 파싱
        val transferLogs = txReceipt.logs.filter { logEntry ->
            logEntry.topics.isNotEmpty() && logEntry.topics[0] == transferEventSignature
        }

        if (transferLogs.isEmpty()) {
            throw IllegalArgumentException("Transfer 이벤트를 찾을 수 없습니다.")
        }

        // 수신자와 금액 검증
        for (logEntry in transferLogs) {
            if (logEntry.topics.size < 3) continue

            val toAddress = "0x" + logEntry.topics[2].removePrefix("0x").takeLast(40)
            val transferValue = BigInteger(logEntry.data.removePrefix("0x"), 16)

            if (toAddress.equals(receiverWallet, ignoreCase = true)) {
                if (transferValue < expectedAmountRaw) {
                    throw IllegalArgumentException(
                        "전송 금액이 부족합니다. (필요: $expectedAmountRaw, 실제: $transferValue)"
                    )
                }
                return transferValue
            }
        }

        throw IllegalArgumentException("수신 지갑으로의 USDC 전송을 찾을 수 없습니다.")
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
