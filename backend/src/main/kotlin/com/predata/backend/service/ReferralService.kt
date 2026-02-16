package com.predata.backend.service

import com.predata.backend.domain.Referral
import com.predata.backend.exception.ConflictException
import com.predata.backend.exception.NotFoundException
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.ReferralRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ReferralService(
    private val referralRepository: ReferralRepository,
    private val memberRepository: MemberRepository,
    private val notificationService: NotificationService,
    private val transactionHistoryService: TransactionHistoryService
) {
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private lateinit var badgeService: BadgeService

    private val logger = LoggerFactory.getLogger(ReferralService::class.java)

    companion object {
        private const val REFERRER_REWARD = 500L
        private const val REFEREE_REWARD = 500L
        private const val CODE_PREFIX = "PRD"
        private const val CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private const val CODE_RANDOM_LENGTH = 5
        private const val MAX_REFERRALS = 50
    }

    /**
     * 회원에게 고유 레퍼럴 코드를 생성한다.
     */
    @Transactional
    fun generateReferralCode(memberId: Long): String {
        val member = memberRepository.findById(memberId)
            .orElseThrow { NotFoundException("회원을 찾을 수 없습니다.") }

        if (member.referralCode != null) {
            return member.referralCode!!
        }

        var code: String
        var attempts = 0
        do {
            code = CODE_PREFIX + (1..CODE_RANDOM_LENGTH)
                .map { CODE_CHARS.random() }
                .joinToString("")
            attempts++
        } while (memberRepository.findByReferralCode(code).isPresent && attempts < 10)

        member.referralCode = code
        memberRepository.save(member)
        return code
    }

    /**
     * 레퍼럴 코드를 적용한다 (가입 시 호출).
     */
    @Transactional
    fun applyReferral(refereeId: Long, referralCode: String, refereeIp: String?): ReferralResult {
        // 1. 레퍼럴 코드로 추천인 찾기
        val referrer = memberRepository.findByReferralCode(referralCode.uppercase().trim())
            .orElse(null) ?: return ReferralResult(false, "유효하지 않은 레퍼럴 코드입니다.")

        val referee = memberRepository.findById(refereeId)
            .orElseThrow { NotFoundException("회원을 찾을 수 없습니다.") }

        // 2. 자기 참조 방지
        if (referrer.id == refereeId) {
            return ReferralResult(false, "자기 자신의 코드는 사용할 수 없습니다.")
        }

        // 3. 이미 추천받은 유저 체크
        if (referralRepository.existsByRefereeId(refereeId)) {
            return ReferralResult(false, "이미 추천을 받은 계정입니다.")
        }

        // 4. 동일 IP 방지
        if (refereeIp != null && referrer.signupIp == refereeIp) {
            logger.warn("Same IP referral attempt blocked: referrer=${referrer.id}, referee=$refereeId, ip=$refereeIp")
            return ReferralResult(false, "동일 네트워크에서의 추천은 제한됩니다.")
        }

        // 5. 추천인 밴 체크
        if (referrer.isBanned) {
            return ReferralResult(false, "유효하지 않은 레퍼럴 코드입니다.")
        }

        // 6. 최대 추천 수 제한
        if (referralRepository.countByReferrerId(referrer.id!!) >= MAX_REFERRALS) {
            return ReferralResult(false, "추천인의 최대 추천 횟수를 초과했습니다.")
        }

        // 7. 레퍼럴 기록 저장
        val referral = Referral(
            referrerId = referrer.id!!,
            refereeId = refereeId,
            referralCode = referralCode.uppercase().trim(),
            referrerReward = REFERRER_REWARD,
            refereeReward = REFEREE_REWARD
        )
        referralRepository.save(referral)

        // 8. USDC 보상 지급
        referrer.usdcBalance = referrer.usdcBalance.add(BigDecimal(REFERRER_REWARD))
        memberRepository.save(referrer)

        transactionHistoryService.record(
            memberId = referrer.id!!,
            type = "DEPOSIT",
            amount = BigDecimal(REFERRER_REWARD),
            balanceAfter = referrer.usdcBalance,
            description = "추천 보상 (추천인)"
        )

        referee.referredBy = referrer.id
        referee.usdcBalance = referee.usdcBalance.add(BigDecimal(REFEREE_REWARD))
        memberRepository.save(referee)

        transactionHistoryService.record(
            memberId = refereeId,
            type = "DEPOSIT",
            amount = BigDecimal(REFEREE_REWARD),
            balanceAfter = referee.usdcBalance,
            description = "추천 보상 (피추천인)"
        )

        // 9. 알림 발송
        notificationService.createNotification(
            memberId = referrer.id!!,
            type = "REFERRAL_REWARD",
            title = "새로운 추천 보상",
            message = "새 회원이 가입했습니다! +${REFERRER_REWARD}P 보상이 지급되었습니다."
        )
        notificationService.createNotification(
            memberId = refereeId,
            type = "REFERRAL_REWARD",
            title = "추천 가입 보상",
            message = "추천 코드로 가입 보상 +${REFEREE_REWARD}P가 지급되었습니다."
        )

        logger.info("Referral applied: referrer=${referrer.id}, referee=$refereeId, code=$referralCode")

        // 뱃지 확인
        badgeService.onReferral(referrer.id!!)

        return ReferralResult(
            success = true,
            message = "추천이 완료되었습니다!",
            referrerReward = REFERRER_REWARD,
            refereeReward = REFEREE_REWARD
        )
    }

    /**
     * 내 레퍼럴 통계를 조회한다.
     */
    @Transactional(readOnly = true)
    fun getReferralStats(memberId: Long): ReferralStatsResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { NotFoundException("회원을 찾을 수 없습니다.") }

        val code = member.referralCode ?: generateReferralCode(memberId)
        val referrals = referralRepository.findByReferrerId(memberId)
        val totalPointsEarned = referrals.sumOf { it.referrerReward }

        val referees = referrals.map { ref ->
            val referee = memberRepository.findById(ref.refereeId).orElse(null)
            RefereeInfo(
                email = maskEmail(referee?.email ?: "unknown"),
                joinedAt = ref.createdAt.toString(),
                rewardAmount = ref.referrerReward
            )
        }

        return ReferralStatsResponse(
            referralCode = code,
            totalReferrals = referrals.size.toLong(),
            totalPointsEarned = totalPointsEarned,
            referees = referees
        )
    }

    /**
     * 내 레퍼럴 코드만 조회한다.
     */
    @Transactional
    fun getReferralCode(memberId: Long): String {
        val member = memberRepository.findById(memberId)
            .orElseThrow { NotFoundException("회원을 찾을 수 없습니다.") }
        return member.referralCode ?: generateReferralCode(memberId)
    }

    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 2) return "***${email.substring(atIndex)}"
        return "${email.substring(0, 2)}***${email.substring(atIndex)}"
    }
}

data class ReferralResult(
    val success: Boolean,
    val message: String,
    val referrerReward: Long = 0,
    val refereeReward: Long = 0
)

data class ReferralStatsResponse(
    val referralCode: String,
    val totalReferrals: Long,
    val totalPointsEarned: Long,
    val referees: List<RefereeInfo>
)

data class RefereeInfo(
    val email: String,
    val joinedAt: String,
    val rewardAmount: Long
)
