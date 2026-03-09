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
    INVALID_CATEGORY(400, "유효하지 않은 카테고리입니다."),

    // Conflict
    CONFLICT(409, "Request cannot be processed in current state."),
    DUPLICATE_ENTRY(409, "Duplicate data."),

    // Not Found
    NOT_FOUND(404, "Resource not found."),

    // Rate Limit
    RATE_LIMITED(429, "Too many requests."),

    // Vote
    VOTING_CLOSED(409, "투표가 마감되었습니다."),
    ALREADY_VOTED(409, "이미 투표했습니다."),
    DAILY_LIMIT_EXCEEDED(429, "일일 투표 한도를 초과했습니다."),
    RELAY_RETRY_EXHAUSTED(503, "온체인 릴레이가 모두 실패했습니다. 잠시 후 다시 시도해 주세요."),
    SETTLEMENT_DELAY_ACTIVE(409, "정산 대기 시간이 아직 종료되지 않았습니다."),
    SETTLEMENT_CANCELLATION_DISABLED(410, "정산 취소는 지원하지 않습니다."),
    VOTE_MODE_MISMATCH(400, "이 질문은 커밋-리빌 투표 방식입니다. POST /api/votes/commit 을 사용하세요."),
    COMMIT_REVEAL_DISABLED(503, "커밋-리빌 투표가 현재 비활성화 상태입니다."),
    COMMIT_REVEAL_NOT_ELIGIBLE(400, "이 질문은 오픈 투표 방식입니다. POST /api/votes 를 사용하세요."),

    // Credit / Draft
    INSUFFICIENT_CREDITS(409, "크레딧이 부족합니다."),
    INSUFFICIENT_YEARLY_CREDITS(409, "연간 크레딧이 부족합니다."),
    DAILY_CREATE_LIMIT_EXCEEDED(409, "하루 질문 생성 한도를 초과했습니다."),
    ACTIVE_QUESTION_EXISTS(409, "진행 중인 질문이 있어 새 질문을 등록할 수 없습니다."),
    DRAFT_NOT_FOUND(409, "질문 초안을 찾을 수 없습니다."),
    DRAFT_EXPIRED(409, "작성 기간이 만료되었습니다. 다시 시작해 주세요."),
    DRAFT_ALREADY_CONSUMED(409, "이미 제출된 초안입니다."),
    DUPLICATE_QUESTION(409, "유사한 질문이 이미 존재합니다."),
    CREDIT_LOCK_TIMEOUT(409, "동시 요청이 많습니다. 잠시 후 다시 시도해 주세요."),

    // Settlement Review Queue
    REVIEW_QUEUE_NOT_FOUND(404, "정산 검토 큐 항목을 찾을 수 없습니다."),
    REVIEW_QUEUE_ALREADY_RESOLVED(409, "이미 처리 완료된 항목입니다."),

    // Market Batch
    BATCH_NOT_FOUND(404, "배치를 찾을 수 없습니다."),
    BATCH_ALREADY_RUNNING(409, "해당 슬롯의 배치가 이미 실행 중입니다."),
    BATCH_NO_FAILED_CANDIDATES(409, "재시도 대상 실패 후보가 없습니다."),
    BATCH_SELECTION_FAILED(500, "배치 선별 단계가 실패했습니다."),
    BATCH_OPEN_FAILED(500, "배치 오픈 단계가 실패했습니다."),
    BATCH_PARTIAL_FAILED(500, "배치 일부 오픈이 실패했습니다."),

    // Server
    INTERNAL_ERROR(500, "Internal server error occurred.");
}
