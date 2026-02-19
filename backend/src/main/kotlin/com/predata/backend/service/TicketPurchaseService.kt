package com.predata.backend.service

import com.predata.backend.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class VotingPassService(
    private val memberRepository: MemberRepository,
    private val transactionHistoryService: TransactionHistoryService
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

        if (member.usdcBalance < VOTING_PASS_PRICE) {
            throw IllegalArgumentException("Insufficient balance. (required: \$${VOTING_PASS_PRICE}, balance: \$${member.usdcBalance})")
        }

        member.usdcBalance = member.usdcBalance.subtract(VOTING_PASS_PRICE)
        member.hasVotingPass = true
        memberRepository.save(member)

        transactionHistoryService.record(
            memberId = memberId,
            type = "VOTING_PASS",
            amount = VOTING_PASS_PRICE.negate(),
            balanceAfter = member.usdcBalance,
            description = "투표 패스 구매"
        )

        return VotingPassPurchaseResponse(
            success = true,
            hasVotingPass = true,
            remainingBalance = member.usdcBalance.toDouble(),
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
