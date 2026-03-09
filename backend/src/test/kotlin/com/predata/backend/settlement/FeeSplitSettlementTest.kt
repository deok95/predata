package com.predata.backend.settlement

import com.predata.backend.domain.Member
import com.predata.backend.domain.Question
import com.predata.backend.domain.QuestionStatus
import com.predata.backend.repository.MemberRepository
import com.predata.backend.repository.QuestionRepository
import com.predata.backend.service.FeePoolService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class FeeSplitSettlementTest {

    @Autowired
    lateinit var feePoolService: FeePoolService

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Test
    fun `fee pool uses question fee share and creator share distribution works`() {
        val creator = memberRepository.save(
            Member(
                email = "fee-split-${UUID.randomUUID()}@test.com",
                countryCode = "KR",
                role = "USER",
            )
        )
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val question = questionRepository.save(
            Question(
                title = "수수료 분배 정산 테스트",
                category = "GENERAL",
                status = QuestionStatus.BETTING,
                creatorMemberId = creator.id,
                platformFeeShare = BigDecimal("0.2000"),
                creatorFeeShare = BigDecimal("0.2400"),
                voterFeeShare = BigDecimal("0.5600"),
                creatorSplitInPool = 30,
                votingEndAt = now.minusHours(1),
                bettingStartAt = now.minusMinutes(55),
                bettingEndAt = now.plusDays(1),
                expiredAt = now.plusDays(1),
            )
        )
        val questionId = question.id!!

        feePoolService.collectFee(questionId, BigDecimal("10.000000"))
        val summary = feePoolService.getPoolSummary(questionId)

        assertThat(summary["platformShare"] as BigDecimal).isEqualByComparingTo("2.000000")
        assertThat(summary["creatorShare"] as BigDecimal).isEqualByComparingTo("2.400000")
        assertThat(summary["rewardPoolShare"] as BigDecimal).isEqualByComparingTo("5.600000")

        val distributed = feePoolService.distributeCreatorShare(questionId)
        assertThat(distributed).isEqualByComparingTo("2.400000")

        val summaryAfter = feePoolService.getPoolSummary(questionId)
        assertThat(summaryAfter["creatorShare"] as BigDecimal).isEqualByComparingTo("0.000000")
    }
}
