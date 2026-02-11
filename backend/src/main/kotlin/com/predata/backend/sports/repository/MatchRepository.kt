package com.predata.backend.sports.repository

import com.predata.backend.sports.domain.Match
import com.predata.backend.sports.domain.MatchStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MatchRepository : JpaRepository<Match, Long> {

    fun findByMatchStatus(matchStatus: MatchStatus): List<Match>

    fun findByLeagueIdAndMatchTimeBetween(
        leagueId: Long,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Match>

    fun findByExternalMatchIdAndProvider(externalMatchId: String, provider: String): Match?
}
