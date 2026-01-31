package com.predata.backend.service

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import com.predata.backend.dto.FilterOptions
import com.predata.backend.repository.ActivityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DataQualityService(
    private val activityRepository: ActivityRepository
) {

    /**
     * 1단계: Latency 기반 필터링
     * 너무 빠른 응답(무지성 클릭) 제거
     */
    @Transactional(readOnly = true)
    fun filterByLatency(questionId: Long, minLatencyMs: Int = 2000): List<Activity> {
        return activityRepository
            .findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)
            .filter { activity ->
                activity.latencyMs != null && activity.latencyMs >= minLatencyMs
            }
    }

    /**
     * 2단계: 베팅 경험자의 투표만 추출
     * 돈을 건 사람들의 투표가 더 신뢰도 높음
     */
    @Transactional(readOnly = true)
    fun getVotesFromBettors(questionId: Long): List<Activity> {
        // 이 질문에 베팅한 사람들의 ID
        val bettorIds = activityRepository
            .findByQuestionIdAndActivityType(questionId, ActivityType.BET)
            .map { it.memberId }
            .toSet()
        
        // 베팅 경험자들의 투표만 반환
        return activityRepository
            .findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)
            .filter { it.memberId in bettorIds }
    }

    /**
     * 3단계: 복합 필터링
     * 여러 조건을 동시 적용
     */
    @Transactional(readOnly = true)
    fun applyFilters(questionId: Long, options: FilterOptions): List<Activity> {
        var votes = activityRepository
            .findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)
        
        // Latency 필터
        votes = votes.filter { 
            it.latencyMs != null && it.latencyMs >= options.minLatencyMs 
        }
        
        // 베팅 경험자만
        if (options.onlyBettors) {
            val bettorIds = activityRepository
                .findByQuestionIdAndActivityType(questionId, ActivityType.BET)
                .map { it.memberId }
                .toSet()
            votes = votes.filter { it.memberId in bettorIds }
        }
        
        return votes
    }

    /**
     * 빠른 클릭 탐지 (봇 의심)
     */
    @Transactional(readOnly = true)
    fun detectFastClickers(questionId: Long, thresholdMs: Int = 1000): List<Activity> {
        return activityRepository
            .findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)
            .filter { it.latencyMs != null && it.latencyMs < thresholdMs }
    }

    /**
     * 품질 점수 계산 (0-100점)
     */
    @Transactional(readOnly = true)
    fun calculateQualityScore(questionId: Long): Double {
        val allVotes = activityRepository
            .findByQuestionIdAndActivityType(questionId, ActivityType.VOTE)
        
        if (allVotes.isEmpty()) return 0.0
        
        var score = 100.0
        
        // 1. Latency 체크 (빠른 응답 비율만큼 감점)
        val fastClickers = allVotes.count { 
            it.latencyMs != null && it.latencyMs < 1000 
        }
        score -= (fastClickers * 100.0 / allVotes.size) * 0.3 // 최대 30점 감점
        
        // 2. 베팅 경험자 비율 (높을수록 가산)
        val bettorIds = activityRepository
            .findByQuestionIdAndActivityType(questionId, ActivityType.BET)
            .map { it.memberId }
            .toSet()
        val bettorVotes = allVotes.count { it.memberId in bettorIds }
        val bettorRatio = bettorVotes * 100.0 / allVotes.size
        score += (bettorRatio - 50) * 0.2 // 50% 기준으로 가감
        
        return score.coerceIn(0.0, 100.0)
    }
}
