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
        initPriorityLeagues()
    }

    private fun initPriorityLeagues() {
        FootballLeagueCatalog.priorityLeagues.forEach { seed ->
            val existing = leagueRepository.findByExternalLeagueIdAndProvider(
                seed.externalLeagueId, FootballDataProvider.PROVIDER_NAME
            )
            if (existing != null) {
                logger.info("[FootballDataInit] {} already exists (id={})", seed.name, existing.id)
                return@forEach
            }

            // Legacy football-data.org code migration (PL/PD/SA/...)
            val legacy = seed.legacyCode?.let {
                leagueRepository.findByExternalLeagueIdAndProvider(it, FootballDataProvider.PROVIDER_NAME)
            }
            if (legacy != null) {
                val migrated = legacy.copy(
                    name = seed.name,
                    countryCode = seed.countryCode,
                    externalLeagueId = seed.externalLeagueId,
                    active = true
                )
                leagueRepository.save(migrated)
                logger.info("[FootballDataInit] {} migrated from legacy code {} -> {}", seed.name, seed.legacyCode, seed.externalLeagueId)
                return@forEach
            }

            val league = League(
                name = seed.name,
                sportType = Sport.FOOTBALL,
                countryCode = seed.countryCode,
                externalLeagueId = seed.externalLeagueId,
                provider = FootballDataProvider.PROVIDER_NAME,
                active = true
            )
            leagueRepository.save(league)
            logger.info("[FootballDataInit] {} created successfully", seed.name)
        }
    }
}
