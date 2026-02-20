package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.NotificationResponse
import com.predata.backend.service.NotificationService
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun getNotifications(httpRequest: HttpServletRequest): ResponseEntity<ApiEnvelope<List<NotificationResponse>>> {
        val memberId = httpRequest.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(notificationService.getNotifications(memberId)))
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(httpRequest: HttpServletRequest): ResponseEntity<ApiEnvelope<UnreadCountResponse>> {
        val memberId = httpRequest.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(UnreadCountResponse(count = notificationService.getUnreadCount(memberId))))
    }

    @PostMapping("/{id}/read")
    fun markAsRead(@PathVariable id: Long): ResponseEntity<ApiEnvelope<MarkReadResponse>> {
        notificationService.markAsRead(id)
        return ResponseEntity.ok(ApiEnvelope.ok(MarkReadResponse(success = true)))
    }

    @PostMapping("/read-all")
    fun markAllAsRead(httpRequest: HttpServletRequest): ResponseEntity<ApiEnvelope<MarkAllReadResponse>> {
        val memberId = httpRequest.authenticatedMemberId()
        val count = notificationService.markAllAsRead(memberId)
        return ResponseEntity.ok(ApiEnvelope.ok(MarkAllReadResponse(success = true, updated = count)))
    }
}

data class UnreadCountResponse(val count: Long)
data class MarkReadResponse(val success: Boolean)
data class MarkAllReadResponse(val success: Boolean, val updated: Int)
