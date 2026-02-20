package com.predata.backend.service.bot

import com.predata.backend.domain.Choice
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.SwapAction
import com.predata.backend.domain.VotingPhase
import com.predata.backend.dto.VoteCommitRequest
import com.predata.backend.dto.VoteRevealRequest
import com.predata.backend.dto.amm.SwapRequest
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.VoteCommitService
import com.predata.backend.service.amm.SwapService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class BotTradingService(
    private val botMemberService: BotMemberService,
    private val voteCommitService: VoteCommitService,
    private val swapService: SwapService,
    private val questionRepository: QuestionRepository
) {
    private val logger = LoggerFactory.getLogger(BotTradingService::class.java)
    private val botSaltStore = ConcurrentHashMap<String, Pair<Choice, String>>()

    @Transactional
    fun executeBotVoting() {
        val bots = botMemberService.getBotMembers()
        if (bots.isEmpty()) return

        val votingQuestions = questionRepository.findByStatus(QuestionStatus.VOTING)

        for (question in votingQuestions) {
            val questionId = question.id ?: continue

            when (question.votingPhase) {
                VotingPhase.VOTING_COMMIT_OPEN -> {
                    val voterCount = (5..15).random()
                    val selectedBots = bots.shuffled().take(voterCount)

                    for (bot in selectedBots) {
                        try {
                            val botId = bot.id ?: continue
                            val choice = if ((0..1).random() == 0) Choice.YES else Choice.NO
                            val salt = UUID.randomUUID().toString()
                            val commitHash = generateCommitHash(questionId, botId, choice, salt)

                            botSaltStore["${questionId}_${botId}"] = Pair(choice, salt)

                            voteCommitService.commit(
                                botId,
                                VoteCommitRequest(
                                    questionId = questionId,
                                    commitHash = commitHash
                                )
                            )
                        } catch (e: Exception) {
                            logger.debug("Bot vote commit failed: botId=${bot.id}, questionId=$questionId, error=${e.message}")
                        }
                    }
                }

                VotingPhase.VOTING_REVEAL_OPEN -> {
                    for (bot in bots) {
                        val botId = bot.id ?: continue
                        val key = "${questionId}_${botId}"
                        val stored = botSaltStore[key] ?: continue

                        try {
                            voteCommitService.reveal(
                                botId,
                                VoteRevealRequest(
                                    questionId = questionId,
                                    choice = stored.first,
                                    salt = stored.second
                                )
                            )
                            botSaltStore.remove(key)
                        } catch (e: Exception) {
                            logger.debug("Bot vote reveal failed: botId=${bot.id}, questionId=$questionId, error=${e.message}")
                        }
                    }
                }

                else -> {
                    // skip
                }
            }
        }
    }

    @Transactional
    fun executeBotTrading() {
        val bots = botMemberService.getBotMembers()
        if (bots.isEmpty()) return

        val bettingQuestions = questionRepository.findByStatus(QuestionStatus.BETTING)

        for (question in bettingQuestions) {
            val questionId = question.id ?: continue
            val traderCount = (3..8).random()
            val selectedBots = bots.shuffled().take(traderCount)

            for (bot in selectedBots) {
                try {
                    val botId = bot.id ?: continue
                    val outcome = if ((0..1).random() == 0) ShareOutcome.YES else ShareOutcome.NO
                    val amount = BigDecimal((10..500).random())

                    swapService.executeSwap(
                        botId,
                        SwapRequest(
                            questionId = questionId,
                            action = SwapAction.BUY,
                            outcome = outcome,
                            usdcIn = amount
                        )
                    )

                    logger.debug("Bot trade: botId=${bot.id}, questionId=$questionId, $outcome, $amount USDC")
                } catch (e: Exception) {
                    logger.debug("Bot trade failed: botId=${bot.id}, questionId=$questionId, error=${e.message}")
                }
            }
        }
    }

    private fun generateCommitHash(questionId: Long, memberId: Long, choice: Choice, salt: String): String {
        val input = "$questionId:$memberId:${choice.name}:$salt"
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
