package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.VoteRecord
import com.predata.backend.repository.VoteRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime

@Service
class VoteRecordService(
    private val voteRecordRepository: VoteRecordRepository
) {
    private val logger = LoggerFactory.getLogger(VoteRecordService::class.java)

    /**
     * 투표를 온체인(Mock)에 기록
     * - SHA256 해시로 tx_hash 생성
     * - vote_records 테이블에 저장
     */
    fun recordVote(activity: Activity): VoteRecord {
        val timestamp = LocalDateTime.now()
        val txHash = generateTxHash(
            memberId = activity.memberId,
            questionId = activity.questionId,
            choice = activity.choice.name,
            timestamp = timestamp
        )

        val record = VoteRecord(
            voteId = activity.id!!,
            memberId = activity.memberId,
            questionId = activity.questionId,
            choice = activity.choice,
            recordedAt = timestamp,
            txHash = txHash
        )

        val savedRecord = voteRecordRepository.save(record)
        logger.info("Vote recorded on-chain (mock): voteId=${activity.id}, txHash=$txHash")

        return savedRecord
    }

    /**
     * SHA256 해시 생성
     * data = "memberId:questionId:choice:timestamp"
     */
    private fun generateTxHash(
        memberId: Long,
        questionId: Long,
        choice: String,
        timestamp: LocalDateTime
    ): String {
        val data = "$memberId:$questionId:$choice:$timestamp"
        return MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun findByVoteId(voteId: Long): VoteRecord? {
        return voteRecordRepository.findByVoteId(voteId)
    }

    fun findByQuestionId(questionId: Long): List<VoteRecord> {
        return voteRecordRepository.findByQuestionId(questionId)
    }

    fun findByMemberId(memberId: Long): List<VoteRecord> {
        return voteRecordRepository.findByMemberId(memberId)
    }
}
