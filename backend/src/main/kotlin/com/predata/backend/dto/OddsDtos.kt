package com.predata.backend.dto

data class OddsResult(
    val yesOdds: String,
    val noOdds: String,
    val yesPrice: String,
    val noPrice: String
)

data class OddsResponse(
    val questionId: Long,
    val yesOdds: String,
    val noOdds: String,
    val yesPrice: String,
    val noPrice: String,
    val poolYes: Long,
    val poolNo: Long,
    val totalPool: Long
)
