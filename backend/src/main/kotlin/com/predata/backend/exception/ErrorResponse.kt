package com.predata.backend.exception

import java.time.LocalDateTime

data class ErrorResponse(
    val code: String,
    val message: String,
    val status: Int,
    val details: List<String>? = null,
    val timestamp: String = LocalDateTime.now().toString()
) {
    companion object {
        fun of(errorCode: ErrorCode, details: List<String>? = null, customMessage: String? = null) =
            ErrorResponse(
                code = errorCode.name,
                message = customMessage ?: errorCode.message,
                status = errorCode.status,
                details = details
            )
    }
}
