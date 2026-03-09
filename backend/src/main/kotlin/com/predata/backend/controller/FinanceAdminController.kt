package com.predata.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag

import com.predata.backend.dto.ApiEnvelope
import com.predata.backend.exception.NotFoundException
import com.predata.backend.repository.MemberWalletRepository
import com.predata.backend.repository.TreasuryLedgerRepository
import com.predata.backend.repository.WalletLedgerRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@Tag(name = "finance-wallet", description = "Finance admin APIs")
@RequestMapping("/api/admin/finance")
class FinanceAdminController(
    private val memberWalletRepository: MemberWalletRepository,
    private val walletLedgerRepository: WalletLedgerRepository,
    private val treasuryLedgerRepository: TreasuryLedgerRepository,
) {
    enum class SortDir { ASC, DESC }

    @GetMapping("/wallets/{memberId}")
    fun getWallet(
        @PathVariable memberId: Long,
    ): ResponseEntity<ApiEnvelope<MemberWalletResponse>> {
        val wallet = memberWalletRepository.findByMemberId(memberId)
            ?: throw NotFoundException("Wallet not found.")
        return ResponseEntity.ok(
            ApiEnvelope.ok(
                MemberWalletResponse(
                    memberId = wallet.memberId,
                    availableBalance = wallet.availableBalance,
                    lockedBalance = wallet.lockedBalance,
                    updatedAt = wallet.updatedAt,
                )
            )
        )
    }

    @GetMapping("/wallet-ledgers")
    fun getWalletLedgers(
        @RequestParam memberId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ResponseEntity<ApiEnvelope<PagedResponse<WalletLedgerRow>>> {
        val pageable = buildPageable(page, size, normalizeWalletSortBy(sortBy), parseSortDir(sortDir))
        val result = walletLedgerRepository.findByMemberId(memberId, pageable)
        val rows = result.content.map {
            WalletLedgerRow(
                ledgerId = it.id ?: 0L,
                direction = it.direction.name,
                txType = it.txType,
                amount = it.amount,
                balanceAfter = it.balanceAfter,
                lockedBalanceAfter = it.lockedBalanceAfter,
                referenceType = it.referenceType,
                referenceId = it.referenceId,
                createdAt = it.createdAt,
            )
        }
        return ResponseEntity.ok(
            ApiEnvelope.ok(
                PagedResponse(
                    page = result.number,
                    size = result.size,
                    totalElements = result.totalElements,
                    totalPages = result.totalPages,
                    items = rows,
                )
            )
        )
    }

    @GetMapping("/treasury-ledgers")
    fun getTreasuryLedgers(
        @RequestParam(required = false) txType: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ResponseEntity<ApiEnvelope<PagedResponse<TreasuryLedgerRow>>> {
        val pageable = buildPageable(page, size, normalizeTreasurySortBy(sortBy), parseSortDir(sortDir))
        val result = if (txType.isNullOrBlank()) {
            treasuryLedgerRepository.findAll(pageable)
        } else {
            treasuryLedgerRepository.findByTxType(txType, pageable)
        }
        val rows = result.content.map {
            TreasuryLedgerRow(
                id = it.id ?: 0L,
                txType = it.txType,
                amount = it.amount,
                asset = it.asset,
                referenceType = it.referenceType,
                referenceId = it.referenceId,
                createdAt = it.createdAt,
            )
        }
        return ResponseEntity.ok(
            ApiEnvelope.ok(
                PagedResponse(
                    page = result.number,
                    size = result.size,
                    totalElements = result.totalElements,
                    totalPages = result.totalPages,
                    items = rows,
                )
            )
        )
    }

    private fun parseSortDir(raw: String): SortDir {
        return when (raw.lowercase()) {
            "asc" -> SortDir.ASC
            "desc" -> SortDir.DESC
            else -> SortDir.DESC
        }
    }

    private fun normalizeWalletSortBy(raw: String): String {
        return when (raw.lowercase()) {
            "createdat", "created_at" -> "createdAt"
            "amount" -> "amount"
            else -> "createdAt"
        }
    }

    private fun normalizeTreasurySortBy(raw: String): String {
        return when (raw.lowercase()) {
            "createdat", "created_at" -> "createdAt"
            "amount" -> "amount"
            else -> "createdAt"
        }
    }

    private fun buildPageable(page: Int, size: Int, sortBy: String, sortDir: SortDir): PageRequest {
        val direction = if (sortDir == SortDir.ASC) Sort.Direction.ASC else Sort.Direction.DESC
        return PageRequest.of(page, size.coerceIn(1, 200), Sort.by(direction, sortBy))
    }
}

data class MemberWalletResponse(
    val memberId: Long,
    val availableBalance: BigDecimal,
    val lockedBalance: BigDecimal,
    val updatedAt: LocalDateTime,
)

data class WalletLedgerRow(
    val ledgerId: Long,
    val direction: String,
    val txType: String,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val lockedBalanceAfter: BigDecimal,
    val referenceType: String?,
    val referenceId: Long?,
    val createdAt: LocalDateTime,
)

data class TreasuryLedgerRow(
    val id: Long,
    val txType: String,
    val amount: BigDecimal,
    val asset: String,
    val referenceType: String?,
    val referenceId: Long?,
    val createdAt: LocalDateTime,
)

data class PagedResponse<T>(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val items: List<T>,
)
