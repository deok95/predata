package com.predata.backend.exception

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.LocalDateTime

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // === 비즈니스 예외 ===

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponse> {
        // 401, 403은 warning, 나머지는 info 레벨 로깅
        if (ex.httpStatus in 401..403) {
            logger.warn("Business exception: ${ex.code} - ${ex.message}")
        } else {
            logger.info("Business exception: ${ex.code} - ${ex.message}")
        }

        return ResponseEntity.status(ex.httpStatus).body(
            ErrorResponse(
                code = ex.code,
                message = ex.message,
                status = ex.httpStatus
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Bad request: ${ex.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                code = "BAD_REQUEST",
                message = ex.message ?: "잘못된 요청입니다.",
                status = 400
            )
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        logger.warn("Conflict: ${ex.message}")
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                code = "CONFLICT",
                message = ex.message ?: "요청을 처리할 수 없는 상태입니다.",
                status = 409
            )
        )
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                code = "NOT_FOUND",
                message = ex.message ?: "리소스를 찾을 수 없습니다.",
                status = 404
            )
        )
    }

    // === 입력 검증 예외 ===

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        logger.warn("Validation failed: $errors")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                code = "VALIDATION_FAILED",
                message = "입력값 검증에 실패했습니다.",
                status = 400,
                details = errors
            )
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.warn("Unreadable request body: ${ex.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                code = "INVALID_REQUEST_BODY",
                message = "요청 본문을 파싱할 수 없습니다.",
                status = 400
            )
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                code = "MISSING_PARAMETER",
                message = "필수 파라미터 '${ex.parameterName}'가 누락되었습니다.",
                status = 400
            )
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                code = "TYPE_MISMATCH",
                message = "파라미터 '${ex.name}'의 타입이 올바르지 않습니다.",
                status = 400
            )
        )
    }

    // === 데이터 무결성 예외 (중복 키 등) ===

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        logger.warn("Data integrity violation: ${ex.message}")
        val message = when {
            ex.message?.contains("uk_member_question_type") == true -> "이미 동일한 활동이 존재합니다."
            ex.message?.contains("Duplicate entry") == true -> "중복된 데이터입니다."
            else -> "데이터 무결성 위반입니다."
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                code = "DUPLICATE_ENTRY",
                message = message,
                status = 409
            )
        )
    }

    // === 예상치 못한 예외 ===

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                code = "INTERNAL_ERROR",
                message = "서버 내부 오류가 발생했습니다.",
                status = 500
            )
        )
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val status: Int,
    val details: List<String>? = null,
    val timestamp: String = LocalDateTime.now().toString()
)
