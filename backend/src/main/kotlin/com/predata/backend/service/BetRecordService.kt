package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.BetRecord
import com.predata.backend.domain.Choice
import com.predata.backend.domain.Position
import com.predata.backend.repository.BetRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime

@Service
class BetRecordService(
    private val betRecordRepository: BetRecordRepository
) {
    private val logger = LoggerFactory.getLogger(BetRecordService::class.java)

    /**
     * 베팅을 온체인(Mock)에 기록
     * - SHA256 해시로 tx_hash 생성
     * - bet_records 테이블에 저장
     * - position: YES -> LONG, NO -> SHORT
     */
    fun recordBet(activity: Activity): BetRecord {
        val timestamp = LocalDateTime.now()
        val position = choiceToPosition(activity.choice)
        val txHash = generateTxHash(
            memberId = activity.memberId,
            questionId = activity.questionId,
            position = position.name,
            amount = activity.amount,
            timestamp = timestamp
        )

        val record = BetRecord(
            betId = activity.id!!,
            memberId = activity.memberId,
            questionId = activity.questionId,
            position = position,
            amount = activity.amount,
            recordedAt = timestamp,
            txHash = txHash
        )

        val savedRecord = betRecordRepository.save(record)
        logger.info("Bet recorded on-chain (mock): betId=${activity.id}, position=$position, amount=${activity.amount}, txHash=$txHash")

        return savedRecord
    }

    /**
     * Choice를 Position으로 변환
     * YES -> LONG (상승 베팅)
     * NO -> SHORT (하락 베팅)
     */
    private fun choiceToPosition(choice: Choice): Position {
        return when (choice) {
            Choice.YES -> Position.LONG
            Choice.NO -> Position.SHORT
        }
    }

    /**
     * SHA256 해시 생성
     * data = "memberId:questionId:position:amount:timestamp"
     */
    private fun generateTxHash(
        memberId: Long,
        questionId: Long,
        position: String,
        amount: Long,
        timestamp: LocalDateTime
    ): String {
        val data = "$memberId:$questionId:$position:$amount:$timestamp"
        return MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun findByBetId(betId: Long): BetRecord? {
        return betRecordRepository.findByBetId(betId)
    }

    fun findByQuestionId(questionId: Long): List<BetRecord> {
        return betRecordRepository.findByQuestionId(questionId)
    }

    fun findByMemberId(memberId: Long): List<BetRecord> {
        return betRecordRepository.findByMemberId(memberId)
    }
}
