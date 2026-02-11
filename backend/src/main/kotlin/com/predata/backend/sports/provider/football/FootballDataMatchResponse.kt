package com.predata.backend.sports.provider.football

data class FootballDataMatchesResponse(
    val matches: List<FootballDataMatch> = emptyList()
)

data class FootballDataMatch(
    val id: Long,
    val utcDate: String,
    val status: String,
    val homeTeam: FootballDataTeam,
    val awayTeam: FootballDataTeam,
    val score: FootballDataScore?
)

data class FootballDataTeam(
    val id: Long,
    val name: String,
    val shortName: String? = null,
    val tla: String? = null
)

data class FootballDataScore(
    val winner: String? = null,
    val fullTime: FootballDataScoreDetail? = null,
    val halfTime: FootballDataScoreDetail? = null
)

data class FootballDataScoreDetail(
    val home: Int? = null,
    val away: Int? = null
)
