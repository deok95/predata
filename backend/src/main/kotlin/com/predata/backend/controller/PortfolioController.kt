package com.predata.backend.controller

import com.predata.backend.dto.ApiEnvelope
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
    fun getPortfolioSummary(request: HttpServletRequest): ResponseEntity<ApiEnvelope<Any>> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(portfolioService.getPortfolioSummary(memberId)))
    }

    @GetMapping("/positions")
    fun getOpenPositions(request: HttpServletRequest): ResponseEntity<ApiEnvelope<Any>> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(portfolioService.getOpenPositions(memberId)))
    }

    @GetMapping("/category-breakdown")
    fun getCategoryBreakdown(request: HttpServletRequest): ResponseEntity<ApiEnvelope<Any>> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(portfolioService.getCategoryBreakdown(memberId)))
    }

    @GetMapping("/accuracy-trend")
    fun getAccuracyTrend(request: HttpServletRequest): ResponseEntity<ApiEnvelope<Any>> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(portfolioService.getAccuracyTrend(memberId)))
    }
}
