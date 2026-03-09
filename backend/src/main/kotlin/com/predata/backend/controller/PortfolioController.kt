package com.predata.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.service.CategoryPerformanceResponse
import com.predata.backend.service.OpenPositionResponse
import com.predata.backend.service.PortfolioService
import com.predata.backend.util.authenticatedMemberId
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@Tag(name = "finance-wallet", description = "Portfolio APIs")
@RequestMapping("/api/portfolio")
class PortfolioController(
    private val portfolioService: PortfolioService
) {
    enum class SortDir { ASC, DESC }

    @GetMapping("/summary")
    fun getPortfolioSummary(request: HttpServletRequest): ResponseEntity<ApiEnvelope<Any>> {
        val memberId = request.authenticatedMemberId()
        return ResponseEntity.ok(ApiEnvelope.ok(portfolioService.getPortfolioSummary(memberId)))
    }

    @GetMapping("/positions")
    fun getOpenPositions(
        request: HttpServletRequest,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "placedAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ResponseEntity<ApiEnvelope<Any>> {
        val memberId = request.authenticatedMemberId()
        val list = portfolioService.getOpenPositions(memberId)
        val sorted = sortOpenPositions(list, sortBy, parseSortDir(sortDir))
        return ResponseEntity.ok(ApiEnvelope.ok(paginate(sorted, page, size)))
    }

    @GetMapping("/category-breakdown")
    fun getCategoryBreakdown(
        request: HttpServletRequest,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "totalBets") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ResponseEntity<ApiEnvelope<Any>> {
        val memberId = request.authenticatedMemberId()
        val list = portfolioService.getCategoryBreakdown(memberId)
        val sorted = sortCategoryBreakdown(list, sortBy, parseSortDir(sortDir))
        return ResponseEntity.ok(ApiEnvelope.ok(paginate(sorted, page, size)))
    }

    @GetMapping("/accuracy-trend")
    fun getAccuracyTrend(
        request: HttpServletRequest,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "date") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String,
    ): ResponseEntity<ApiEnvelope<Any>> {
        val memberId = request.authenticatedMemberId()
        val list = portfolioService.getAccuracyTrend(memberId)
        val sorted = if (sortBy.equals("accuracy", true)) {
            if (parseSortDir(sortDir) == SortDir.ASC) list.sortedBy { it.accuracy } else list.sortedByDescending { it.accuracy }
        } else {
            if (parseSortDir(sortDir) == SortDir.ASC) list.sortedBy { YearMonth.parse(it.date) } else list.sortedByDescending { YearMonth.parse(it.date) }
        }
        return ResponseEntity.ok(ApiEnvelope.ok(paginate(sorted, page, size)))
    }

    private fun parseSortDir(raw: String): SortDir = if (raw.equals("asc", true)) SortDir.ASC else SortDir.DESC

    private fun sortOpenPositions(
        list: List<OpenPositionResponse>,
        sortByRaw: String,
        dir: SortDir,
    ): List<OpenPositionResponse> {
        val sortBy = when (sortByRaw.lowercase()) {
            "betamount", "bet_amount" -> "betAmount"
            "estimatedprofitloss", "estimated_profit_loss" -> "estimatedProfitLoss"
            else -> "placedAt"
        }
        val sorted = when (sortBy) {
            "betAmount" -> if (dir == SortDir.ASC) list.sortedBy { it.betAmount } else list.sortedByDescending { it.betAmount }
            "estimatedProfitLoss" -> if (dir == SortDir.ASC) list.sortedBy { it.estimatedProfitLoss } else list.sortedByDescending { it.estimatedProfitLoss }
            else -> if (dir == SortDir.ASC) list.sortedBy { it.placedAt } else list.sortedByDescending { it.placedAt }
        }
        return sorted
    }

    private fun sortCategoryBreakdown(
        list: List<CategoryPerformanceResponse>,
        sortByRaw: String,
        dir: SortDir,
    ): List<CategoryPerformanceResponse> {
        val sortBy = when (sortByRaw.lowercase()) {
            "profit" -> "profit"
            "winrate", "win_rate" -> "winRate"
            else -> "totalBets"
        }
        return when (sortBy) {
            "profit" -> if (dir == SortDir.ASC) list.sortedBy { it.profit } else list.sortedByDescending { it.profit }
            "winRate" -> if (dir == SortDir.ASC) list.sortedBy { it.winRate } else list.sortedByDescending { it.winRate }
            else -> if (dir == SortDir.ASC) list.sortedBy { it.totalBets } else list.sortedByDescending { it.totalBets }
        }
    }

    private fun <T> paginate(list: List<T>, page: Int, size: Int): List<T> {
        val p = page.coerceAtLeast(0)
        val s = size.coerceIn(1, 200)
        val from = (p * s).coerceAtMost(list.size)
        val to = (from + s).coerceAtMost(list.size)
        return list.subList(from, to)
    }
}
