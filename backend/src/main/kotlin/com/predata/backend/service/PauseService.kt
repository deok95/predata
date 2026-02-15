package com.predata.backend.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * 투표 일시 중지/재개 서비스
 * - 특정 질문 또는 전체 투표 중지/재개
 * - 인메모리 상태 관리 (ConcurrentHashMap)
 */
@Service
class PauseService {
    private val logger = LoggerFactory.getLogger(PauseService::class.java)

    // 질문별 중지 상태 (questionId -> isPaused)
    private val questionPauseStatus = ConcurrentHashMap<Long, Boolean>()

    // 전체 중지 상태
    @Volatile
    private var globalPaused = false

    /**
     * 특정 질문 투표 일시 중지
     */
    fun pauseVoting(questionId: Long) {
        questionPauseStatus[questionId] = true
        logger.warn("Voting paused for questionId=$questionId")
    }

    /**
     * 특정 질문 투표 재개
     */
    fun resumeVoting(questionId: Long) {
        questionPauseStatus[questionId] = false
        logger.info("Voting resumed for questionId=$questionId")
    }

    /**
     * 전체 투표 중지
     */
    fun pauseAll() {
        globalPaused = true
        logger.warn("All voting paused globally")
    }

    /**
     * 전체 투표 재개
     */
    fun resumeAll() {
        globalPaused = false
        logger.info("All voting resumed globally")
    }

    /**
     * 특정 질문 중지 상태 확인
     * - 전체 중지 상태이거나, 질문별 중지 상태인 경우 true
     */
    fun isPaused(questionId: Long): Boolean {
        if (globalPaused) return true
        return questionPauseStatus[questionId] ?: false
    }

    /**
     * 전체 중지 상태 확인
     */
    fun isGlobalPaused(): Boolean = globalPaused

    /**
     * 모든 중지 상태 조회 (관리자용)
     */
    fun getAllPauseStatus(): Map<String, Any> {
        return mapOf(
            "globalPaused" to globalPaused,
            "pausedQuestions" to questionPauseStatus.filter { it.value }.keys.toList()
        )
    }
}
