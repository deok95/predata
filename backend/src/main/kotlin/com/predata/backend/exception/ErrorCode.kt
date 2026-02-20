package com.predata.backend.exception

enum class ErrorCode(val status: Int, val message: String) {
    // Auth
    UNAUTHORIZED(401, "Authentication required."),
    INVALID_TOKEN(401, "Invalid or expired token."),
    FORBIDDEN(403, "Access denied."),
    ACCOUNT_BANNED(403, "Account has been suspended."),

    // Validation
    BAD_REQUEST(400, "Invalid request."),
    VALIDATION_FAILED(400, "Input validation failed."),
    INVALID_REQUEST_BODY(400, "Unable to parse request body."),
    MISSING_PARAMETER(400, "Required parameter is missing."),
    TYPE_MISMATCH(400, "Parameter has invalid type."),

    // Conflict
    CONFLICT(409, "Request cannot be processed in current state."),
    DUPLICATE_ENTRY(409, "Duplicate data."),

    // Not Found
    NOT_FOUND(404, "Resource not found."),

    // Rate Limit
    RATE_LIMITED(429, "Too many requests."),

    // Server
    INTERNAL_ERROR(500, "Internal server error occurred.");
}
