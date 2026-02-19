#!/usr/bin/env python3
"""
Convert Korean strings to English in Kotlin backend files
"""

import re
import os
import sys

# Translation dictionary for common Korean phrases
TRANSLATIONS = {
    # Common messages
    "인증이 필요합니다": "Authentication is required",
    "인증이 필요합니다.": "Authentication is required.",
    "권한이 없습니다": "Access denied",
    "권한이 없습니다.": "Access denied.",
    "리소스를 찾을 수 없습니다": "Resource not found",
    "리소스를 찾을 수 없습니다.": "Resource not found.",
    "요청을 처리할 수 없는 상태입니다": "Request cannot be processed in current state",
    "요청을 처리할 수 없는 상태입니다.": "Request cannot be processed in current state.",
    "서비스를 일시적으로 사용할 수 없습니다": "Service is temporarily unavailable",
    "서비스를 일시적으로 사용할 수 없습니다.": "Service is temporarily unavailable.",
    "잘못된 요청입니다": "Invalid request",
    "잘못된 요청입니다.": "Invalid request.",
    "서버 내부 오류가 발생했습니다": "Internal server error occurred",
    "서버 내부 오류가 발생했습니다.": "Internal server error occurred.",

    # Validation messages
    "입력값 검증에 실패했습니다": "Validation failed",
    "입력값 검증에 실패했습니다.": "Validation failed.",
    "요청 본문을 파싱할 수 없습니다": "Cannot parse request body",
    "요청 본문을 파싱할 수 없습니다.": "Cannot parse request body.",
    "데이터 무결성 위반입니다": "Data integrity violation",
    "데이터 무결성 위반입니다.": "Data integrity violation.",
    "중복된 데이터입니다": "Duplicate entry",
    "중복된 데이터입니다.": "Duplicate entry.",
    "이미 동일한 활동이 존재합니다": "Activity already exists",
    "이미 동일한 활동이 존재합니다.": "Activity already exists.",

    # Member/Auth related
    "이미 가입된 이메일입니다": "Email is already registered",
    "이미 가입된 이메일입니다.": "Email is already registered.",
    "회원을 찾을 수 없습니다": "Member not found",
    "회원을 찾을 수 없습니다.": "Member not found.",
    "회원가입이 완료되었습니다": "Registration completed",
    "회원가입이 완료되었습니다.": "Registration completed.",
    "로그인 성공": "Login successful",
    "로그인에 성공했습니다": "Login successful",
    "이메일 또는 비밀번호가 일치하지 않습니다": "Email or password does not match",
    "이메일 또는 비밀번호가 일치하지 않습니다.": "Email or password does not match.",
    "계정이 정지되었습니다": "Account is suspended",
    "정지된 계정입니다": "Account is suspended",
    "유효하지 않거나 만료된 토큰입니다": "Invalid or expired token",
    "유효하지 않거나 만료된 토큰입니다.": "Invalid or expired token.",
    "토큰 형식이 올바르지 않습니다": "Invalid token format",
    "토큰 형식이 올바르지 않습니다.": "Invalid token format.",

    # Verification
    "인증 코드가 이메일로 발송되었습니다": "Verification code has been sent to email",
    "인증 코드를 찾을 수 없습니다": "Verification code not found",
    "인증 코드를 찾을 수 없습니다.": "Verification code not found.",
    "이미 사용된 인증 코드입니다": "Verification code has already been used",
    "이미 사용된 인증 코드입니다.": "Verification code has already been used.",
    "인증 코드가 만료되었습니다": "Verification code has expired",
    "인증 코드가 만료되었습니다.": "Verification code has expired.",
    "인증 시도 횟수를 초과했습니다": "Maximum verification attempts exceeded",
    "인증 시도 횟수를 초과했습니다.": "Maximum verification attempts exceeded.",
    "인증 코드가 일치하지 않습니다": "Verification code does not match",
    "인증 코드가 일치하지 않습니다.": "Verification code does not match.",
    "인증 코드가 확인되었습니다": "Verification code confirmed",
    "인증 코드가 확인되었습니다.": "Verification code confirmed.",
    "인증되지 않은 이메일입니다": "Email is not verified",
    "인증되지 않은 이메일입니다.": "Email is not verified.",

    # Question related
    "질문을 찾을 수 없습니다": "Question not found",
    "질문을 찾을 수 없습니다.": "Question not found.",
    "해당 질문은 AMM 실행 모델이 아닙니다": "Question does not use AMM execution model",
    "해당 질문은 AMM 실행 모델이 아닙니다.": "Question does not use AMM execution model.",

    # Betting/Trading related
    "베팅 기간이 아닙니다": "Not in betting period",
    "베팅 기간이 아닙니다.": "Not in betting period.",
    "마켓 풀을 찾을 수 없습니다": "Market pool not found",
    "마켓 풀을 찾을 수 없습니다.": "Market pool not found.",
    "마켓 풀이 활성화되지 않았습니다": "Market pool is not active",
    "마켓 풀이 활성화되지 않았습니다.": "Market pool is not active.",
    "최소 거래 금액은": "Minimum trade amount is",
    "USDC 잔액이 부족합니다": "Insufficient USDC balance",
    "shares 잔액이 부족합니다": "Insufficient shares balance",
    "보유한 shares가 없습니다": "No shares available",
    "보유한 shares가 없습니다.": "No shares available.",
    "슬리피지 초과": "Slippage exceeded",
    "시드 금액은 0보다 커야 합니다": "Seed amount must be greater than 0",
    "시드 금액은 0보다 커야 합니다.": "Seed amount must be greater than 0.",
    "수수료율은": "Fee rate must be",
    "해당 질문에 이미 마켓 풀이 존재합니다": "Market pool already exists for this question",
    "해당 질문에 이미 마켓 풀이 존재합니다.": "Market pool already exists for this question.",
    "정산 완료되거나 취소된 질문에는 풀을 생성할 수 없습니다": "Cannot create pool for settled or cancelled question",
    "정산 완료되거나 취소된 질문에는 풀을 생성할 수 없습니다.": "Cannot create pool for settled or cancelled question.",

    # Vote related
    "시스템 점검 중입니다": "System maintenance in progress",
    "시스템 점검 중입니다.": "System maintenance in progress.",
    "투표 패스가 필요합니다": "Voting pass is required",
    "투표 패스가 필요합니다.": "Voting pass is required.",
    "현재 투표 접수 기간이 아닙니다": "Not in voting commit period",
    "현재 투표 접수 기간이 아닙니다.": "Not in voting commit period.",
    "이미 투표하셨습니다": "Already voted",
    "이미 투표하셨습니다.": "Already voted.",

    # Rate limit
    "요청이 너무 많습니다": "Too many requests",

    # Parameter validation
    "필수입니다": "is required",
    "필수 파라미터": "Required parameter",
    "가 누락되었습니다": "is missing",
    "의 타입이 올바르지 않습니다": "has invalid type",
    "는 1 이상": "must be between 1 and",
    "이하여야 합니다": "",

    # Actions
    "필수입니다.": "is required.",
    "조회": "retrieve",
    "생성": "create",
    "수정": "update",
    "삭제": "delete",
    "정산": "settlement",
    "베팅": "betting",
    "투표": "vote",
    "질문": "question",
    "회원": "member",
    "주문": "order",
    "체결": "fill",
    "포지션": "position",
    "잔액": "balance",

    # Comments
    "기본 검증": "Basic validation",
    "액션별 처리": "Process by action",
    "슬리피지 보호": "Slippage protection",
    "상태 업데이트": "Update state",
    "수수료 분배": "Distribute fee",
    "비동기 처리": "async",
    "비관적 락": "pessimistic lock",
    "응답 생성": "Build response",
    "풀 초기화": "Initialize pool",
    "풀 생성": "Create pool",
    "이미 풀이 있는지 확인": "Check if pool already exists",
    "질문 존재 확인": "Check question existence",
    "회원 존재 확인": "Check member existence",
    "밴 체크": "Check ban status",
    "중복 투표 체크": "Check duplicate vote",
    "일일 투표 한도 체크": "Check daily vote limit",
    "임시 저장소": "Temporary storage",
    "프로덕션에서는 Redis 등 사용": "use Redis in production",
    "이메일로 인증 코드 발송": "Send verification code to email",
    "인증 코드 검증": "Verify code",
    "회원 생성 안 함": "does not create member",
    "비밀번호 설정 및 회원 생성": "Set password and create member",
    "로그인": "Login",
    "인증된 이메일인지 확인": "Verify email is verified",
    "DB에서 직접 확인": "check directly from DB",
    "인증 완료 여부 확인": "Check verification completion status",
    "코드 일치 여부 확인": "Check code match",
    "만료 확인": "Check expiration",
    "이중 확인": "double-check",
    "ISO 8601 파싱": "ISO 8601 parsing",
    "있다면": "if exists",
    "자동 로그인": "auto login",
    "비밀번호 해시 존재 여부 확인": "Check if password hash exists",
    "비밀번호 검증": "Verify password",
    "손상 데이터": "corrupted data",
    "비정상 해시": "invalid hash",

    # Time/Date related
    "분 이내 입력": "Enter within",
    "회 남음": "attempts remaining",
    "초 후 다시 시도해주세요": "seconds",

    # Admin/Config
    "ADMIN 전용": "ADMIN only",
    "개발 모드": "Development mode",
    "프로덕션 모드": "Production mode",
    "관대한 제한": "relaxed limits",
    "엄격한 제한": "strict limits",
    "헬스체크는 제한 없음": "Health check has no limit",
    "경로별 제한 분류": "Classify limits by path",
    "개발/프로덕션 모드에 따라 다른 제한 적용": "apply different limits based on dev/prod mode",
    "주기적 정리": "Periodic cleanup",
    "제한 초과": "Limit exceeded",
    "새 윈도우 시작": "Start new window",

    # Misc
    "관리자에게 문의하세요": "Please contact administrator",
    "관리자에게 문의하세요.": "Please contact administrator.",
    "다시 시도해주세요": "Please try again",
    "다시 시도해주세요.": "Please try again.",
    "잠시 후 다시 시도해주세요": "Please try again later",
    "잠시 후 다시 시도해주세요.": "Please try again later.",
    "마이페이지에서 구매해주세요": "Please purchase from My Page",
    "마이페이지에서 구매해주세요.": "Please purchase from My Page.",
    "지갑으로 로그인하거나 비밀번호를 재설정하세요": "Please login with wallet or reset your password",
    "지갑으로 로그인하거나 비밀번호를 재설정하세요.": "Please login with wallet or reset your password.",
    "인증 코드를 먼저 확인해주세요": "Please verify the code first",
    "인증 코드를 먼저 확인해주세요.": "Please verify the code first.",
    "이용약관 위반": "Terms of service violation",
    "사유": "Reason",
    "사유:": "Reason:",
    "보유:": "Have:",
    "필요:": "Need:",
    "예상 최소": "expected minimum",
    "실제": "actual",
    "현재 상태": "Current status",
    "현재 상태:": "Current status:",
    "티켓 소진": "Ticket exhausted",
    "오늘 날짜 기준 티켓 조회 또는 생성": "Retrieve or create ticket for today",
    "티켓 1개 차감": "Deduct 1 ticket",
    "5-Lock 체크": "5-Lock check",
    "티켓 1개 복구": "Refund 1 ticket",
    "경합/예외 시 보정용": "for race condition/exception compensation",
    "남은 티켓 개수 조회": "Retrieve remaining tickets",
    "최소": "minimum",
    "최대": "maximum",
    "이상": "or more",
    "이하": "or less",
    "범위": "range",
    "범위여야 합니다": "must be in range",
}

