package com.predata.backend.sports.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "leagues")
data class League(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "league_id")
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false, length = 20)
    val sportType: Sport,

    @Column(name = "country_code", length = 2)
    val countryCode: String? = null,

    @Column(name = "external_league_id", length = 50)
    val externalLeagueId: String? = null,

    @Column(length = 50)
    val provider: String? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
