package com.predata.backend.controller

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.exception.UnauthorizedException
import com.predata.backend.service.PortfolioService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/portfolio")
class PortfolioController(
    private val portfolioService: PortfolioService
) {

    @GetMapping("/summary")
    fun getPortfolioSummary(request: HttpServletRequest): ResponseEntity<Any> {
        val memberId = request.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: throw UnauthorizedException()
        return ResponseEntity.ok(portfolioService.getPortfolioSummary(memberId))
    }

    @GetMapping("/positions")
    fun getOpenPositions(request: HttpServletRequest): ResponseEntity<Any> {
        val memberId = request.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: throw UnauthorizedException()
        return ResponseEntity.ok(portfolioService.getOpenPositions(memberId))
    }

    @GetMapping("/category-breakdown")
    fun getCategoryBreakdown(request: HttpServletRequest): ResponseEntity<Any> {
        val memberId = request.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: throw UnauthorizedException()
        return ResponseEntity.ok(portfolioService.getCategoryBreakdown(memberId))
    }

    @GetMapping("/accuracy-trend")
    fun getAccuracyTrend(request: HttpServletRequest): ResponseEntity<Any> {
        val memberId = request.getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
            ?: throw UnauthorizedException()
        return ResponseEntity.ok(portfolioService.getAccuracyTrend(memberId))
    }
}
