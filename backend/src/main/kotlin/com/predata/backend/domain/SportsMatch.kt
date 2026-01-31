package com.predata.backend.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "sports_matches")
data class SportsMatch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    val id: Long? = null,

    @Column(name = "question_id")
    var questionId: Long? = null,

    @Column(name = "external_api_id")
    val externalApiId: String, // API에서 받아온 경기 ID

    @Column(name = "sport_type")
    val sportType: String, // FOOTBALL, BASKETBALL, BASEBALL

    @Column(name = "league_name")
    val leagueName: String, // EPL, La Liga, NBA 등

    @Column(name = "home_team")
    val homeTeam: String,

    @Column(name = "away_team")
    val awayTeam: String,

    @Column(name = "match_date")
    val matchDate: LocalDateTime,

    @Column(name = "result")
    var result: String? = null, // HOME_WIN, AWAY_WIN, DRAW

    @Column(name = "home_score")
    var homeScore: Int? = null,

    @Column(name = "away_score")
    var awayScore: Int? = null,

    @Column(name = "status")
    var status: String = "SCHEDULED", // SCHEDULED, LIVE, FINISHED, BETTING_SUSPENDED
    
    @Column(name = "betting_suspended_until")
    var bettingSuspendedUntil: LocalDateTime? = null, // 베팅 재개 시간

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
