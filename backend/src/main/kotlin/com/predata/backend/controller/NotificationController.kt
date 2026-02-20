package com.predata.backend.controller

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.exception.UnauthorizedException
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
    fun getNotifications(httpRequest: HttpServletRequest): ResponseEntity<List<NotificationResponse>> {
        return try {
            val memberId = httpRequest.authenticatedMemberId()
            ResponseEntity.ok(notificationService.getNotifications(memberId))
        } catch (e: Exception) {
            // Return empty list on any error
            ResponseEntity.ok(emptyList())
        }
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(httpRequest: HttpServletRequest): ResponseEntity<Map<String, Long>> {
        val memberId = httpRequest.authenticatedMemberId()
        return ResponseEntity.ok(mapOf("count" to notificationService.getUnreadCount(memberId)))
    }

    @PostMapping("/{id}/read")
    fun markAsRead(@PathVariable id: Long): ResponseEntity<Map<String, Boolean>> {
        notificationService.markAsRead(id)
        return ResponseEntity.ok(mapOf("success" to true))
    }

    @PostMapping("/read-all")
    fun markAllAsRead(httpRequest: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val memberId = httpRequest.authenticatedMemberId()
        val count = notificationService.markAllAsRead(memberId)
        return ResponseEntity.ok(mapOf("success" to true, "updated" to count))
    }
}
