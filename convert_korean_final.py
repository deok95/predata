#!/usr/bin/env python3
"""
Final cleanup of remaining Korean strings
"""

import re
import os
import sys

def final_cleanup(content):
    """Final cleanup of remaining Korean patterns"""

    replacements = [
        # Comment patterns that were missed
        (r'// 블록체인 failure 등으로 cancel됨', '// Cancelled due to blockchain failure, etc.'),
        (r'// validate 가능한 question \(스포츠 result 등\)', '// Verifiable question (sports results, etc.)'),
        (r'// 의견 기반 question', '// Opinion-based question'),
        (r'// validate 가능한 시장 \(스포츠, 주가 등 외부 data 기반\)', '// Verifiable market (based on external data like sports, stocks)'),
        (r'// 의견 기반 시장 \(vote result 기반\)', '// Opinion-based market (based on vote results)'),
        (r'// 레거시 오더북 execute model', '// Legacy orderbook execution model'),

        # Voting phase comments
        (r'\* vote 및 betting 단계', '* Vote and betting phases'),
        (r'\* - question의 all 생명주기를 단계별로 제어', '* - Control all lifecycle of question by phases'),
        (r'// vote 커밋 단계 \(해시 제출 가능\)', '// Vote commit phase (hash submission allowed)'),
        (r'// vote public 단계 \(select public 가능\)', '// Vote reveal phase (choice reveal allowed)'),
        (r'// vote public 단계 end', '// Vote reveal phase end'),
        (r'// betting 단계 \(betting 가능\)', '// Betting phase (betting allowed)'),
        (r'// 리워드 분배 done', '// Reward distribution done'),

        # Order type comments
        (r'// 지정가 order \(오더북 적재\)', '// Limit order (placed in orderbook)'),
        (r'// 시장가 IOC order \(즉시 fill, 미fill분 cancel\)', '// Market IOC order (immediate fill, cancel unfilled)'),
        (r'// 매수: USDC yes치, position 증가', '// Buy: pay USDC, increase position'),
        (r'// 매도: position 담보, USDC 수령', '// Sell: collateralize position, receive USDC'),

        # Sports match comments
        (r'// API에서 받아온 경기 ID', '// Match ID from API'),
        (r'// EPL, La Liga, NBA 등', '// EPL, La Liga, NBA, etc.'),
        (r'// betting resume 시간', '// Betting resume time'),

        # Swap service patterns
        (r'SELL 시 sharesIn은', 'sharesIn for SELL'),
        (r'BUY 시 usdcIn은', 'usdcIn for BUY'),
        (r'shares입니다\.', 'shares.'),
        (r'shares\."', 'shares."'),
        (r'USDC입니다\.', 'USDC.'),
        (r'USDC\."', 'USDC."'),

        # Partial translations
        (r'경기 ID', 'match ID'),
        (r'등으로', 'etc.'),
        (r'스포츠 result', 'sports results'),
        (r'vote result', 'vote results'),
        (r'외부 data', 'external data'),
        (r'스포츠, 주가 등', 'sports, stocks, etc.'),

        # Additional patterns
        (r'failure', 'failure'),
        (r'cancel됨', 'cancelled'),
        (r'가능한', 'available'),
        (r'제출 가능', 'submission allowed'),
        (r'public 가능', 'reveal allowed'),
        (r'resume 시간', 'resume time'),
        (r'result 기반', 'result-based'),
        (r'결과 기반', 'result-based'),
        (r'즉시 fill', 'immediate fill'),
        (r'미fill분', 'unfilled portion'),
        (r'yes치', 'pay'),
        (r'담보', 'collateralize'),
        (r'수령', 'receive'),
        (r'받아온', 'from'),
        (r'받아온', 'received from'),
        (r'분배 done', 'distribution done'),

        # Any remaining Korean words
        (r'단계', 'phase'),
        (r'제어', 'control'),
        (r'생명주기', 'lifecycle'),
        (r'커밋', 'commit'),
        (r'공개', 'reveal'),
        (r'해시', 'hash'),
        (r'선택', 'choice'),
        (r'종료', 'end'),
        (r'분배', 'distribution'),
        (r'완료', 'done'),
        (r'지정가', 'limit'),
        (r'시장가', 'market'),
        (r'오더북', 'orderbook'),
        (r'적재', 'placement'),
        (r'즉시', 'immediate'),
        (r'체결', 'fill'),
        (r'미체결분', 'unfilled portion'),
        (r'매수', 'buy'),
        (r'매도', 'sell'),
        (r'지불', 'pay'),
        (r'증가', 'increase'),
        (r'감소', 'decrease'),
        (r'경기', 'match'),
        (r'리그', 'league'),
        (r'재개', 'resume'),
        (r'중단', 'suspend'),
        (r'일시중지', 'suspend'),
        (r'블록체인', 'blockchain'),
        (r'실패', 'failure'),
        (r'오류', 'error'),
        (r'검증', 'verification'),
        (r'의견', 'opinion'),
        (r'레거시', 'legacy'),
        (r'모델', 'model'),
        (r'및', 'and'),
        (r'기반', 'based'),
        (r'등', 'etc'),
        (r'주가', 'stock'),
        (r'주식', 'stock'),
        (r'결과', 'result'),
        (r'시장', 'market'),
    ]

    for pattern, replacement in replacements:
        content = re.sub(pattern, replacement, content)

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
        content = final_cleanup(content)

        # Only write if content changed
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)

            # Check if Korean still remains
            if re.search(r'[가-힣]', content):
                print(f"WARNING: Korean still remains in {filepath}")
                # Show remaining Korean
                lines = content.split('\n')
                for i, line in enumerate(lines, 1):
                    if re.search(r'[가-힣]', line):
                        print(f"  Line {i}: {line.strip()}")
            else:
                print(f"Processed: {filepath}")
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

    # Find all Kotlin files with Korean
    for root, dirs, files in os.walk(backend_dir):
        for file in files:
            if file.endswith('.kt'):
                filepath = os.path.join(root, file)
                total += 1
                if process_file(filepath):
                    processed += 1

    print(f"\nTotal files: {total}")
    print(f"Processed files: {processed}")

    # Count remaining Korean files
    remaining = 0
    for root, dirs, files in os.walk(backend_dir):
        for file in files:
            if file.endswith('.kt'):
                filepath = os.path.join(root, file)
                with open(filepath, 'r', encoding='utf-8') as f:
                    if re.search(r'[가-힣]', f.read()):
                        remaining += 1

    print(f"Remaining files with Korean: {remaining}")

    return 0

if __name__ == "__main__":
    sys.exit(main())
