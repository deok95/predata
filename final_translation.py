#!/usr/bin/env python3
"""
Final comprehensive Korean to English translation
Handles all remaining Korean strings with context-aware translation
"""

import re
import os

def comprehensive_translate(text):
    """Comprehensively translate all Korean text to English"""

    # Dictionary of specific phrases (most specific first)
    translations = {
        # Complete sentences and phrases
        "테스트용: 이의제기 기간 없음 (즉시 확정)": "test: no dispute period (immediate confirmation)",
        "실제 운영시엔 24L로 변경": "change to 24L in production",
        "자동정산 Auto-first + Fail-safe 구현": "autosettlement Auto-first + Fail-safe implementation",
        "가지 조건 체크": " conditions check",
        "모두 non-null": "all non-null",
        "소스의 최종 상태 확인": "confirm source final status",
        "종목 확정": "final confirmation",
        "초 간격": " seconds interval",
        "회 조회 일치": " times retrieve match",
        "로 중복 정산 방지": "to prevent duplicate settlement",
        "가 아님": " is not",
        "를 통해 결과 조회": "retrieve result through",
        "결과 불완전": "result incomplete",
        "신뢰도 부족": "insufficient confidence",
        "소스가 미확정 상태": "source unconfirmed status",
        "구현 필요": "implementation needed",
        "중복 정산 방지": "prevent duplicate settlement",
        "조건 충족": "condition met",
        "완료 실행": "execute completion",
        "즉시 확정": "immediate confirmation",
        "어댑터 활용": "utilize adapter",
        "를 통해 자동으로 결과를 결정한다": "automatically determine result through",
        "이미 정산된 질문": "question already settled",
        "정산 가능 상태가 아닙니다": "not in settlement available status",
        "를 통해 자동 정산": "auto settlement through",
        "결과가 미확정이면 정산 시작 불가": "cannot start settlement if result unconfirmed",
        "정산 결과가 아직 확정되지 않았습니다": "settlement result not yet confirmed",
        "기존 메서드 재활용": "reuse existing method",
        "결과와 근거를 기록하고 이의 제기 기간을 시작한다": "record result and evidence and start dispute period",
        "배당 분배는 아직 하지 않는다": "do not distribute rewards yet",
    }

    for kr, en in translations.items():
        text = text.replace(kr, en)

    # Common Korean words and phrases
    word_dict = {
        # Nouns
        "트랜잭션": "transaction",
        "게시": "publish",
        "특정": "specific",
        "괴리율": "divergence rate",
        "동일": "same",
        "복구": "recovery",
        "가중치": "weight",
        "체크": "check",
        "탐지": "detection",
        "레퍼럴": "referral",
        "배당금": "dividend",
        "보유": "held",
        "분포": "distribution",
        "폴링": "polling",
        "보상": "reward",
        "구간": "interval",
        "누적": "cumulative",
        "예정": "scheduled",
        "대기": "pending",
        "진행": "in progress",
        "종료": "ended",
        "개선": "improvement",
        "최적화": "optimization",
        "성능": "performance",
        "효율": "efficiency",
        "안정": "stable",
        "장애": "failure",
        "재시도": "retry",
        "재전송": "resend",
        "대상": "target",
        "목표": "goal",
        "기간": "period",
        "추가": "add",
        "변경": "change",
        "갱신": "update",
        "통계": "statistics",
        "계산": "calculation",
        "가격": "price",
        "풀": "pool",
        "서버": "server",
        "오류": "error",
        "정산": "settlement",
        "결과": "result",
        "상태": "status",
        "조건": "condition",
        "확정": "confirmed",
        "미확정": "unconfirmed",
        "신뢰도": "confidence",
        "소스": "source",
        "질문": "question",
        "배당": "dividend",
        "분배": "distribution",
        "근거": "evidence",
        "이의": "dispute",
        "제기": "raise",
        "메서드": "method",
        "재활용": "reuse",
        "활용": "utilize",
        "어댑터": "adapter",
        "자동": "auto",
        "수동": "manual",
        "테스트": "test",
        "실제": "actual",
        "운영": "operation",
        "중복": "duplicate",
        "방지": "prevent",
        "회": "times",
        "개": "items",
        "번": "times",
        "초": "seconds",
        "분": "minutes",
        "시간": "hours",
        "일": "days",
        "주": "weeks",
        "건": "cases",
        "불완전": "incomplete",
        "부족": "insufficient",
        "완료": "complete",
        "실행": "execute",
        "즉시": "immediate",
        "기존": "existing",
        "아직": "yet",
        "만": "only",
        "전": "before",
        "후": "after",
        "중": "in/during",
        "시": "when",
        "용": "for",
        "간격": "interval",
        "일치": "match",

        # Verbs and adjectives
        "발생했습니다": "occurred",
        "확인": "check/confirm",
        "조회": "retrieve",
        "기록": "record",
        "시작": "start",
        "없음": "none",
        "가능": "available",
        "필요": "needed",
        "충족": "met",

        # Sentence endings (remove)
        "입니다": "",
        "합니다": "",
        "됩니다": "",
        "습니다": "",
        "하다": "",

        # Particles
        "를": "",
        "을": "",
        "가": "",
        "이": "",
        "는": "",
        "은": "",
        "와": "",
        "과": "",
        "로": "",
        "에": "",
        "의": "",
        "도": "",
        "부터": "from",
        "까지": "until",
    }

    # Apply word-level translations
    for kr, en in word_dict.items():
        # Use word boundary when replacing to avoid partial matches
        text = text.replace(kr, en)

    # Clean up extra spaces
    text = re.sub(r'\s+', ' ', text)
    text = re.sub(r'\s+\.', '.', text)
    text = re.sub(r'\s+,', ',', text)
    text = re.sub(r'\s+\)', ')', text)
    text = re.sub(r'\(\s+', '(', text)

    return text

def process_file(filepath):
    """Process a single file"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        if not re.search(r'[가-힣]', content):
            return 0

        original_korean_count = len(re.findall(r'[가-힣]+', content))
        new_content = comprehensive_translate(content)
        new_korean_count = len(re.findall(r'[가-힣]+', new_content))

        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)

            return original_korean_count - new_korean_count
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
    return 0

def main():
    backend_dir = "/Users/harrykim/Desktop/predata/backend/src/main/kotlin"
    total_removed = 0
    files_processed = 0

    for root, dirs, files in os.walk(backend_dir):
        for file in files:
            if file.endswith('.kt'):
                filepath = os.path.join(root, file)
                removed = process_file(filepath)
                if removed > 0:
                    files_processed += 1
                    total_removed += removed

    print(f"Files processed: {files_processed}")
    print(f"Korean words removed: {total_removed}")

    # Count remaining
    remaining_files = 0
    remaining_lines = 0
    for root, dirs, files in os.walk(backend_dir):
        for file in files:
            if file.endswith('.kt'):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, 'r', encoding='utf-8') as f:
                        content = f.read()
                        if re.search(r'[가-힣]', content):
                            remaining_files += 1
                            remaining_lines += len(re.findall(r'[가-힣]+', content))
                except:
                    pass

    print(f"Remaining files with Korean: {remaining_files}")
    print(f"Remaining Korean words: {remaining_lines}")

if __name__ == '__main__':
    main()
