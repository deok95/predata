package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "system_settings")
data class SystemSettings(
    @Id
    @Column(name = "setting_key", length = 100)
    val key: String,

    @Column(name = "setting_value", columnDefinition = "TEXT", nullable = false)
    var value: String,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
