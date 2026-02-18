package com.predata.backend.service

import com.predata.backend.dto.BetOnChainData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 베팅 배치 처리 서비스
 * 베팅을 큐에 모았다가 10초마다 배치로 온체인에 전송 (가스비 절감)
 */
@Service
class BettingBatchService(
    private val blockchainService: BlockchainService,
    @Value("\${app.scheduler.enabled:true}")
    private val schedulerEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(BettingBatchService::class.java)
    private val bettingQueue = ConcurrentLinkedQueue<PendingBet>()
    
    companion object {
        private const val BATCH_SIZE = 50 // 한 번에 처리할 최대 베팅 수
    }
    
    /**
     * 베팅을 큐에 추가
     */
    fun enqueueBet(bet: PendingBet) {
        bettingQueue.offer(bet)
        logger.debug("베팅 큐 추가: Question #${bet.questionId}, User ${bet.userAddress}")
    }
    
    /**
     * 10초마다 큐에 쌓인 베팅을 배치로 처리
     */
    @Scheduled(fixedDelay = 10000) // 10초마다
    fun processBatchBets() {
        // 스케줄러 비활성화 체크
        if (!schedulerEnabled) {
            logger.debug("[BettingBatch] 스케줄러 비활성화됨 (app.scheduler.enabled=false)")
            return
        }

        if (bettingQueue.isEmpty()) {
            return
        }
        
        val betsToProcess = mutableListOf<PendingBet>()
        
        // 큐에서 최대 BATCH_SIZE개 꺼내기
        repeat(BATCH_SIZE) {
            val bet = bettingQueue.poll() ?: return@repeat
            betsToProcess.add(bet)
        }
        
        if (betsToProcess.isEmpty()) {
            return
        }
        
        logger.info("⚡ 배치 베팅 처리 시작: ${betsToProcess.size}개")
        
        // 온체인 데이터로 변환
        val onChainBets = betsToProcess.map { bet ->
            BetOnChainData(
                questionId = bet.questionId,
                userAddress = bet.userAddress,
                choice = bet.choice == "YES",
                amount = bet.amount
            )
        }
        
        // 블록체인에 배치 전송
        blockchainService.batchPlaceBetsOnChain(onChainBets)
            .thenAccept { txHash ->
                if (txHash != null) {
                    logger.info("✅ 배치 베팅 온체인 기록 완료: $txHash")
                } else {
                    logger.warn("⚠️ 배치 베팅 온체인 기록 실패 (블록체인 비활성화 또는 오류)")
                }
            }
            .exceptionally { e ->
                logger.error("❌ 배치 베팅 처리 실패: ${e.message}", e)
                null
            }
    }
    
    /**
     * 큐 상태 조회
     */
    fun getQueueStatus(): QueueStatus {
        return QueueStatus(
            queueSize = bettingQueue.size,
            isProcessing = bettingQueue.isNotEmpty()
        )
    }
}

/**
 * 큐에 대기 중인 베팅
 */
data class PendingBet(
    val questionId: Long,
    val userAddress: String,
    val choice: String, // "YES" or "NO"
    val amount: Long
)

/**
 * 큐 상태
 */
data class QueueStatus(
    val queueSize: Int,
    val isProcessing: Boolean
)
