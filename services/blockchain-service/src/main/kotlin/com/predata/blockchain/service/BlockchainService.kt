package com.predata.blockchain.service

import com.predata.blockchain.domain.FinalResult
import com.predata.blockchain.domain.Question
import com.predata.blockchain.dto.BetOnChainData
import com.predata.blockchain.dto.BlockchainStatusResponse
import com.predata.blockchain.dto.QuestionOnChain
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

/**
 * 블록체인 서비스 (통합)
 * - 개발 환경: MockBlockchainService 사용
 * - 프로덕션 환경: 실제 Web3j 사용 (나중에 구현)
 */
@Service
class BlockchainService(
    @Value("\${blockchain.enabled:false}") private val blockchainEnabled: Boolean,
    @Value("\${blockchain.mode:mock}") private val blockchainMode: String, // mock or real
    private val mockBlockchainService: MockBlockchainService
) {
    private val logger = LoggerFactory.getLogger(BlockchainService::class.java)

    init {
        if (blockchainEnabled) {
            logger.info("🔗 블록체인 서비스 활성화")
            logger.info("📍 모드: ${if (blockchainMode == "mock") "Mock Chain (개발)" else "Real Chain (프로덕션)"}")
        } else {
            logger.info("⚠️ 블록체인 서비스 비활성화 (DB만 사용)")
        }
    }

    /**
     * 질문 생성을 온체인에 기록
     */
    fun createQuestionOnChain(question: Question): CompletableFuture<String?> {
        if (!blockchainEnabled) {
            logger.debug("블록체인 비활성화 상태 - 질문 생성 건너뜀")
            return CompletableFuture.completedFuture(null)
        }

        return when (blockchainMode) {
            "mock" -> mockBlockchainService.createQuestionOnChain(question)
            "real" -> createQuestionOnRealChain(question)
            else -> {
                logger.warn("알 수 없는 블록체인 모드: $blockchainMode")
                CompletableFuture.completedFuture(null)
            }
        }
    }

    /**
     * 배치 베팅을 온체인에 기록
     */
    fun batchPlaceBetsOnChain(bets: List<BetOnChainData>): CompletableFuture<String?> {
        if (!blockchainEnabled || bets.isEmpty()) {
            return CompletableFuture.completedFuture(null)
        }

        return when (blockchainMode) {
            "mock" -> mockBlockchainService.batchPlaceBetsOnChain(bets)
            "real" -> batchPlaceBetsOnRealChain(bets)
            else -> {
                logger.warn("알 수 없는 블록체인 모드: $blockchainMode")
                CompletableFuture.completedFuture(null)
            }
        }
    }

    /**
     * 정산 결과를 온체인에 기록
     */
    fun settleQuestionOnChain(questionId: Long, finalResult: FinalResult): CompletableFuture<String?> {
        if (!blockchainEnabled) {
            return CompletableFuture.completedFuture(null)
        }

        return when (blockchainMode) {
            "mock" -> mockBlockchainService.settleQuestionOnChain(questionId, finalResult)
            "real" -> settleQuestionOnRealChain(questionId, finalResult)
            else -> {
                logger.warn("알 수 없는 블록체인 모드: $blockchainMode")
                CompletableFuture.completedFuture(null)
            }
        }
    }

    /**
     * 온체인에서 질문 데이터 조회
     */
    fun getQuestionFromChain(questionId: Long): QuestionOnChain? {
        if (!blockchainEnabled) {
            return null
        }

        return when (blockchainMode) {
            "mock" -> mockBlockchainService.getQuestionFromChain(questionId)
            "real" -> null // TODO: 실제 체인 조회 구현
            else -> null
        }
    }

    /**
     * 블록체인 상태 조회
     */
    fun getBlockchainStatus(): BlockchainStatusResponse {
        if (!blockchainEnabled) {
            return BlockchainStatusResponse(
                enabled = false,
                network = "Disabled",
                totalQuestions = 0,
                totalTransactions = 0
            )
        }

        return when (blockchainMode) {
            "mock" -> {
                val status = mockBlockchainService.getMockChainStatus()
                BlockchainStatusResponse(
                    enabled = true,
                    network = status.network,
                    totalQuestions = status.totalQuestions,
                    totalTransactions = status.totalTransactions
                )
            }
            "real" -> {
                // TODO: 실제 체인 상태 조회
                BlockchainStatusResponse(
                    enabled = true,
                    network = "Base Sepolia",
                    totalQuestions = 0,
                    totalTransactions = 0
                )
            }
            else -> BlockchainStatusResponse(
                enabled = false,
                network = "Unknown",
                totalQuestions = 0,
                totalTransactions = 0
            )
        }
    }

    // ===== 실제 블록체인 연동 (나중에 구현) =====

    private fun createQuestionOnRealChain(question: Question): CompletableFuture<String?> {
        // TODO: Web3j를 사용한 실제 온체인 기록
        logger.warn("실제 블록체인 연동은 아직 구현되지 않았습니다.")
        return CompletableFuture.completedFuture(null)
    }

    private fun batchPlaceBetsOnRealChain(bets: List<BetOnChainData>): CompletableFuture<String?> {
        // TODO: Web3j를 사용한 실제 온체인 베팅 기록
        logger.warn("실제 블록체인 연동은 아직 구현되지 않았습니다.")
        return CompletableFuture.completedFuture(null)
    }

    private fun settleQuestionOnRealChain(questionId: Long, finalResult: FinalResult): CompletableFuture<String?> {
        // TODO: Web3j를 사용한 실제 온체인 정산
        logger.warn("실제 블록체인 연동은 아직 구현되지 않았습니다.")
        return CompletableFuture.completedFuture(null)
    }
}
