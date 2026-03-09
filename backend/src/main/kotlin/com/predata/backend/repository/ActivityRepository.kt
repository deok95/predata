package com.predata.backend.repository

import com.predata.backend.domain.Activity
import com.predata.backend.domain.ActivityType
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import com.predata.backend.domain.VoteWindowType
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.*

@Repository
interface ActivityRepository : JpaRepository<Activity, Long> {
    
    fun existsByMemberIdAndQuestionIdAndActivityType(
        memberId: Long,
        questionId: Long,
        activityType: ActivityType
    ): Boolean
    
    fun findByMemberId(memberId: Long): List<Activity>
    
    fun findByQuestionId(questionId: Long): List<Activity>
    
    fun findByQuestionIdAndActivityType(
        questionId: Long,
        activityType: ActivityType
    ): List<Activity>
    
    fun findByMemberIdAndActivityType(
        memberId: Long,
        activityType: ActivityType
    ): List<Activity>

    fun findByMemberIdAndQuestionId(
        memberId: Long,
        questionId: Long
    ): List<Activity>

    fun findByIpAddress(ipAddress: String): List<Activity>

    fun findByParentBetIdAndActivityType(
        parentBetId: Long,
        activityType: ActivityType
    ): Activity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Activity a WHERE a.id = :id")
    fun findByIdWithLock(id: Long): Optional<Activity>

    @Query(
        """
        SELECT q.id AS questionId,
               q.title AS title,
               q.category AS category,
               SUM(CASE WHEN a.choice = com.predata.backend.domain.Choice.YES THEN 1 ELSE 0 END) AS yesVotes,
               SUM(CASE WHEN a.choice = com.predata.backend.domain.Choice.NO THEN 1 ELSE 0 END) AS noVotes,
               COUNT(a.id) AS totalVotes,
               MAX(a.createdAt) AS lastVoteAt
        FROM Activity a
        JOIN Question q ON q.id = a.questionId
        WHERE a.activityType = com.predata.backend.domain.ActivityType.VOTE
          AND a.createdAt >= :since
          AND (:category IS NULL OR q.category = :category)
          AND (:voteWindowType IS NULL OR q.voteWindowType = :voteWindowType)
          AND q.status = com.predata.backend.domain.QuestionStatus.VOTING
        GROUP BY q.id, q.title, q.category
        ORDER BY COUNT(a.id) DESC, MAX(a.createdAt) DESC
        """
    )
    fun findTopVotedQuestionsSince(
        @Param("since") since: LocalDateTime,
        @Param("category") category: String?,
        @Param("voteWindowType") voteWindowType: VoteWindowType?,
        pageable: Pageable,
    ): List<TopVotedQuestionProjection>

    interface TopVotedQuestionProjection {
        fun getQuestionId(): Long
        fun getTitle(): String
        fun getCategory(): String?
        fun getYesVotes(): Long
        fun getNoVotes(): Long
        fun getTotalVotes(): Long
        fun getLastVoteAt(): LocalDateTime
    }

    @Query(
        """
        SELECT a.questionId AS questionId, COUNT(a.id) AS voteCount
        FROM Activity a
        WHERE a.activityType = com.predata.backend.domain.ActivityType.VOTE
          AND a.questionId IN :questionIds
        GROUP BY a.questionId
        """
    )
    fun countVotesByQuestionIds(
        @Param("questionIds") questionIds: Collection<Long>,
    ): List<QuestionVoteCountProjection>

    interface QuestionVoteCountProjection {
        fun getQuestionId(): Long
        fun getVoteCount(): Long
    }
}
