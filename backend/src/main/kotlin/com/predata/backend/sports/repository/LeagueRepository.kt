package com.predata.backend.sports.repository

import com.predata.backend.sports.domain.League
import com.predata.backend.sports.domain.Sport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueRepository : JpaRepository<League, Long> {

    fun findBySportType(sportType: Sport): List<League>

    fun findByActiveTrue(): List<League>

    fun findBySportTypeAndActiveTrue(sportType: Sport): List<League>

    fun findByExternalLeagueIdAndProvider(externalLeagueId: String, provider: String): League?
}
