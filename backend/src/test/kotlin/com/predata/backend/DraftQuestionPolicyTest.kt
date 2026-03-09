package com.predata.backend

import com.predata.backend.domain.VoteWindowType
import com.predata.backend.domain.policy.DraftQuestionPolicy
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals

class DraftQuestionPolicyTest {
    @Test
    fun `requiredCredits follows vote window policy`() {
        assertEquals(1, DraftQuestionPolicy.requiredCredits(VoteWindowType.H6))
        assertEquals(1, DraftQuestionPolicy.requiredCredits(VoteWindowType.D1))
        assertEquals(3, DraftQuestionPolicy.requiredCredits(VoteWindowType.D3))
    }

    @Test
    fun `maxBettingDuration follows fixed policy`() {
        assertEquals(Duration.ofDays(1), DraftQuestionPolicy.maxBettingDuration(VoteWindowType.H6))
        assertEquals(Duration.ofDays(3), DraftQuestionPolicy.maxBettingDuration(VoteWindowType.D1))
        assertEquals(Duration.ofDays(7), DraftQuestionPolicy.maxBettingDuration(VoteWindowType.D3))
    }

    @Test
    fun `feeSplit keeps platform twenty percent and splits distributable pool`() {
        val split = DraftQuestionPolicy.feeSplit(60)
        assertEquals("0.2000", split.platformFeeShare.toPlainString())
        assertEquals("0.4800", split.creatorFeeShare.toPlainString())
        assertEquals("0.3200", split.voterFeeShare.toPlainString())
    }
}
