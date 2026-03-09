package com.predata.backend.service.wallet

import com.predata.backend.config.properties.FinanceProperties
import com.predata.backend.config.properties.PolygonProperties
import com.predata.backend.domain.PaymentTransaction
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.PaymentTransactionRepository
import com.predata.backend.service.TransactionHistoryService
import com.predata.backend.service.WalletBalanceService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.http.HttpService
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

@Service
@ConditionalOnProperty(prefix = "app.finance.deposit-indexer", name = ["enabled"], havingValue = "true")
class DepositIndexerScheduler(
    private val polygonProperties: PolygonProperties,
    private val financeProperties: FinanceProperties,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val memberRepository: MemberRepository,
    private val walletBalanceService: WalletBalanceService,
    private val transactionHistoryService: TransactionHistoryService,
) {
    private val logger = LoggerFactory.getLogger(DepositIndexerScheduler::class.java)
    private val web3j: Web3j by lazy { Web3j.build(HttpService(polygonProperties.rpcUrl)) }

    private val transferEvent = Event(
        "Transfer",
        listOf(
            TypeReference.create(Address::class.java, true),
            TypeReference.create(Address::class.java, true),
            TypeReference.create(Uint256::class.java),
        )
    )
    private val transferTopic = EventEncoder.encode(transferEvent)

    @Scheduled(fixedDelayString = "\${app.finance.deposit-indexer.poll-interval-ms:30000}")
    fun pollDeposits() {
        if (polygonProperties.usdcContract.isBlank() || polygonProperties.receiverWallet.isBlank()) {
            logger.debug("[DepositIndexer] polygon usdc/receiver not configured, skipping.")
            return
        }

        try {
            scanAndIngest()
        } catch (e: Exception) {
            logger.error("[DepositIndexer] scan failed: {}", e.message, e)
        }
    }

    private fun scanAndIngest() {
        val cfg = financeProperties.depositIndexer
        val latest = web3j.ethBlockNumber().send().blockNumber
        if (latest <= BigInteger.ZERO) return

        val confirmations = BigInteger.valueOf(cfg.confirmations)
        if (latest <= confirmations) return
        val toBlock = latest.subtract(confirmations)
        val fromBlock = toBlock.subtract(BigInteger.valueOf(cfg.scanWindowBlocks)).max(BigInteger.ZERO)

        val receiverTopic = toTopicAddress(polygonProperties.receiverWallet)

        val filter = EthFilter(
            DefaultBlockParameter.valueOf(fromBlock),
            DefaultBlockParameter.valueOf(toBlock),
            polygonProperties.usdcContract
        ).addSingleTopic(transferTopic)
            .addOptionalTopics(null, receiverTopic)

        val logs = web3j.ethGetLogs(filter).send().logs
        if (logs.isEmpty()) {
            logger.debug("[DepositIndexer] no logs in blocks {}..{}", fromBlock, toBlock)
            return
        }

        var ingested = 0
        logs.forEach { raw ->
            val logObj = raw.get() as org.web3j.protocol.core.methods.response.Log
            val txHash = logObj.transactionHash ?: return@forEach
            val topics = logObj.topics ?: return@forEach
            if (topics.size < 3) return@forEach

            val fromAddress = "0x" + topics[1].removePrefix("0x").takeLast(40)
            val amountRaw = BigInteger(logObj.data.removePrefix("0x"), 16)
            val amount = amountRaw
                .toBigDecimal()
                .divide(BigDecimal("1000000"), 6, RoundingMode.DOWN)

            if (amount < cfg.minDepositUsdc) return@forEach
            if (paymentTransactionRepository.existsByTxHash(txHash)) return@forEach

            val member = memberRepository.findByWalletAddressIgnoreCase(fromAddress).orElse(null)
                ?: return@forEach

            if (ingestSingleDeposit(member.id!!, txHash, amount, fromAddress)) {
                ingested++
            }
        }

        if (ingested > 0) {
            logger.info("[DepositIndexer] ingested {} deposits (blocks {}..{})", ingested, fromBlock, toBlock)
        }
    }

    @Transactional
    fun ingestSingleDeposit(memberId: Long, txHash: String, amount: BigDecimal, fromAddress: String): Boolean {
        if (paymentTransactionRepository.existsByTxHash(txHash)) return false

        return try {
            val payment = paymentTransactionRepository.save(
                PaymentTransaction(
                    memberId = memberId,
                    txHash = txHash,
                    amount = amount,
                    type = "DEPOSIT_INDEXED",
                    status = "CONFIRMED",
                )
            )

            val wallet = walletBalanceService.credit(
                memberId = memberId,
                amount = amount,
                txType = "DEPOSIT_INDEXED",
                referenceType = "PAYMENT_TX",
                referenceId = payment.id,
                description = "자동 인덱서 입금 ($fromAddress)",
                treasuryInflow = true,
            )

            transactionHistoryService.record(
                memberId = memberId,
                type = "DEPOSIT",
                amount = amount,
                balanceAfter = wallet.availableBalance,
                description = "USDC 자동 입금 반영",
                txHash = txHash,
            )
            true
        } catch (e: DataIntegrityViolationException) {
            false
        }
    }

    private fun toTopicAddress(address: String): String {
        val normalized = address.removePrefix("0x").lowercase()
        return "0x" + normalized.padStart(64, '0')
    }
}

