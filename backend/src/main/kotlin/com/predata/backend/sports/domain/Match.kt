package com.predata.backend.sports.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "matches")
data class Match(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    val league: League,

    @Column(name = "home_team", nullable = false, length = 100)
    val homeTeam: String,

    @Column(name = "away_team", nullable = false, length = 100)
    val awayTeam: String,

    @Column(name = "home_score")
    var homeScore: Int? = null,

    @Column(name = "away_score")
    var awayScore: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 20)
    var matchStatus: MatchStatus = MatchStatus.SCHEDULED,

    @Column(name = "match_time", nullable = false)
    val matchTime: LocalDateTime,

    @Column(name = "external_match_id", length = 50)
    val externalMatchId: String? = null,

    @Column(length = 50)
    val provider: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
