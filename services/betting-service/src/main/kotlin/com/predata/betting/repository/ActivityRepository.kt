package com.predata.betting.repository

import com.predata.common.domain.ActivityType
import com.predata.betting.domain.Activity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActivityRepository : JpaRepository<Activity, Long> {
    fun findByQuestionId(questionId: Long): List<Activity>
    fun findByMemberId(memberId: Long): List<Activity>
    fun findByQuestionIdAndActivityType(questionId: Long, activityType: ActivityType): List<Activity>
    fun existsByMemberIdAndQuestionIdAndActivityType(
        memberId: Long,
        questionId: Long,
        activityType: ActivityType
    ): Boolean
}
