package com.predata.backend.domain

import jakarta.persistence.*

@Entity
@Table(name = "badge_definitions")
data class BadgeDefinition(
    @Id
    @Column(name = "badge_id", length = 40)
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val rarity: String = "COMMON",
    @Column(name = "icon_url")
    val iconUrl: String? = null,
    @Column(name = "sort_order")
    val sortOrder: Int = 0
)
