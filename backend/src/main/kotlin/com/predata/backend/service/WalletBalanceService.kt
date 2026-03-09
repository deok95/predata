package com.predata.backend.service

import com.predata.backend.domain.MemberWallet
import com.predata.backend.domain.TreasuryLedger
import com.predata.backend.domain.WalletLedger
import com.predata.backend.domain.WalletLedgerDirection
import com.predata.backend.domain.policy.WalletLedgerPolicy
import com.predata.backend.domain.policy.WalletOperation
import com.predata.backend.domain.policy.WalletPolicy
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.MemberWalletRepository
import com.predata.backend.repository.TreasuryLedgerRepository
import com.predata.backend.repository.WalletLedgerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class WalletBalanceService(
    private val memberRepository: MemberRepository,
    private val memberWalletRepository: MemberWalletRepository,
    private val walletLedgerRepository: WalletLedgerRepository,
    private val treasuryLedgerRepository: TreasuryLedgerRepository,
) {
    @Transactional
    fun lockForWithdrawal(
        memberId: Long,
        amount: BigDecimal,
        txType: String,
        referenceType: String? = null,
        referenceId: Long? = null,
        description: String? = null,
    ): WalletSnapshot {
        WalletPolicy.ensurePositive(amount, "Lock")

        val wallet = lockOrCreateWallet(memberId)
        WalletPolicy.ensureEnoughAvailable(wallet.availableBalance, amount)

        wallet.availableBalance = wallet.availableBalance.subtract(amount)
        wallet.lockedBalance = wallet.lockedBalance.add(amount)
        wallet.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        memberWalletRepository.save(wallet)
        syncMemberBalance(memberId, wallet.availableBalance)

        recordWalletLedger(
            walletId = wallet.id!!,
            memberId = memberId,
            direction = WalletLedgerPolicy.directionFor(WalletOperation.LOCK_WITHDRAWAL),
            txType = txType,
            amount = amount,
            balanceAfter = wallet.availableBalance,
            lockedBalanceAfter = wallet.lockedBalance,
            referenceType = referenceType,
            referenceId = referenceId,
            description = description,
        )

        return WalletSnapshot(wallet.availableBalance, wallet.lockedBalance)
    }

    @Transactional
    fun unlockWithdrawal(
        memberId: Long,
        amount: BigDecimal,
        txType: String,
        referenceType: String? = null,
        referenceId: Long? = null,
        description: String? = null,
    ): WalletSnapshot {
        WalletPolicy.ensurePositive(amount, "Unlock")

        val wallet = lockOrCreateWallet(memberId)
        WalletPolicy.ensureEnoughLocked(wallet.lockedBalance, amount)

        wallet.lockedBalance = wallet.lockedBalance.subtract(amount)
        wallet.availableBalance = wallet.availableBalance.add(amount)
        wallet.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        memberWalletRepository.save(wallet)
        syncMemberBalance(memberId, wallet.availableBalance)

        recordWalletLedger(
            walletId = wallet.id!!,
            memberId = memberId,
            direction = WalletLedgerPolicy.directionFor(WalletOperation.UNLOCK_WITHDRAWAL),
            txType = txType,
            amount = amount,
            balanceAfter = wallet.availableBalance,
            lockedBalanceAfter = wallet.lockedBalance,
            referenceType = referenceType,
            referenceId = referenceId,
            description = description,
        )

        return WalletSnapshot(wallet.availableBalance, wallet.lockedBalance)
    }

    @Transactional
    fun settleLockedWithdrawal(
        memberId: Long,
        amount: BigDecimal,
        txType: String,
        referenceType: String? = null,
        referenceId: Long? = null,
        description: String? = null,
    ): WalletSnapshot {
        WalletPolicy.ensurePositive(amount, "Settlement")

        val wallet = lockOrCreateWallet(memberId)
        WalletPolicy.ensureEnoughLocked(wallet.lockedBalance, amount)

        wallet.lockedBalance = wallet.lockedBalance.subtract(amount)
        wallet.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        memberWalletRepository.save(wallet)
        syncMemberBalance(memberId, wallet.availableBalance)

        recordWalletLedger(
            walletId = wallet.id!!,
            memberId = memberId,
            direction = WalletLedgerPolicy.directionFor(WalletOperation.SETTLE_LOCKED_WITHDRAWAL),
            txType = txType,
            amount = amount,
            balanceAfter = wallet.availableBalance,
            lockedBalanceAfter = wallet.lockedBalance,
            referenceType = referenceType,
            referenceId = referenceId,
            description = description,
        )

        return WalletSnapshot(wallet.availableBalance, wallet.lockedBalance)
    }

    @Transactional
    fun debit(
        memberId: Long,
        amount: BigDecimal,
        txType: String,
        referenceType: String? = null,
        referenceId: Long? = null,
        description: String? = null,
    ): WalletSnapshot {
        WalletPolicy.ensurePositive(amount, "Debit")

        val wallet = lockOrCreateWallet(memberId)
        WalletPolicy.ensureEnoughAvailable(wallet.availableBalance, amount)

        wallet.availableBalance = wallet.availableBalance.subtract(amount)
        wallet.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        memberWalletRepository.save(wallet)
        syncMemberBalance(memberId, wallet.availableBalance)

        recordWalletLedger(
            walletId = wallet.id!!,
            memberId = memberId,
            direction = WalletLedgerPolicy.directionFor(WalletOperation.DEBIT),
            txType = txType,
            amount = amount,
            balanceAfter = wallet.availableBalance,
            lockedBalanceAfter = wallet.lockedBalance,
            referenceType = referenceType,
            referenceId = referenceId,
            description = description,
        )

        return WalletSnapshot(wallet.availableBalance, wallet.lockedBalance)
    }

    @Transactional
    fun credit(
        memberId: Long,
        amount: BigDecimal,
        txType: String,
        referenceType: String? = null,
        referenceId: Long? = null,
        description: String? = null,
        treasuryInflow: Boolean = false,
    ): WalletSnapshot {
        WalletPolicy.ensurePositive(amount, "Credit")

        val wallet = lockOrCreateWallet(memberId)
        wallet.availableBalance = wallet.availableBalance.add(amount)
        wallet.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
        memberWalletRepository.save(wallet)
        syncMemberBalance(memberId, wallet.availableBalance)

        recordWalletLedger(
            walletId = wallet.id!!,
            memberId = memberId,
            direction = WalletLedgerPolicy.directionFor(WalletOperation.CREDIT),
            txType = txType,
            amount = amount,
            balanceAfter = wallet.availableBalance,
            lockedBalanceAfter = wallet.lockedBalance,
            referenceType = referenceType,
            referenceId = referenceId,
            description = description,
        )

        if (treasuryInflow) {
            recordTreasuryLedger(
                txType = txType,
                amount = amount,
                referenceType = referenceType,
                referenceId = referenceId,
                description = description ?: "Treasury inflow",
            )
        }

        return WalletSnapshot(wallet.availableBalance, wallet.lockedBalance)
    }

    @Transactional(readOnly = true)
    fun getAvailableBalance(memberId: Long): BigDecimal {
        val wallet = memberWalletRepository.findByMemberId(memberId)
        if (wallet != null) return wallet.availableBalance
        return memberRepository.findById(memberId).orElseThrow {
            IllegalArgumentException("Member not found.")
        }.usdcBalance
    }

    @Transactional
    fun recordTreasuryOutflow(
        amount: BigDecimal,
        txType: String,
        referenceType: String? = null,
        referenceId: Long? = null,
        description: String? = null,
    ) {
        if (amount <= BigDecimal.ZERO) return
        recordTreasuryLedger(
            txType = txType,
            amount = WalletLedgerPolicy.treasuryOutflowAmount(amount),
            referenceType = referenceType,
            referenceId = referenceId,
            description = description ?: "Treasury outflow",
        )
    }

    @Transactional
    fun recordTreasuryInflow(
        amount: BigDecimal,
        txType: String,
        referenceType: String? = null,
        referenceId: Long? = null,
        description: String? = null,
    ) {
        if (amount <= BigDecimal.ZERO) return
        recordTreasuryLedger(
            txType = txType,
            amount = WalletLedgerPolicy.treasuryInflowAmount(amount),
            referenceType = referenceType,
            referenceId = referenceId,
            description = description ?: "Treasury inflow",
        )
    }

    private fun lockOrCreateWallet(memberId: Long): MemberWallet {
        val locked = memberWalletRepository.findByMemberIdWithLock(memberId)
        if (locked != null) return locked

        val member = memberRepository.findById(memberId).orElseThrow {
            IllegalArgumentException("Member not found.")
        }
        return memberWalletRepository.save(
            MemberWallet(
                memberId = memberId,
                availableBalance = member.usdcBalance,
                lockedBalance = BigDecimal.ZERO,
            )
        )
    }

    private fun syncMemberBalance(memberId: Long, availableBalance: BigDecimal) {
        val member = memberRepository.findById(memberId).orElseThrow {
            IllegalArgumentException("Member not found.")
        }
        member.usdcBalance = availableBalance
        memberRepository.save(member)
    }

    private fun recordWalletLedger(
        walletId: Long,
        memberId: Long,
        direction: WalletLedgerDirection,
        txType: String,
        amount: BigDecimal,
        balanceAfter: BigDecimal,
        lockedBalanceAfter: BigDecimal,
        referenceType: String?,
        referenceId: Long?,
        description: String?,
    ) {
        walletLedgerRepository.save(
            WalletLedger(
                walletId = walletId,
                memberId = memberId,
                direction = direction,
                txType = txType,
                amount = amount,
                balanceAfter = balanceAfter,
                lockedBalanceAfter = lockedBalanceAfter,
                referenceType = referenceType,
                referenceId = referenceId,
                description = description,
            )
        )
    }

    private fun recordTreasuryLedger(
        txType: String,
        amount: BigDecimal,
        referenceType: String?,
        referenceId: Long?,
        description: String,
    ) {
        treasuryLedgerRepository.save(
            TreasuryLedger(
                txType = txType,
                amount = amount,
                referenceType = referenceType,
                referenceId = referenceId,
                description = description,
            )
        )
    }
}

data class WalletSnapshot(
    val availableBalance: BigDecimal,
    val lockedBalance: BigDecimal,
)
