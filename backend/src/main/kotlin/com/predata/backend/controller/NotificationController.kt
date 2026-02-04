package com.predata.backend.controller

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.exception.UnauthorizedException
import com.predata.backend.service.NotificationResponse
import com.predata.backend.service.NotificationService
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
        val memberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long ?: throw UnauthorizedException()
        return ResponseEntity.ok(notificationService.getNotifications(memberId))
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(httpRequest: HttpServletRequest): ResponseEntity<Map<String, Long>> {
        val memberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long ?: throw UnauthorizedException()
        return ResponseEntity.ok(mapOf("count" to notificationService.getUnreadCount(memberId)))
    }

    @PostMapping("/{id}/read")
    fun markAsRead(@PathVariable id: Long): ResponseEntity<Map<String, Boolean>> {
        notificationService.markAsRead(id)
        return ResponseEntity.ok(mapOf("success" to true))
    }

    @PostMapping("/read-all")
    fun markAllAsRead(httpRequest: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val memberId = httpRequest.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long ?: throw UnauthorizedException()
        val count = notificationService.markAllAsRead(memberId)
        return ResponseEntity.ok(mapOf("success" to true, "updated" to count))
    }
}
