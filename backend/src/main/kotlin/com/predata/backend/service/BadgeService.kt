package com.predata.backend.service

import com.predata.backend.domain.ActivityType
import com.predata.backend.domain.Choice
import com.predata.backend.domain.FinalResult
import com.predata.backend.domain.MemberBadge
import com.predata.backend.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BadgeService(
    private val memberBadgeRepository: MemberBadgeRepository,
    private val badgeDefinitionRepository: BadgeDefinitionRepository,
    private val memberRepository: MemberRepository,
    private val activityRepository: ActivityRepository,
    private val referralRepository: ReferralRepository,
    private val questionRepository: QuestionRepository,
    private val notificationService: NotificationService
) {
    private val logger = LoggerFactory.getLogger(BadgeService::class.java)

    // ===== Core check-and-award logic =====

    /**
     * Finds or creates a MemberBadge for the given (memberId, badgeId).
     * Updates progress and awards if threshold is met.
     */
    @Transactional
    private fun checkAndAward(memberId: Long, badgeId: String, currentProgress: Int, target: Int) {
        val memberBadge = memberBadgeRepository.findByMemberIdAndBadgeId(memberId, badgeId)
            .orElseGet {
                memberBadgeRepository.save(
                    MemberBadge(
                        memberId = memberId,
                        badgeId = badgeId,
                        progress = 0,
                        target = target
                    )
                )
            }

        memberBadge.progress = currentProgress

        if (memberBadge.progress >= target && memberBadge.awardedAt == null) {
            memberBadge.awardedAt = LocalDateTime.now()
            logger.info("[Badge] Awarded badge=$badgeId to member=$memberId (progress=$currentProgress/$target)")

            // Look up badge name for the notification
            val badgeDef = badgeDefinitionRepository.findById(badgeId).orElse(null)
            val badgeName = badgeDef?.name ?: badgeId

            notificationService.createNotification(
                memberId = memberId,
                type = "BADGE_EARNED",
                title = "New Badge Earned!",
                message = "You earned the \"$badgeName\" badge!"
            )
        }

        memberBadgeRepository.save(memberBadge)
    }

    // ===== Event Handlers =====

    /**
     * Called after a bet is placed.
     */
    @Transactional
    fun onBetPlaced(memberId: Long) {
        try {
            val totalBets = activityRepository.findByMemberIdAndActivityType(memberId, ActivityType.BET).size

            checkAndAward(memberId, "FIRST_BET", totalBets, 1)
            checkAndAward(memberId, "BET_10", totalBets, 10)
            checkAndAward(memberId, "BET_50", totalBets, 50)
            checkAndAward(memberId, "BET_100", totalBets, 100)

            // Category diversity: count distinct categories from bets
            val bets = activityRepository.findByMemberIdAndActivityType(memberId, ActivityType.BET)
            val distinctCategories = bets.mapNotNull { bet ->
                questionRepository.findById(bet.questionId).orElse(null)?.category
            }.distinct()

            val commonCategories = setOf("ECONOMY", "SPORTS", "POLITICS", "TECH", "CULTURE")
            if (commonCategories.all { it in distinctCategories }) {
                checkAndAward(memberId, "CATEGORY_ALL", 1, 1)
            }
        } catch (e: Exception) {
            logger.error("[Badge] Error in onBetPlaced for member=$memberId", e)
        }
    }

    /**
     * Called after a question is settled for each bettor.
     */
    @Transactional
    fun onSettlement(memberId: Long, isWinner: Boolean, payoutRatio: Double) {
        try {
            val member = memberRepository.findById(memberId).orElse(null) ?: return

            if (isWinner) {
                // First win badge
                checkAndAward(memberId, "FIRST_WIN", member.correctPredictions, 1)

                // Big win badge (payout ratio >= 5x)
                if (payoutRatio >= 5.0) {
                    checkAndAward(memberId, "BIG_WIN", 1, 1)
                }
            }

            // Win streak calculation: query recent settled bets and count consecutive wins
            val allBets = activityRepository.findByMemberIdAndActivityType(memberId, ActivityType.BET)
            val settledBets = allBets
                .mapNotNull { bet ->
                    val q = questionRepository.findById(bet.questionId).orElse(null)
                    if (q != null && q.status == "SETTLED") {
                        val winningChoice = if (q.finalResult == FinalResult.YES) Choice.YES else Choice.NO
                        Pair(bet.createdAt, bet.choice == winningChoice)
                    } else null
                }
                .sortedByDescending { it.first }

            // Count current win streak from most recent
            var winStreak = 0
            for ((_, won) in settledBets) {
                if (won) winStreak++ else break
            }

            checkAndAward(memberId, "WIN_STREAK_3", winStreak, 3)
            checkAndAward(memberId, "WIN_STREAK_5", winStreak, 5)
            checkAndAward(memberId, "WIN_STREAK_10", winStreak, 10)

            // Accuracy badges
            val totalPredictions = member.totalPredictions
            val correctPredictions = member.correctPredictions
            if (totalPredictions > 0) {
                val accuracy = correctPredictions.toDouble() / totalPredictions.toDouble()

                if (totalPredictions >= 10 && accuracy >= 0.6) {
                    checkAndAward(memberId, "ACCURACY_60", 1, 1)
                }
                if (totalPredictions >= 20 && accuracy >= 0.75) {
                    checkAndAward(memberId, "ACCURACY_75", 1, 1)
                }
                if (totalPredictions >= 30 && accuracy >= 0.9) {
                    checkAndAward(memberId, "ACCURACY_90", 1, 1)
                }
            }
        } catch (e: Exception) {
            logger.error("[Badge] Error in onSettlement for member=$memberId", e)
        }
    }

    /**
     * Called after a tier change.
     */
    @Transactional
    fun onTierChange(memberId: Long, newTier: String) {
        try {
            when (newTier.uppercase()) {
                "SILVER" -> checkAndAward(memberId, "TIER_SILVER", 1, 1)
                "GOLD" -> checkAndAward(memberId, "TIER_GOLD", 1, 1)
                "PLATINUM" -> checkAndAward(memberId, "TIER_PLATINUM", 1, 1)
            }
        } catch (e: Exception) {
            logger.error("[Badge] Error in onTierChange for member=$memberId", e)
        }
    }

    /**
     * Called when point balance changes.
     */
    @Transactional
    fun onPointsChange(memberId: Long, newBalance: Long) {
        try {
            if (newBalance >= 10_000) {
                checkAndAward(memberId, "POINTS_10K", 1, 1)
            }
            if (newBalance >= 100_000) {
                checkAndAward(memberId, "POINTS_100K", 1, 1)
            }
        } catch (e: Exception) {
            logger.error("[Badge] Error in onPointsChange for member=$memberId", e)
        }
    }

    /**
     * Called after a referral is applied.
     */
    @Transactional
    fun onReferral(referrerId: Long) {
        try {
            val totalReferrals = referralRepository.countByReferrerId(referrerId).toInt()
            checkAndAward(referrerId, "REFERRAL_1", totalReferrals, 1)
            checkAndAward(referrerId, "REFERRAL_5", totalReferrals, 5)
            checkAndAward(referrerId, "REFERRAL_20", totalReferrals, 20)
        } catch (e: Exception) {
            logger.error("[Badge] Error in onReferral for referrer=$referrerId", e)
        }
    }

    /**
     * Called after a faucet claim.
     * Uses incremental progress approach since we don't have a direct count query.
     */
    @Transactional
    fun onFaucetClaim(memberId: Long) {
        try {
            // Increment faucet claim count by reading current progress and adding 1
            val currentBadge7 = memberBadgeRepository.findByMemberIdAndBadgeId(memberId, "FAUCET_7")
            val currentProgress = currentBadge7.map { it.progress }.orElse(0) + 1

            checkAndAward(memberId, "FAUCET_7", currentProgress, 7)
            checkAndAward(memberId, "FAUCET_30", currentProgress, 30)
        } catch (e: Exception) {
            logger.error("[Badge] Error in onFaucetClaim for member=$memberId", e)
        }
    }

    // ===== Read Methods =====

    /**
     * Returns all badge definitions merged with the member's progress.
     */
    @Transactional(readOnly = true)
    fun getMemberBadges(memberId: Long): List<BadgeWithProgressResponse> {
        val definitions = badgeDefinitionRepository.findAllByOrderBySortOrderAsc()
        val memberBadges = memberBadgeRepository.findByMemberId(memberId)
        val memberBadgeMap = memberBadges.associateBy { it.badgeId }

        return definitions.map { def ->
            val mb = memberBadgeMap[def.id]
            BadgeWithProgressResponse(
                badge = BadgeDefinitionDto(
                    id = def.id,
                    name = def.name,
                    description = def.description,
                    category = def.category,
                    rarity = def.rarity,
                    iconUrl = def.iconUrl,
                    sortOrder = def.sortOrder
                ),
                progress = mb?.progress ?: 0,
                target = mb?.target ?: getDefaultTarget(def.id),
                earned = mb?.awardedAt != null,
                awardedAt = mb?.awardedAt?.toString()
            )
        }
    }

    /**
     * Returns only badges that the member has earned.
     */
    @Transactional(readOnly = true)
    fun getEarnedBadges(memberId: Long): List<BadgeWithProgressResponse> {
        return getMemberBadges(memberId).filter { it.earned }
    }

    /**
     * Default target values for badges (used when member hasn't started progress yet).
     */
    private fun getDefaultTarget(badgeId: String): Int {
        return when (badgeId) {
            "FIRST_BET" -> 1
            "BET_10" -> 10
            "BET_50" -> 50
            "BET_100" -> 100
            "FIRST_WIN" -> 1
            "BIG_WIN" -> 1
            "WIN_STREAK_3" -> 3
            "WIN_STREAK_5" -> 5
            "WIN_STREAK_10" -> 10
            "ACCURACY_60" -> 1
            "ACCURACY_75" -> 1
            "ACCURACY_90" -> 1
            "TIER_SILVER" -> 1
            "TIER_GOLD" -> 1
            "TIER_PLATINUM" -> 1
            "POINTS_10K" -> 1
            "POINTS_100K" -> 1
            "REFERRAL_1" -> 1
            "REFERRAL_5" -> 5
            "REFERRAL_20" -> 20
            "FAUCET_7" -> 7
            "FAUCET_30" -> 30
            "CATEGORY_ALL" -> 1
            else -> 1
        }
    }
}

// ===== DTOs =====

data class BadgeWithProgressResponse(
    val badge: BadgeDefinitionDto,
    val progress: Int,
    val target: Int,
    val earned: Boolean,
    val awardedAt: String?
)

data class BadgeDefinitionDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val rarity: String,
    val iconUrl: String?,
    val sortOrder: Int
)
