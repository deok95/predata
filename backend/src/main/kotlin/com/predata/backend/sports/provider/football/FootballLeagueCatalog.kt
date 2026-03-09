package com.predata.backend.sports.provider.football

/**
 * Football-data 대상 리그 카탈로그
 * - UCL/UEL/UECL + 5대 리그 + K리그를 우선 수집 대상으로 제한한다.
 */
object FootballLeagueCatalog {

    data class LeagueSeed(
        val externalLeagueId: String,
        val name: String,
        val countryCode: String,
        val subCategory: String,
        val legacyCode: String? = null,
    )

    val priorityLeagues: List<LeagueSeed> = listOf(
        LeagueSeed("2", "UEFA Champions League", "EU", "UCL", legacyCode = "CL"),
        LeagueSeed("3", "UEFA Europa League", "EU", "UEL", legacyCode = "EL"),
        LeagueSeed("4", "UEFA Europa Conference League", "EU", "UECL", legacyCode = "UECL"),
        LeagueSeed("39", "Premier League", "GB", "TOP5_PREMIER_LEAGUE", legacyCode = "PL"),
        LeagueSeed("140", "La Liga", "ES", "TOP5_LA_LIGA", legacyCode = "PD"),
        LeagueSeed("135", "Serie A", "IT", "TOP5_SERIE_A", legacyCode = "SA"),
        LeagueSeed("78", "Bundesliga", "DE", "TOP5_BUNDESLIGA", legacyCode = "BL1"),
        LeagueSeed("61", "Ligue 1", "FR", "TOP5_LIGUE_1", legacyCode = "FL1"),
        LeagueSeed("292", "K League 1", "KR", "K_LEAGUE", legacyCode = "KLI"),
    )

    private val byExternalLeagueId: Map<String, LeagueSeed> = priorityLeagues.associateBy { it.externalLeagueId.uppercase() }
    private val byLegacyCode: Map<String, LeagueSeed> = priorityLeagues
        .filter { !it.legacyCode.isNullOrBlank() }
        .associateBy { it.legacyCode!!.uppercase() }

    fun findByLeagueIdOrLegacy(code: String?): LeagueSeed? {
        val normalized = code?.trim()?.uppercase() ?: return null
        return byExternalLeagueId[normalized] ?: byLegacyCode[normalized]
    }

    fun subCategoryByCode(code: String?): String =
        findByLeagueIdOrLegacy(code)?.subCategory ?: "FOOTBALL_OTHER"
}
