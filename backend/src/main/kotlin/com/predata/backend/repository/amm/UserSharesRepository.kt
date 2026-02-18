package com.predata.backend.repository.amm

import com.predata.backend.domain.ShareOutcome
import com.predata.backend.domain.UserShares
import com.predata.backend.domain.UserSharesId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserSharesRepository : JpaRepository<UserShares, UserSharesId> {

    fun findByMemberIdAndQuestionId(memberId: Long, questionId: Long): List<UserShares>

    fun findByMemberId(memberId: Long): List<UserShares>

    fun findByQuestionId(questionId: Long): List<UserShares>

    @Query("""
        SELECT us FROM UserShares us
        WHERE us.questionId = :questionId
        AND us.outcome = :outcome
        ORDER BY us.shares DESC
    """)
    fun findTopHoldersByQuestionAndOutcome(questionId: Long, outcome: ShareOutcome): List<UserShares>
}
