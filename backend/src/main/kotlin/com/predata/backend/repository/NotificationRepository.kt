package com.predata.backend.repository

import com.predata.backend.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<Notification>

    fun countByMemberIdAndIsReadFalse(memberId: Long): Long

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.memberId = :memberId AND n.isRead = false")
    fun markAllAsReadByMemberId(memberId: Long): Int
}
