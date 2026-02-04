package com.predata.backend.repository

import com.predata.backend.domain.BadgeDefinition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BadgeDefinitionRepository : JpaRepository<BadgeDefinition, String> {
    fun findAllByOrderBySortOrderAsc(): List<BadgeDefinition>
}
