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
            throw IllegalArgumentException("계정이 정지되었습니다.")
        }

        if (member.hasVotingPass) {
            throw IllegalArgumentException("이미 투표 패스를 보유하고 있습니다.")
        }

        if (member.usdcBalance < VOTING_PASS_PRICE) {
            throw IllegalArgumentException("잔액이 부족합니다. (필요: \$${VOTING_PASS_PRICE}, 보유: \$${member.usdcBalance})")
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
            message = "투표 패스를 구매했습니다."
        )
    }
}

data class VotingPassPurchaseResponse(
    val success: Boolean,
    val hasVotingPass: Boolean,
    val remainingBalance: Double,
    val message: String
)
