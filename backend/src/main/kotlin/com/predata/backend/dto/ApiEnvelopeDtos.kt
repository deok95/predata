package com.predata.backend.dto

data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

data class HealthStatusResponse(
    val status: String,
    val service: String,
    val version: String
)
