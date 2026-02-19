package com.predata.backend.exception

open class BusinessException(
    val code: String,
    override val message: String,
    val httpStatus: Int = 400
) : RuntimeException(message)

class UnauthorizedException(
    message: String = "Authentication is required.",
    code: String = "UNAUTHORIZED"
) : BusinessException(code, message, 401)

class ForbiddenException(
    message: String = "Access denied.",
    code: String = "FORBIDDEN"
) : BusinessException(code, message, 403)

class NotFoundException(
    message: String = "Resource not found.",
    code: String = "NOT_FOUND"
) : BusinessException(code, message, 404)

class ConflictException(
    message: String = "Cannot process the request in current state.",
    code: String = "CONFLICT"
) : BusinessException(code, message, 409)

class ServiceUnavailableException(
    message: String = "Service is temporarily unavailable.",
    code: String = "SERVICE_UNAVAILABLE"
) : BusinessException(code, message, 503)
