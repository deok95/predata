package com.predata.backend.service

import com.predata.backend.domain.SportsMatch
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.repository.SportsMatchRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class BettingSuspensionService(
    private val sportsMatchRepository: SportsMatchRepository,
    private val questionRepository: QuestionRepository
) {

    private val logger = LoggerFactory.getLogger(BettingSuspensionService::class.java)

    // Match(sports domain) 기반 베팅 정지 추적 (matchId → suspendedUntil)
    private val matchSuspensions = ConcurrentHashMap<Long, LocalDateTime>()

    /**
     * Match(sports domain) 기반 - 골 이벤트 시 30초 베팅 정지
     */
    fun suspendBettingForMatch(matchId: Long) {
        val until = LocalDateTime.now().plusSeconds(30)
        matchSuspensions[matchId] = until
        logger.info("[BettingSuspension] matchId={} 베팅 30초 정지 (until={})", matchId, until)
    }

    /**
     * Match(sports domain) 기반 - 베팅 정지 여부 확인
     */
    fun isMatchBettingSuspended(matchId: Long): Boolean {
        val until = matchSuspensions[matchId] ?: return false
        if (LocalDateTime.now().isAfter(until)) {
            matchSuspensions.remove(matchId)
            return false
        }
        return true
    }

    /**
     * 스코어 변경 시 베팅 일시 중지 (30초)
     */
    @Transactional
    fun suspendBettingOnScoreChange(match: SportsMatch, oldHomeScore: Int?, oldAwayScore: Int?) {
        val currentHomeScore = match.homeScore ?: 0
        val currentAwayScore = match.awayScore ?: 0
        
        // 스코어 변경 감지
        val scoreChanged = (oldHomeScore != currentHomeScore) || (oldAwayScore != currentAwayScore)
        
        if (scoreChanged && match.status == "LIVE") {
            // 30초 동안 베팅 중지
            match.bettingSuspendedUntil = LocalDateTime.now().plusSeconds(30)
            sportsMatchRepository.save(match)
            
            println("[BettingSuspension] 🚫 골! 베팅 30초 중지: ${match.homeTeam} ${currentHomeScore}-${currentAwayScore} ${match.awayTeam}")
            
            // 연결된 질문도 베팅 중지 상태로 업데이트
            if (match.questionId != null) {
                val question = questionRepository.findById(match.questionId!!).orElse(null)
                if (question != null && question.status == QuestionStatus.VOTING) {
                    // 질문 상태는 OPEN 유지하되, 프론트엔드에서 체크할 수 있도록 함
                    questionRepository.save(question)
                }
            }
        }
    }

    /**
     * 베팅이 현재 중지 상태인지 확인
     */
    fun isBettingSuspended(matchId: Long): Boolean {
        val match = sportsMatchRepository.findById(matchId).orElse(null) ?: return false
        
        if (match.bettingSuspendedUntil == null) {
            return false
        }
        
        val now = LocalDateTime.now()
        val isSuspended = now.isBefore(match.bettingSuspendedUntil)
        
        // 중지 시간이 지났으면 자동으로 null로 설정
        if (!isSuspended && match.bettingSuspendedUntil != null) {
            match.bettingSuspendedUntil = null
            sportsMatchRepository.save(match)
            println("[BettingSuspension] ✅ 베팅 재개: ${match.homeTeam} vs ${match.awayTeam}")
        }
        
        return isSuspended
    }

    /**
     * 질문 ID로 베팅 중지 여부 확인
     */
    fun isBettingSuspendedByQuestionId(questionId: Long): BettingSuspensionStatus {
        val match = sportsMatchRepository.findByQuestionId(questionId) ?: return BettingSuspensionStatus(
            suspended = false,
            resumeAt = null,
            remainingSeconds = 0
        )
        
        if (match.bettingSuspendedUntil == null) {
            return BettingSuspensionStatus(
                suspended = false,
                resumeAt = null,
                remainingSeconds = 0
            )
        }
        
        val now = LocalDateTime.now()
        val isSuspended = now.isBefore(match.bettingSuspendedUntil)
        
        if (!isSuspended) {
            // 자동 재개
            match.bettingSuspendedUntil = null
            sportsMatchRepository.save(match)
            return BettingSuspensionStatus(
                suspended = false,
                resumeAt = null,
                remainingSeconds = 0
            )
        }
        
        val remainingSeconds = java.time.Duration.between(now, match.bettingSuspendedUntil).seconds
        
        return BettingSuspensionStatus(
            suspended = true,
            resumeAt = match.bettingSuspendedUntil.toString(),
            remainingSeconds = remainingSeconds.toInt()
        )
    }

    /**
     * 베팅 재개 시간 확인 (남은 시간)
     */
    fun getRemainingCooldownTime(matchId: Long): Int {
        val match = sportsMatchRepository.findById(matchId).orElse(null) ?: return 0
        
        if (match.bettingSuspendedUntil == null) {
            return 0
        }
        
        val now = LocalDateTime.now()
        if (now.isAfter(match.bettingSuspendedUntil)) {
            return 0
        }
        
        return java.time.Duration.between(now, match.bettingSuspendedUntil).seconds.toInt()
    }
}

data class BettingSuspensionStatus(
    val suspended: Boolean,
    val resumeAt: String?,
    val remainingSeconds: Int
)
