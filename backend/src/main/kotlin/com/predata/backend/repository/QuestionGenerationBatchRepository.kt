package com.predata.backend.repository

import com.predata.backend.domain.QuestionGenerationBatch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface QuestionGenerationBatchRepository : JpaRepository<QuestionGenerationBatch, Long> {
    fun findByBatchId(batchId: String): QuestionGenerationBatch?
    fun findBySubcategoryAndTargetDate(subcategory: String, targetDate: LocalDate): QuestionGenerationBatch?
}
