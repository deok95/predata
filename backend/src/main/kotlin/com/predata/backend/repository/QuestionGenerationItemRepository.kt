package com.predata.backend.repository

import com.predata.backend.domain.QuestionGenerationItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuestionGenerationItemRepository : JpaRepository<QuestionGenerationItem, Long> {
    fun findByBatchId(batchId: String): List<QuestionGenerationItem>
    fun findByBatchIdAndStatus(batchId: String, status: String): List<QuestionGenerationItem>
    fun findByBatchIdAndDraftIdIn(batchId: String, draftIds: List<String>): List<QuestionGenerationItem>
}
