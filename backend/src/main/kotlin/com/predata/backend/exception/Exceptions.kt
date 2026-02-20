package com.predata.backend.exception

open class BusinessException(
    val code: String,
    override val message: String,
    val httpStatus: Int = 400
) : RuntimeException(message)

class UnauthorizedException(
    message: String = ErrorCode.UNAUTHORIZED.message,
    code: String = ErrorCode.UNAUTHORIZED.name
) : BusinessException(code, message, ErrorCode.UNAUTHORIZED.status)

class ForbiddenException(
    message: String = ErrorCode.FORBIDDEN.message,
    code: String = ErrorCode.FORBIDDEN.name
) : BusinessException(code, message, ErrorCode.FORBIDDEN.status)

class NotFoundException(
    message: String = ErrorCode.NOT_FOUND.message,
    code: String = ErrorCode.NOT_FOUND.name
) : BusinessException(code, message, ErrorCode.NOT_FOUND.status)

class ConflictException(
    message: String = ErrorCode.CONFLICT.message,
    code: String = ErrorCode.CONFLICT.name
) : BusinessException(code, message, ErrorCode.CONFLICT.status)

class ServiceUnavailableException(
    message: String = "Service is temporarily unavailable.",
    code: String = "SERVICE_UNAVAILABLE"
) : BusinessException(code, message, 503)
