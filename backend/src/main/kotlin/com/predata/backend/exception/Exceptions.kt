package com.predata.backend.exception

open class BusinessException(
    val code: String,
    override val message: String,
    val httpStatus: Int = 400
) : RuntimeException(message)

class UnauthorizedException(
    message: String = "인증이 필요합니다.",
    code: String = "UNAUTHORIZED"
) : BusinessException(code, message, 401)

class ForbiddenException(
    message: String = "권한이 없습니다.",
    code: String = "FORBIDDEN"
) : BusinessException(code, message, 403)

class NotFoundException(
    message: String = "리소스를 찾을 수 없습니다.",
    code: String = "NOT_FOUND"
) : BusinessException(code, message, 404)

class ConflictException(
    message: String = "요청을 처리할 수 없는 상태입니다.",
    code: String = "CONFLICT"
) : BusinessException(code, message, 409)

class ServiceUnavailableException(
    message: String = "서비스를 일시적으로 사용할 수 없습니다.",
    code: String = "SERVICE_UNAVAILABLE"
) : BusinessException(code, message, 503)
