package com.predata.backend.service

import com.predata.backend.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class VotingPassService(
    private val memberRepository: MemberRepository,
    private val transactionHistoryService: TransactionHistoryService,
    private val walletBalanceService: WalletBalanceService,
) {

    companion object {
        val VOTING_PASS_PRICE: BigDecimal = BigDecimal("10") // $10
    }

    @Transactional
    fun purchaseVotingPass(memberId: Long): VotingPassPurchaseResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }

        if (member.isBanned) {
            throw IllegalArgumentException("Account has been suspended.")
        }

        if (member.hasVotingPass) {
            throw IllegalArgumentException("Already have voting pass.")
        }

        val availableBalance = walletBalanceService.getAvailableBalance(memberId)
        if (availableBalance < VOTING_PASS_PRICE) {
            throw IllegalArgumentException("Insufficient balance. (required: \$${VOTING_PASS_PRICE}, balance: \$$availableBalance)")
        }

        val wallet = walletBalanceService.debit(
            memberId = memberId,
            amount = VOTING_PASS_PRICE,
            txType = "VOTING_PASS",
            referenceType = "MEMBER",
            referenceId = memberId,
            description = "투표 패스 구매",
        )
        member.hasVotingPass = true
        memberRepository.save(member)

        transactionHistoryService.record(
            memberId = memberId,
            type = "VOTING_PASS",
            amount = VOTING_PASS_PRICE.negate(),
            balanceAfter = wallet.availableBalance,
            description = "투표 패스 구매"
        )

        return VotingPassPurchaseResponse(
            success = true,
            hasVotingPass = true,
            remainingBalance = wallet.availableBalance.toDouble(),
            message = "Voting pass purchased successfully."
        )
    }
}

data class VotingPassPurchaseResponse(
    val success: Boolean,
    val hasVotingPass: Boolean,
    val remainingBalance: Double,
    val message: String
)
