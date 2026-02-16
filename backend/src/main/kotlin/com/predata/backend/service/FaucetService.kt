package com.predata.backend.service

import com.predata.backend.domain.DailyFaucet
import com.predata.backend.repository.DailyFaucetRepository
import com.predata.backend.repository.MemberRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class FaucetService(
    private val dailyFaucetRepository: DailyFaucetRepository,
    private val memberRepository: MemberRepository,
    private val transactionHistoryService: TransactionHistoryService
) {

    companion object {
        const val DAILY_FAUCET_AMOUNT = 100L
    }

    /**
     * 일일 포인트 수령
     */
    @Transactional
    fun claimDailyPoints(memberId: Long): FaucetClaimResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다.") }

        val today = LocalDate.now()
        val faucet = try {
            dailyFaucetRepository.findByMemberIdAndResetDate(memberId, today)
                .orElseGet {
                    dailyFaucetRepository.save(
                        DailyFaucet(
                            memberId = memberId,
                            claimed = false,
                            amount = DAILY_FAUCET_AMOUNT,
                            resetDate = today
                        )
                    )
                }
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청으로 인한 중복 생성 시도 → 기존 레코드 조회
            dailyFaucetRepository.findByMemberIdAndResetDate(memberId, today)
                .orElseThrow { IllegalStateException("Faucet 레코드 조회 실패") }
        }

        if (faucet.claimed) {
            return FaucetClaimResponse(
                success = false,
                amount = 0,
                newBalance = member.usdcBalance.toLong(),
                message = "오늘의 포인트를 이미 수령하셨습니다."
            )
        }

        faucet.claimed = true
        dailyFaucetRepository.save(faucet)

        member.usdcBalance = member.usdcBalance.add(BigDecimal(DAILY_FAUCET_AMOUNT))
        memberRepository.save(member)

        transactionHistoryService.record(
            memberId = memberId,
            type = "DEPOSIT",
            amount = BigDecimal(DAILY_FAUCET_AMOUNT),
            balanceAfter = member.usdcBalance,
            description = "일일 보상 수령"
        )

        return FaucetClaimResponse(
            success = true,
            amount = DAILY_FAUCET_AMOUNT,
            newBalance = member.usdcBalance.toLong(),
            message = "${DAILY_FAUCET_AMOUNT}P 일일 보상을 받았습니다!"
        )
    }

    /**
     * 오늘 수령 상태 조회
     */
    @Transactional(readOnly = true)
    fun getFaucetStatus(memberId: Long): FaucetStatusResponse {
        val today = LocalDate.now()
        val faucet = dailyFaucetRepository.findByMemberIdAndResetDate(memberId, today)

        return FaucetStatusResponse(
            claimed = faucet.map { it.claimed }.orElse(false),
            amount = DAILY_FAUCET_AMOUNT,
            resetDate = today.toString()
        )
    }
}

data class FaucetClaimResponse(
    val success: Boolean,
    val amount: Long,
    val newBalance: Long,
    val message: String
)

data class FaucetStatusResponse(
    val claimed: Boolean,
    val amount: Long,
    val resetDate: String
)
