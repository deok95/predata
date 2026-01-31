package com.predata.backend.service

import com.predata.backend.domain.Question
import com.predata.backend.domain.FinalResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture

/**
 * 블록체인 통합 서비스
 * Base L2에 배포된 PredataMarket 컨트랙트와 통신
 */
@Service
class BlockchainService(
    @Value("\${blockchain.enabled:false}") private val enabled: Boolean,
    @Value("\${blockchain.rpc.url:https://sepolia.base.org}") private val rpcUrl: String,
    @Value("\${blockchain.contract.address:}") private val contractAddress: String,
    @Value("\${blockchain.admin.private-key:}") private val adminPrivateKey: String
) {
    private val logger = LoggerFactory.getLogger(BlockchainService::class.java)
    private val web3j: Web3j?
    private val credentials: Credentials?
    private val gasProvider = DefaultGasProvider()

    init {
        if (enabled && adminPrivateKey.isNotBlank()) {
            try {
                web3j = Web3j.build(HttpService(rpcUrl))
                credentials = Credentials.create(adminPrivateKey)
                logger.info("✅ 블록체인 서비스 활성화됨")
                logger.info("📍 네트워크: $rpcUrl")
                logger.info("📝 컨트랙트: $contractAddress")
                logger.info("👤 Admin: ${credentials.address}")
            } catch (e: Exception) {
                logger.error("❌ 블록체인 초기화 실패: ${e.message}")
                web3j = null
                credentials = null
            }
        } else {
            web3j = null
            credentials = null
            logger.warn("⚠️ 블록체인 서비스 비활성화됨")
        }
    }

    /**
     * 질문 생성을 온체인에 기록
     */
    @Async
    fun createQuestionOnChain(question: Question): CompletableFuture<String?> {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(null)
        }

        return CompletableFuture.supplyAsync {
            try {
                logger.info("🔗 온체인 질문 생성 시작: Question #${question.id}")

                val function = Function(
                    "createQuestion",
                    listOf(
                        Uint256(BigInteger.valueOf(question.id!!)),
                        Utf8String(question.title),
                        Utf8String(question.category ?: "GENERAL"),
                        Uint256(BigInteger.valueOf(question.expiredAt.toEpochSecond(ZoneOffset.UTC)))
                    ),
                    emptyList()
                )

                val txHash = sendTransaction(function)
                logger.info("✅ 온체인 질문 생성 완료: $txHash")
                txHash
            } catch (e: Exception) {
                logger.error("❌ 온체인 질문 생성 실패: ${e.message}", e)
                null
            }
        }
    }

    /**
     * 배치로 베팅을 온체인에 기록
     */
    @Async
    fun batchPlaceBetsOnChain(
        bets: List<BetOnChainData>
    ): CompletableFuture<String?> {
        if (!isEnabled() || bets.isEmpty()) {
            return CompletableFuture.completedFuture(null)
        }

        return CompletableFuture.supplyAsync {
            try {
                logger.info("🔗 배치 베팅 온체인 기록 시작: ${bets.size}개")

                val questionIds = bets.map { Uint256(BigInteger.valueOf(it.questionId)) }
                val users = bets.map { Address(it.userAddress) }
                val choices = bets.map { Bool(it.choice) }
                val amounts = bets.map { Uint256(BigInteger.valueOf(it.amount)) }

                val function = Function(
                    "batchPlaceBets",
                    listOf(
                        DynamicArray(Uint256::class.java, questionIds),
                        DynamicArray(Address::class.java, users),
                        DynamicArray(Bool::class.java, choices),
                        DynamicArray(Uint256::class.java, amounts)
                    ),
                    emptyList()
                )

                val txHash = sendTransaction(function)
                logger.info("✅ 배치 베팅 온체인 기록 완료: $txHash (${bets.size}개)")
                txHash
            } catch (e: Exception) {
                logger.error("❌ 배치 베팅 온체인 기록 실패: ${e.message}", e)
                null
            }
        }
    }

    /**
     * 정산 결과를 온체인에 기록
     */
    @Async
    fun settleQuestionOnChain(
        questionId: Long,
        finalResult: FinalResult
    ): CompletableFuture<String?> {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(null)
        }

        return CompletableFuture.supplyAsync {
            try {
                logger.info("🔗 온체인 정산 시작: Question #$questionId -> $finalResult")

                val resultValue = when (finalResult) {
                    FinalResult.YES -> BigInteger.ONE
                    FinalResult.NO -> BigInteger.TWO
                    else -> throw IllegalArgumentException("Invalid result: $finalResult")
                }

                val function = Function(
                    "settleQuestion",
                    listOf(
                        Uint256(BigInteger.valueOf(questionId)),
                        Uint8(resultValue)
                    ),
                    emptyList()
                )

                val txHash = sendTransaction(function)
                logger.info("✅ 온체인 정산 완료: $txHash")
                txHash
            } catch (e: Exception) {
                logger.error("❌ 온체인 정산 실패: ${e.message}", e)
                null
            }
        }
    }

    /**
     * 온체인 데이터 조회 (검증용)
     */
    fun getQuestionFromChain(questionId: Long): QuestionOnChain? {
        if (!isEnabled()) return null

        return try {
            val function = Function(
                "getQuestion",
                listOf(Uint256(BigInteger.valueOf(questionId))),
                listOf(
                    object : TypeReference<Uint256>() {},
                    object : TypeReference<Utf8String>() {},
                    object : TypeReference<Utf8String>() {},
                    object : TypeReference<Uint256>() {},
                    object : TypeReference<Uint256>() {},
                    object : TypeReference<Uint256>() {},
                    object : TypeReference<Uint8>() {},
                    object : TypeReference<Bool>() {},
                    object : TypeReference<Uint256>() {},
                    object : TypeReference<Uint256>() {},
                    object : TypeReference<Uint256>() {}
                )
            )

            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j!!.ethCall(
                Transaction.createEthCallTransaction(
                    credentials!!.address,
                    contractAddress,
                    encodedFunction
                ),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError()) {
                logger.error("온체인 조회 실패: ${response.error.message}")
                return null
            }

            // 응답 파싱
            QuestionOnChain(
                questionId = questionId,
                totalBetPool = 0, // 실제 파싱 필요
                yesBetPool = 0,
                noBetPool = 0,
                settled = false
            )
        } catch (e: Exception) {
            logger.error("온체인 조회 실패: ${e.message}", e)
            null
        }
    }

    /**
     * 트랜잭션 전송
     */
    private fun sendTransaction(function: Function): String {
        val encodedFunction = FunctionEncoder.encode(function)
        
        val nonce = web3j!!.ethGetTransactionCount(
            credentials!!.address,
            DefaultBlockParameterName.PENDING
        ).send().transactionCount

        val transaction = Transaction.createFunctionCallTransaction(
            credentials.address,
            nonce,
            gasProvider.gasPrice,
            gasProvider.gasLimit,
            contractAddress,
            encodedFunction
        )

        val response: EthSendTransaction = web3j.ethSendTransaction(transaction).send()

        if (response.hasError()) {
            throw RuntimeException("트랜잭션 실패: ${response.error.message}")
        }

        return response.transactionHash
    }

    private fun isEnabled(): Boolean {
        return enabled && web3j != null && credentials != null
    }
}

/**
 * 온체인 베팅 데이터
 */
data class BetOnChainData(
    val questionId: Long,
    val userAddress: String,
    val choice: Boolean, // true = YES, false = NO
    val amount: Long
)

/**
 * 온체인 질문 데이터
 */
data class QuestionOnChain(
    val questionId: Long,
    val totalBetPool: Long,
    val yesBetPool: Long,
    val noBetPool: Long,
    val settled: Boolean
)
