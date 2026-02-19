#!/usr/bin/env python3
import re
import os
import sys

# Comprehensive Korean to English dictionary
KR_EN = {
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
    "진행": "progress",
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
    
    # Verbs and expressions
    "발생했습니다": "occurred",
    "입니다": "",
    "합니다": "",
    "됩니다": "",
    "않습니다": "",
    
    # Particles (Korean grammar particles - replace with spaces or appropriate English)
    # These should be removed carefully
}

def aggressive_clean(content):
    # First: dictionary replacements
    for kr, en in KR_EN.items():
        content = content.replace(kr, en)
    
    # Remove common Korean particles (carefully)
    particles = ["는", "은", "가", "이", "을", "를", "에", "로", "으로", "와", "과", "의", "도", "만", "부터", "까지", "에게", "한", "시", "건"]
    for particle in particles:
        # Only remove if followed by space
        content = re.sub(f'{particle} ', ' ', content)
    
    # Clean up counter words
    content = re.sub(r'(\d+)개', r'\1 items', content)
    content = re.sub(r'(\d+)회', r'\1 times', content)
    content = re.sub(r'(\d+)번', r'\1 times', content)
    content = re.sub(r'(\d+)시간', r'\1 hours', content)
    content = re.sub(r'(\d+)분', r'\1 minutes', content)
    content = re.sub(r'(\d+)초', r'\1 seconds', content)
    content = re.sub(r'(\d+)일', r'\1 days', content)
    content = re.sub(r'(\d+)주', r'\1 weeks', content)
    
    return content

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if not re.search(r'[가-힣]', content):
            return False
            
        new_content = aggressive_clean(content)
        
        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            return True
    except Exception as e:
        print(f"Error: {filepath}: {e}", file=sys.stderr)
    return False

backend_dir = "/Users/harrykim/Desktop/predata/backend/src/main/kotlin"
processed = 0

for root, dirs, files in os.walk(backend_dir):
    for file in files:
        if file.endswith('.kt'):
            if process_file(os.path.join(root, file)):
                processed += 1

print(f"Processed: {processed} files")
