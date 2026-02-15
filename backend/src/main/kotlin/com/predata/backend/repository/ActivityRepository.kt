package com.predata.backend.repository

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

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

    fun findByMemberIdAndQuestionId(
        memberId: Long,
        questionId: Long
    ): List<Activity>

    fun findByIpAddress(ipAddress: String): List<Activity>

    fun findByParentBetIdAndActivityType(
        parentBetId: Long,
        activityType: ActivityType
    ): Activity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Activity a WHERE a.id = :id")
    fun findByIdWithLock(id: Long): Optional<Activity>
}
