package com.predata.backend.dto

import com.predata.backend.exception.ErrorCode
import java.time.LocalDateTime

data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
    // Ticket I에서 제거 예정: 현재 컨트롤러 직접 생성 패턴과의 호환용
    val message: String? = null,
    val timestamp: String = LocalDateTime.now().toString()
) {
    companion object {
        fun <T> ok(data: T): ApiEnvelope<T> =
            ApiEnvelope(success = true, data = data)

        fun <T> error(errorCode: ErrorCode, customMessage: String? = null): ApiEnvelope<T> =
            ApiEnvelope(
                success = false,
                error = ErrorDetail(
                    code = errorCode.name,
                    message = customMessage ?: errorCode.message,
                    status = errorCode.status
                )
            )
    }
}

data class ErrorDetail(
    val code: String,
    val message: String,
    val status: Int,
    val details: List<String>? = null
)

data class HealthStatusResponse(
    val status: String,
    val service: String,
    val version: String
)
