package com.predata.backend.service

import com.predata.backend.domain.policy.QuestionQuotaPolicy
import com.predata.backend.exception.ConflictException
import com.predata.backend.exception.ErrorCode
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class QuestionQuotaService(
    private val questionRepository: QuestionRepository,
    private val memberRepository: MemberRepository,
) {
    fun validateCanCreate(memberId: Long) {
        if (isAdmin(memberId)) return
        enforceDailyCreateLimit(memberId)
        enforceActiveQuestionLock(memberId)
    }

    private fun isAdmin(memberId: Long): Boolean =
        QuestionQuotaPolicy.shouldBypassQuota(
            memberRepository.findById(memberId).orElse(null)?.role
        )

    private fun enforceDailyCreateLimit(memberId: Long) {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val start = now.toLocalDate().atStartOfDay()
        val end = start.plusDays(1)
        val count = questionRepository.countByCreatorMemberIdAndCreatedAtBetween(memberId, start, end)
        if (QuestionQuotaPolicy.exceedsDailyCreateLimit(count)) {
            throw ConflictException(
                ErrorCode.DAILY_CREATE_LIMIT_EXCEEDED.message,
                ErrorCode.DAILY_CREATE_LIMIT_EXCEEDED.name,
            )
        }
    }

    private fun enforceActiveQuestionLock(memberId: Long) {
        val existsActive = questionRepository.existsByCreatorMemberIdAndStatusIn(
            memberId,
            QuestionQuotaPolicy.ACTIVE_LOCK_STATUSES.toList(),
        )
        if (QuestionQuotaPolicy.shouldBlockForActiveQuestion(existsActive)) {
            throw ConflictException(
                ErrorCode.ACTIVE_QUESTION_EXISTS.message,
                ErrorCode.ACTIVE_QUESTION_EXISTS.name,
            )
        }
    }
}
