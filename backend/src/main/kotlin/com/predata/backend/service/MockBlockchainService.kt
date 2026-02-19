package com.predata.backend.service

import com.predata.backend.domain.Question
import com.predata.backend.domain.FinalResult
import com.predata.backend.dto.BetOnChainData
import com.predata.backend.dto.QuestionOnChain
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Mock 블록체인 서비스
 * 로컬 개발 환경에서 실제 블록체인처럼 동작하는 시뮬레이션
 */
@Service
class MockBlockchainService {
    private val logger = LoggerFactory.getLogger(MockBlockchainService::class.java)
    
    // 가짜 블록체인 저장소 (메모리)
    private val mockChain = ConcurrentHashMap<Long, MockQuestion>()
    private val mockTransactions = ConcurrentHashMap<String, MockTransaction>()
    private var transactionCounter = 0L
    
    init {
        logger.info("🔧 Mock 블록체인 서비스 시작 (로컬 개발 모드)")
        logger.info("📍 네트워크: Local Mock Chain")
        logger.info("⚡ 가스비: 무료 (시뮬레이션)")
    }

    /**
     * 질문 생성을 Mock 체인에 기록
     */
    fun createQuestionOnChain(question: Question): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            try {
                val txHash = generateMockTxHash()
                
                logger.info("🔗 [MOCK CHAIN] 질문 생성 트랜잭션")
                logger.info("  📝 Question ID: ${question.id}")
                logger.info("  📄 제목: ${question.title}")
                logger.info("  🔖 TX Hash: $txHash")
                
                // Mock 체인에 저장
                mockChain[question.id!!] = MockQuestion(
                    questionId = question.id!!,
                    title = question.title,
                    category = question.category ?: "GENERAL",
                    totalBetPool = 0L,
                    yesBetPool = 0L,
                    noBetPool = 0L,
                    settled = false,
                    createdAt = LocalDateTime.now()
                )
                
                // 트랜잭션 기록
                mockTransactions[txHash] = MockTransaction(
                    txHash = txHash,
                    type = "CREATE_QUESTION",
                    questionId = question.id!!,
                    timestamp = LocalDateTime.now(),
                    gasUsed = "0.001 ETH (Mock)"
                )
                
                logger.info("  ✅ Mock 체인에 기록 완료!")
                Thread.sleep(100) // 실제 블록체인처럼 약간의 지연
                txHash
            } catch (e: Exception) {
                logger.error("❌ Mock chain record failed: ${e.message}", e)
                throw e  // Re-throw exception to propagate to CompletableFuture.failedFuture
            }
        }
    }

    /**
     * 배치 베팅을 Mock 체인에 기록
     */
    fun batchPlaceBetsOnChain(bets: List<BetOnChainData>): CompletableFuture<String?> {
        if (bets.isEmpty()) {
            return CompletableFuture.completedFuture(null)
        }

        return CompletableFuture.supplyAsync {
            try {
                val txHash = generateMockTxHash()
                
                logger.info("🔗 [MOCK CHAIN] 배치 베팅 트랜잭션")
                logger.info("  📊 베팅 수: ${bets.size}개")
                logger.info("  🔖 TX Hash: $txHash")
                
                // 각 베팅을 Mock 체인에 반영
                bets.forEach { bet ->
                    val mockQuestion = mockChain[bet.questionId]
                    if (mockQuestion != null) {
                        if (bet.choice) {
                            mockQuestion.yesBetPool += bet.amount
                        } else {
                            mockQuestion.noBetPool += bet.amount
                        }
                        mockQuestion.totalBetPool += bet.amount
                        
                        logger.debug("    ↳ Question #${bet.questionId}: ${bet.amount}P → ${if (bet.choice) "YES" else "NO"}")
                    }
                }
                
                // 트랜잭션 기록
                mockTransactions[txHash] = MockTransaction(
                    txHash = txHash,
                    type = "BATCH_BETS",
                    questionId = bets.first().questionId,
                    data = "Batch size: ${bets.size}",
                    timestamp = LocalDateTime.now(),
                    gasUsed = "${bets.size * 0.001} ETH (Mock)"
                )
                
                logger.info("  ✅ ${bets.size}개 베팅 Mock 체인에 기록 완료!")
                Thread.sleep(200) // 배치는 조금 더 긴 지연
                txHash
            } catch (e: Exception) {
                logger.error("❌ Mock chain batch record failed: ${e.message}", e)
                throw e  // Re-throw exception to propagate to CompletableFuture.failedFuture
            }
        }
    }

    /**
     * 정산 결과를 Mock 체인에 기록
     */
    fun settleQuestionOnChain(questionId: Long, finalResult: FinalResult): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            try {
                val txHash = generateMockTxHash()
                
                logger.info("🔗 [MOCK CHAIN] 정산 트랜잭션")
                logger.info("  📝 Question ID: $questionId")
                logger.info("  🎯 결과: $finalResult")
                logger.info("  🔖 TX Hash: $txHash")
                
                // Mock 체인에서 정산 처리
                val mockQuestion = mockChain[questionId]
                if (mockQuestion != null) {
                    mockQuestion.settled = true
                    mockQuestion.finalResult = finalResult.name
                    mockQuestion.settledAt = LocalDateTime.now()
                }
                
                // 트랜잭션 기록
                mockTransactions[txHash] = MockTransaction(
                    txHash = txHash,
                    type = "SETTLE_QUESTION",
                    questionId = questionId,
                    data = "Result: $finalResult",
                    timestamp = LocalDateTime.now(),
                    gasUsed = "0.002 ETH (Mock)"
                )
                
                logger.info("  ✅ Mock 체인 정산 완료!")
                Thread.sleep(150)
                txHash
            } catch (e: Exception) {
                logger.error("❌ Mock chain settlement failed: ${e.message}", e)
                throw e  // Re-throw exception to propagate to CompletableFuture.failedFuture
            }
        }
    }

    /**
     * Mock 체인에서 질문 데이터 조회
     */
    fun getQuestionFromChain(questionId: Long): QuestionOnChain? {
        return try {
            val mockQuestion = mockChain[questionId] ?: return null
            
            logger.info("🔍 [MOCK CHAIN] 질문 조회: #$questionId")
            
            QuestionOnChain(
                questionId = mockQuestion.questionId,
                totalBetPool = mockQuestion.totalBetPool,
                yesBetPool = mockQuestion.yesBetPool,
                noBetPool = mockQuestion.noBetPool,
                settled = mockQuestion.settled
            )
        } catch (e: Exception) {
            logger.error("Mock chain query failed: ${e.message}", e)
            null
        }
    }

    /**
     * Mock 트랜잭션 해시 생성
     */
    private fun generateMockTxHash(): String {
        transactionCounter++
        val randomPart = (1..8).map { ('a'..'f').random() }.joinToString("")
        return "0xmock${transactionCounter.toString().padStart(8, '0')}$randomPart"
    }

    /**
     * Mock 체인 상태 조회
     */
    fun getMockChainStatus(): MockChainStatus {
        return MockChainStatus(
            totalQuestions = mockChain.size,
            totalTransactions = mockTransactions.size,
            isRunning = true,
            network = "Local Mock Chain"
        )
    }
}

/**
 * Mock 질문 데이터
 */
data class MockQuestion(
    val questionId: Long,
    val title: String,
    val category: String,
    var totalBetPool: Long,
    var yesBetPool: Long,
    var noBetPool: Long,
    var settled: Boolean,
    var finalResult: String? = null,
    val createdAt: LocalDateTime,
    var settledAt: LocalDateTime? = null
)

/**
 * Mock 트랜잭션
 */
data class MockTransaction(
    val txHash: String,
    val type: String,
    val questionId: Long,
    val data: String? = null,
    val timestamp: LocalDateTime,
    val gasUsed: String
)

/**
 * Mock 체인 상태
 */
data class MockChainStatus(
    val totalQuestions: Int,
    val totalTransactions: Int,
    val isRunning: Boolean,
    val network: String
)
