#!/usr/bin/env python3
"""
Safe Korean to English converter for backend files.
Preserves code structure and line breaks.
"""

import re
import os
import sys
from pathlib import Path

# Translation dictionary for common Korean phrases
TRANSLATIONS = {
    # Common validation messages
    "필수입니다": "is required",
    "필수": "required",
    "유효하지 않은": "Invalid",
    "유효하지 않습니다": "is invalid",
    "실패했습니다": "failed",
    "실패": "failed",
    "성공했습니다": "succeeded",
    "성공": "succeeded",
    "최소": "minimum",
    "최대": "maximum",
    "이상이어야 합니다": "or more",
    "이하여야 합니다": "or less",
    "이상": "or more",
    "이하": "or less",
    "비어 있을 수 없습니다": "cannot be empty",
    "입력해주세요": "please enter",
    "입력하세요": "enter",
    "선택해주세요": "please select",
    "선택하세요": "select",

    # Business terms
    "질문": "question",
    "회원": "member",
    "베팅": "betting",
    "투표": "vote",
    "정산": "settlement",
    "주문": "order",
    "체결": "fill",
    "포지션": "position",
    "잔액": "balance",
    "티켓": "ticket",
    "활동": "activity",
    "로그": "log",
    "리워드": "reward",
    "보상": "reward",
    "수수료": "fee",
    "가격": "price",
    "거래": "trade",
    "마켓": "market",
    "카테고리": "category",
    "상태": "status",

    # Actions
    "조회": "retrieve",
    "생성": "create",
    "수정": "update",
    "삭제": "delete",
    "확인": "check",
    "검증": "validate",
    "처리": "process",
    "실행": "execute",
    "시작": "start",
    "종료": "end",
    "완료": "complete",

    # Time expressions
    "시간": "time",
    "기간": "period",
    "일일": "daily",
    "주간": "weekly",
    "월간": "monthly",

    # Status
    "활성화": "enabled",
    "비활성화": "disabled",
    "대기": "pending",
    "진행 중": "in progress",
    "완료": "completed",

    # Messages
    "오류가 발생했습니다": "An error occurred",
    "오류": "error",
    "경고": "warning",
    "알림": "notification",
    "메시지": "message",

    # User related
    "사용자": "user",
    "관리자": "admin",
    "게스트": "guest",

    # Numbers and quantities
    "개": "items",
    "건": "cases",
    "명": "people",
    "번": "number",

    # Common phrases
    "대한": "for",
    "관련": "related",
    "기준": "based on",
    "위한": "for",
    "통한": "through",
}

def convert_korean_in_string(text: str) -> str:
    """Convert Korean strings to English while preserving code structure."""
    # Replace common phrases first (longer phrases first to avoid partial replacements)
    for korean, english in sorted(TRANSLATIONS.items(), key=lambda x: len(x[0]), reverse=True):
        text = text.replace(korean, english)

    return text

def process_file(file_path: Path) -> bool:
    """Process a single Kotlin file."""
    try:
        # Read file with UTF-8 encoding
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        # Convert each line
        converted_lines = []
        has_changes = False

        for line in lines:
            original = line
            # Only convert strings within quotes and comments
            converted = convert_korean_in_string(line)

            if converted != original:
                has_changes = True

            converted_lines.append(converted)

        # Write back if there were changes
        if has_changes:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.writelines(converted_lines)
            print(f"✓ Converted: {file_path}")
            return True

        return False

    except Exception as e:
        print(f"✗ Error processing {file_path}: {e}")
        return False

def main():
    """Main function to process all Kotlin files."""
    src_dir = Path("src/main/kotlin")

    if not src_dir.exists():
        print(f"Error: {src_dir} not found")
        sys.exit(1)

    # Find all Kotlin files
    kt_files = list(src_dir.rglob("*.kt"))

    print(f"Found {len(kt_files)} Kotlin files")
    print("Starting conversion...")
    print()

    converted_count = 0
    for kt_file in kt_files:
        if process_file(kt_file):
            converted_count += 1

    print()
    print(f"Conversion complete!")
    print(f"Files converted: {converted_count}/{len(kt_files)}")

if __name__ == "__main__":
    main()
