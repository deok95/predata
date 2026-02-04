package com.predata.backend.service

import com.predata.backend.domain.Notification
import com.predata.backend.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    fun createNotification(
        memberId: Long,
        type: String,
        title: String,
        message: String,
        relatedQuestionId: Long? = null
    ): Notification {
        val notification = Notification(
            memberId = memberId,
            type = type,
            title = title,
            message = message,
            relatedQuestionId = relatedQuestionId
        )
        val saved = notificationRepository.save(notification)
        logger.debug("[Notification] type={}, memberId={}", type, memberId)
        return saved
    }

    @Transactional(readOnly = true)
    fun getNotifications(memberId: Long): List<NotificationResponse> {
        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId).map { n ->
            NotificationResponse(
                id = n.id!!,
                type = n.type,
                title = n.title,
                message = n.message,
                relatedQuestionId = n.relatedQuestionId,
                read = n.isRead,
                createdAt = n.createdAt.toString()
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(memberId: Long): Long {
        return notificationRepository.countByMemberIdAndIsReadFalse(memberId)
    }

    @Transactional
    fun markAsRead(notificationId: Long) {
        val notification = notificationRepository.findById(notificationId).orElse(null) ?: return
        notification.isRead = true
        notificationRepository.save(notification)
    }

    @Transactional
    fun markAllAsRead(memberId: Long): Int {
        return notificationRepository.markAllAsReadByMemberId(memberId)
    }
}

data class NotificationResponse(
    val id: Long,
    val type: String,
    val title: String,
    val message: String,
    val relatedQuestionId: Long?,
    val read: Boolean,
    val createdAt: String
)
