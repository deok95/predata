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

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // === Business exceptions ===

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ErrorResponse> {
        logger.warn("Unauthorized: ${ex.message}")
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(code = ex.code, message = ex.message, status = HttpStatus.UNAUTHORIZED.value())
        )
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponse> {
        if (ex.httpStatus in 401..403) {
            logger.warn("Business exception: ${ex.code} - ${ex.message}")
        } else {
            logger.info("Business exception: ${ex.code} - ${ex.message}")
        }
        return ResponseEntity.status(ex.httpStatus).body(
            ErrorResponse(code = ex.code, message = ex.message, status = ex.httpStatus)
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Bad request: ${ex.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.of(ErrorCode.BAD_REQUEST, customMessage = ex.message)
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        logger.warn("Conflict: ${ex.message}")
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse.of(ErrorCode.CONFLICT, customMessage = ex.message)
        )
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse.of(ErrorCode.NOT_FOUND, customMessage = ex.message)
        )
    }

    // === Input validation exceptions ===

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        logger.warn("Validation failed: $errors")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.of(ErrorCode.VALIDATION_FAILED, details = errors)
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.warn("Unreadable request body: ${ex.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.of(ErrorCode.INVALID_REQUEST_BODY)
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.of(
                ErrorCode.MISSING_PARAMETER,
                customMessage = "Required parameter '${ex.parameterName}' is missing."
            )
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.of(
                ErrorCode.TYPE_MISMATCH,
                customMessage = "Parameter '${ex.name}' has invalid type."
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
            ErrorResponse.of(ErrorCode.DUPLICATE_ENTRY, customMessage = message)
        )
    }

    // === Unexpected exceptions ===

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.of(ErrorCode.INTERNAL_ERROR)
        )
    }
}
