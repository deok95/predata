package com.predata.backend.repository

import com.predata.backend.domain.QuestionCreditAccount
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface QuestionCreditAccountRepository : JpaRepository<QuestionCreditAccount, Long> {

    fun findByMemberId(memberId: Long): QuestionCreditAccount?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT a FROM QuestionCreditAccount a WHERE a.memberId = :memberId")
    fun findByMemberIdWithLock(@Param("memberId") memberId: Long): QuestionCreditAccount?
}