def translate_korean(content):
    """Translate Korean strings to English in the content"""
    # Sort by length (longest first) to avoid partial replacements
    sorted_translations = sorted(TRANSLATIONS.items(), key=lambda x: len(x[0]), reverse=True)

    for korean, english in sorted_translations:
        content = content.replace(korean, english)

    return content

def process_file(filepath):
    """Process a single Kotlin file"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        # Check if file contains Korean
        if not re.search(r'[가-힣]', content):
            return False

        original_content = content
        content = translate_korean(content)

        # Only write if content changed
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
    except Exception as e:
        print(f"Error processing {filepath}: {e}", file=sys.stderr)
        return False

    return False

def main():
    """Main function"""
    backend_dir = "/Users/harrykim/Desktop/predata/backend/src/main/kotlin"

    if not os.path.exists(backend_dir):
        print(f"Backend directory not found: {backend_dir}", file=sys.stderr)
        return 1

    processed = 0
    total = 0

    # Find all Kotlin files
    for root, dirs, files in os.walk(backend_dir):
        for file in files:
            if file.endswith('.kt'):
                filepath = os.path.join(root, file)
                total += 1
                if process_file(filepath):
                    processed += 1
                    print(f"Processed: {filepath}")

    print(f"\nTotal files: {total}")
    print(f"Processed files with Korean: {processed}")

    return 0

if __name__ == "__main__":
    sys.exit(main())
