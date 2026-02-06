package com.predata.backend.repository

import com.predata.backend.domain.SystemSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SystemSettingsRepository : JpaRepository<SystemSettings, String> {

    fun findByKey(key: String): SystemSettings?
}
