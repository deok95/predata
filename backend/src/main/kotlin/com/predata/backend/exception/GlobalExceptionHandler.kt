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

    // === Business exceptions ===

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponse> {
        // 401, 403 use warning level, others use info level logging
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
                message = ex.message ?: "Invalid request.",
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
                message = ex.message ?: "Request cannot be processed in current state.",
                status = 409
            )
        )
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                code = "NOT_FOUND",
                message = ex.message ?: "Resource not found.",
                status = 404
            )
        )
    }

    // === Input validation exceptions ===

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        logger.warn("Validation failed: $errors")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                code = "VALIDATION_FAILED",
                message = "Input validation failed.",
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
                message = "Unable to parse request body.",
                status = 400
            )
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                code = "MISSING_PARAMETER",
                message = "Required parameter '${ex.parameterName}' is missing.",
                status = 400
            )
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                code = "TYPE_MISMATCH",
                message = "Parameter '${ex.name}' has invalid type.",
                status = 400
            )
        )
    }

    // === Data integrity exceptions (duplicate keys, etc.) ===

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        logger.warn("Data integrity violation: ${ex.message}")
        val message = when {
            ex.message?.contains("uk_member_question_type") == true -> "Activity already exists."
            ex.message?.contains("Duplicate entry") == true -> "Duplicate data."
            else -> "Data integrity violation."
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                code = "DUPLICATE_ENTRY",
                message = message,
                status = 409
            )
        )
    }

    // === Unexpected exceptions ===

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                code = "INTERNAL_ERROR",
                message = "Internal server error occurred.",
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
