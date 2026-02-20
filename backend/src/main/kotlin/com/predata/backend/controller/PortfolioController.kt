package com.predata.backend.controller

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.exception.UnauthorizedException
import com.predata.backend.service.PortfolioService
import com.predata.backend.util.authenticatedMemberId
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
        return try {
            val memberId = request.authenticatedMemberId()
            ResponseEntity.ok(portfolioService.getPortfolioSummary(memberId))
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            // Return default portfolio summary on any error
            ResponseEntity.ok(mapOf(
                "memberId" to 0L,
                "totalInvested" to 0L,
                "totalReturns" to 0L,
                "netProfit" to 0L,
                "unrealizedValue" to 0L,
                "currentBalance" to 0L,
                "winRate" to 0.0,
                "totalBets" to 0,
                "openBets" to 0,
                "settledBets" to 0,
                "roi" to 0.0
            ))
        }
    }

    @GetMapping("/positions")
    fun getOpenPositions(request: HttpServletRequest): ResponseEntity<Any> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(portfolioService.getOpenPositions(memberId))
    }

    @GetMapping("/category-breakdown")
    fun getCategoryBreakdown(request: HttpServletRequest): ResponseEntity<Any> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(portfolioService.getCategoryBreakdown(memberId))
    }

    @GetMapping("/accuracy-trend")
    fun getAccuracyTrend(request: HttpServletRequest): ResponseEntity<Any> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(portfolioService.getAccuracyTrend(memberId))
    }
}
