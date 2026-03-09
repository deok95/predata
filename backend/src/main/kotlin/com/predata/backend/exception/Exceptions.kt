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

class BadRequestException(
    message: String = ErrorCode.BAD_REQUEST.message,
    code: String = ErrorCode.BAD_REQUEST.name
) : BusinessException(code, message, ErrorCode.BAD_REQUEST.status)

class RateLimitException(
    message: String = ErrorCode.RATE_LIMITED.message,
    code: String = ErrorCode.RATE_LIMITED.name
) : BusinessException(code, message, ErrorCode.RATE_LIMITED.status)

class VotingClosedException(
    message: String = ErrorCode.VOTING_CLOSED.message,
    code: String = ErrorCode.VOTING_CLOSED.name
) : BusinessException(code, message, ErrorCode.VOTING_CLOSED.status)

class AlreadyVotedException(
    message: String = ErrorCode.ALREADY_VOTED.message,
    code: String = ErrorCode.ALREADY_VOTED.name
) : BusinessException(code, message, ErrorCode.ALREADY_VOTED.status)

class DailyLimitExceededException(
    message: String = ErrorCode.DAILY_LIMIT_EXCEEDED.message,
    code: String = ErrorCode.DAILY_LIMIT_EXCEEDED.name
) : BusinessException(code, message, ErrorCode.DAILY_LIMIT_EXCEEDED.status)

class RelayRetryExhaustedException(
    message: String = ErrorCode.RELAY_RETRY_EXHAUSTED.message,
    code: String = ErrorCode.RELAY_RETRY_EXHAUSTED.name
) : BusinessException(code, message, ErrorCode.RELAY_RETRY_EXHAUSTED.status)

class SettlementDelayActiveException(
    message: String = ErrorCode.SETTLEMENT_DELAY_ACTIVE.message,
    code: String = ErrorCode.SETTLEMENT_DELAY_ACTIVE.name
) : BusinessException(code, message, ErrorCode.SETTLEMENT_DELAY_ACTIVE.status)

class SettlementCancellationDisabledException(
    message: String = ErrorCode.SETTLEMENT_CANCELLATION_DISABLED.message,
    code: String = ErrorCode.SETTLEMENT_CANCELLATION_DISABLED.name
) : BusinessException(code, message, ErrorCode.SETTLEMENT_CANCELLATION_DISABLED.status)
