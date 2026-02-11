package com.predata.backend.sports.provider.football

import com.predata.backend.sports.domain.League
import com.predata.backend.sports.domain.Sport
import com.predata.backend.sports.repository.LeagueRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class FootballDataLeagueInitializer(
    private val leagueRepository: LeagueRepository
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(FootballDataLeagueInitializer::class.java)

    override fun run(args: ApplicationArguments?) {
        initPremierLeague()
    }

    private fun initPremierLeague() {
        val existing = leagueRepository.findByExternalLeagueIdAndProvider(
            "PL", FootballDataProvider.PROVIDER_NAME
        )
        if (existing != null) {
            logger.info("[FootballDataInit] Premier League already exists (id=${existing.id})")
            return
        }

        val league = League(
            name = "Premier League",
            sportType = Sport.FOOTBALL,
            countryCode = "GB",
            externalLeagueId = "PL",
            provider = FootballDataProvider.PROVIDER_NAME,
            active = true
        )
        leagueRepository.save(league)
        logger.info("[FootballDataInit] Premier League created successfully")
    }
}
