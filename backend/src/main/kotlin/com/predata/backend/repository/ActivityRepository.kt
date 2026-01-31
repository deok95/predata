package com.predata.backend.repository

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActivityRepository : JpaRepository<Activity, Long> {
    
    fun existsByMemberIdAndQuestionIdAndActivityType(
        memberId: Long,
        questionId: Long,
        activityType: ActivityType
    ): Boolean
    
    fun findByMemberId(memberId: Long): List<Activity>
    
    fun findByQuestionId(questionId: Long): List<Activity>
    
    fun findByQuestionIdAndActivityType(
        questionId: Long,
        activityType: ActivityType
    ): List<Activity>
    
    fun findByMemberIdAndActivityType(
        memberId: Long,
        activityType: ActivityType
    ): List<Activity>
}
