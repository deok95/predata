package com.predata.backend.repository

import com.predata.backend.domain.QuestionCreditLedger
import org.springframework.data.jpa.repository.JpaRepository

interface QuestionCreditLedgerRepository : JpaRepository<QuestionCreditLedger, Long>
