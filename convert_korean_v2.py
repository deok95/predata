#!/usr/bin/env python3
"""
Convert Korean strings to English in Kotlin backend files - Enhanced version
"""

import re
import os
import sys

def translate_korean_comprehensive(content):
    """Comprehensive Korean to English translation"""

    # First pass: Direct replacements (most specific first)
    replacements = [
        # Comments - order matters (most specific first)
        (r'// 자동 question create 배치 실행 요청', '// Auto question creation batch execution request'),
        (r'subcategory 미지정 시 첫 번째 subcategory 사용', 'Use first subcategory if not specified'),
        (r'스케줄러에서 모든 subcategories 순회', 'scheduler iterates all subcategories'),
        (r'dryRun=true일 경우 DB 저장 없이 초안만 create', 'If dryRun=true, only create draft without DB save'),
        (r'자동 question create 결과 응답', 'Auto question creation result response'),
        (r'create된 question 초안 DTO', 'Created question draft DTO'),
        (r'트렌드 기반 입력 신호 DTO', 'Trend-based input signal DTO'),
        (r'LLM 호출용 정규화 요청 DTO', 'Normalization request DTO for LLM call'),
        (r'LLM 응답\(구조화 JSON\) DTO', 'LLM response (structured JSON) DTO'),
        (r'배치 검증 결과 DTO', 'Batch validation result DTO'),
        (r'운영 재시도/게시 명령 DTO', 'Operations retry/publish command DTO'),

        # Validation messages
        (r'는 is required\.', ' is required.'),
        (r'는 is required\."', ' is required."'),
        (r'는 0 or more이어야 합니다\.', ' must be 0 or more.'),
        (r'는 0 or more이어야 합니다\."', ' must be 0 or more."'),
        (r'는 100 \.', ' must be at most 100.'),
        (r'는 100 \."', ' must be at most 100."'),
        (r'는 비어 있을 수 없습니다\.', ' cannot be empty.'),
        (r'는 비어 있을 수 없습니다\."', ' cannot be empty."'),
        (r'가 비어 있을 수 없습니다\.', ' cannot be empty.'),
        (r'가 비어 있을 수 없습니다\."', ' cannot be empty."'),

        # Common phrases that got partially translated
        (r'SELL 시 sharesIn은 is required\.', 'sharesIn is required for SELL.'),
        (r'BUY 시 usdcIn은 is required\.', 'usdcIn is required for BUY.'),
        (r'Minimum trade amount is \$MIN_AMOUNT shares입니다\.', 'Minimum trade amount is \$MIN_AMOUNT shares.'),
        (r'UserShares 확인 \(pessimistic lock\)', 'Check UserShares (pessimistic lock)'),
        (r'Before 스냅샷 캡처 \(pool 업데이트 전\)', 'Capture before snapshot (before pool update)'),
        (r'c_out_gross 계산 \(net \+ fee\)', 'Calculate c_out_gross (net + fee)'),
        (r'member USDC 증가', 'Increase member USDC'),
        (r'UserShares 업데이트', 'Update UserShares'),
        (r'costBasis 비례 차감', 'Proportional costBasis reduction'),
        (r'SwapHistory 기록', 'Record SwapHistory'),
        (r'현재 유저의 전체 shares retrieve', 'Retrieve current user\'s total shares'),
        (r'풀 상태 retrieve', 'Retrieve pool state'),
        (r'내 position retrieve', 'Retrieve my position'),
        (r'가격 히스토리 retrieve', 'Retrieve price history'),
        (r'swap_history에서 가격 변동 데이터를 가져옴', 'Fetch price change data from swap_history'),
        (r'question 존재 여부 확인 - 없으면 빈 리스트 반환', 'Check question existence - return empty list if not found'),
        (r'풀 존재 여부 확인 - 없으면 빈 리스트 반환 \(ORDERBOOK_LEGACY 등\)', 'Check pool existence - return empty list if not found (ORDERBOOK_LEGACY, etc.)'),
        (r'swap_history에서 오름차순으로 스왑 retrieve', 'Retrieve swaps in ascending order from swap_history'),
        (r'시드 시점 \(첫 포인트\)', 'Seed time (first point)'),
        (r'스왑 히스토리 retrieve \(공개 - 인증 불필요\)', 'Retrieve swap history (public - no auth required)'),
        (r'최근 활동 피드에 사용', 'Used in recent activity feed'),
        (r'내 스왑 히스토리 retrieve \(인증 필요\)', 'Retrieve my swap history (auth required)'),
        (r'"내 거래" 탭에 사용', 'Used in "My Trades" tab'),

        # Comment patterns
        (r'// GET /api/questions/\*\* 패턴만 인증 제외 \(POST/PUT/DELETE는 인증 필요\)', '// Exclude auth only for GET /api/questions/** pattern (POST/PUT/DELETE require auth)'),
        (r'// POST /api/questions/\{id\}/view 는 공개 엔드포인트 \(retrieve수 집계\) - 인증 제외', '// POST /api/questions/{id}/view is public endpoint (view count aggregation) - exclude auth'),
        (r'// GET /api/members/\{숫자ID\} 패턴만 인증 제외 \(me, by-email 등은 인증 필요\)', '// Exclude auth only for GET /api/members/{numeric ID} pattern (me, by-email require auth)'),

        # Fee rate validation
        (r'Fee rate must be \[0, 1\) must be in range\.', 'Fee rate must be in range [0, 1).'),

        # Other patterns
        (r'이하여야 합니다\.', ' or less.'),
        (r'이하여야 합니다\."', ' or less."'),
        (r'이어야 합니다\.', '.'),
        (r'이어야 합니다\."', '."'),
        (r'여야 합니다\.', '.'),
        (r'여야 합니다\."', '."'),
    ]

    for pattern, replacement in replacements:
        content = re.sub(pattern, replacement, content)

    # Second pass: General Korean character removal/translation
    # This catches any remaining Korean characters not handled above
    korean_patterns = {
        '조회': 'retrieve',
        '생성': 'create',
        '수정': 'update',
        '삭제': 'delete',
        '정산': 'settlement',
        '베팅': 'betting',
        '투표': 'vote',
        '질문': 'question',
        '회원': 'member',
        '주문': 'order',
        '체결': 'fill',
        '포지션': 'position',
        '잔액': 'balance',
        '확인': 'check',
        '검증': 'validate',
        '실행': 'execute',
        '처리': 'process',
        '기록': 'record',
        '초안': 'draft',
        '배치': 'batch',
        '결과': 'result',
        '응답': 'response',
        '요청': 'request',
        '명령': 'command',
        '검색': 'search',
        '필터': 'filter',
        '정렬': 'sort',
        '페이지': 'page',
        '목록': 'list',
        '상세': 'detail',
        '통계': 'statistics',
        '분석': 'analysis',
        '보고서': 'report',
        '알림': 'notification',
        '설정': 'settings',
        '프로필': 'profile',
        '계정': 'account',
        '권한': 'permission',
        '역할': 'role',
        '그룹': 'group',
        '팀': 'team',
        '프로젝트': 'project',
        '작업': 'task',
        '이벤트': 'event',
        '로그': 'log',
        '히스토리': 'history',
        '캐시': 'cache',
        '세션': 'session',
        '토큰': 'token',
        '쿠키': 'cookie',
        '헤더': 'header',
        '바디': 'body',
        '파라미터': 'parameter',
        '옵션': 'option',
        '값': 'value',
        '키': 'key',
        '타입': 'type',
        '형식': 'format',
        '구조': 'structure',
        '패턴': 'pattern',
        '규칙': 'rule',
        '정책': 'policy',
        '전략': 'strategy',
        '알고리즘': 'algorithm',
        '함수': 'function',
        '메서드': 'method',
        '클래스': 'class',
        '인터페이스': 'interface',
        '모델': 'model',
        '엔티티': 'entity',
        '서비스': 'service',
        '컨트롤러': 'controller',
        '리포지토리': 'repository',
        '데이터': 'data',
        '정보': 'information',
        '내용': 'content',
        '메시지': 'message',
        '에러': 'error',
        '예외': 'exception',
        '경고': 'warning',
        '성공': 'success',
        '실패': 'failure',
        '완료': 'complete',
        '진행': 'progress',
        '대기': 'pending',
        '취소': 'cancel',
        '시작': 'start',
        '종료': 'end',
        '중지': 'stop',
        '일시정지': 'pause',
        '재시작': 'restart',
        '재개': 'resume',
        '새로고침': 'refresh',
        '갱신': 'renew',
        '동기화': 'sync',
        '백업': 'backup',
        '복원': 'restore',
        '내보내기': 'export',
        '가져오기': 'import',
        '업로드': 'upload',
        '다운로드': 'download',
        '전송': 'send',
        '수신': 'receive',
        '연결': 'connect',
        '연결해제': 'disconnect',
        '등록': 'register',
        '해제': 'unregister',
        '활성화': 'activate',
        '비활성화': 'deactivate',
        '사용': 'enable',
        '미사용': 'disable',
        '허용': 'allow',
        '차단': 'block',
        '금지': 'forbidden',
        '제한': 'limit',
        '제약': 'constraint',
        '조건': 'condition',
        '기준': 'criteria',
        '요구사항': 'requirement',
        '필수': 'required',
        '선택': 'optional',
        '기본': 'default',
        '사용자정의': 'custom',
        '자동': 'auto',
        '수동': 'manual',
        '공개': 'public',
        '비공개': 'private',
        '공유': 'shared',
        '전체': 'all',
        '부분': 'partial',
        '일부': 'some',
        '없음': 'none',
        '비어있음': 'empty',
        '존재': 'exists',
        '포함': 'contains',
        '제외': 'excludes',
        '일치': 'match',
        '불일치': 'mismatch',
        '같음': 'equals',
        '다름': 'differs',
        '크다': 'greater',
        '작다': 'less',
        '이상': 'or more',
        '이하': 'or less',
        '초과': 'exceeds',
        '미만': 'less than',
        '범위': 'range',
        '최소': 'minimum',
        '최대': 'maximum',
        '평균': 'average',
        '합계': 'sum',
        '개수': 'count',
        '총': 'total',
        '순': 'net',
        '전': 'before',
        '후': 'after',
        '이전': 'previous',
        '다음': 'next',
        '첫': 'first',
        '마지막': 'last',
        '현재': 'current',
        '최신': 'latest',
        '오래된': 'oldest',
        '새': 'new',
        '기존': 'existing',
        '변경': 'change',
        '추가': 'add',
        '제거': 'remove',
        '교체': 'replace',
        '복사': 'copy',
        '이동': 'move',
        '병합': 'merge',
        '분할': 'split',
        '그룹화': 'group',
        '정렬': 'sort',
        '필터링': 'filter',
        '검색': 'search',
        '찾기': 'find',
        '보기': 'view',
        '편집': 'edit',
        '저장': 'save',
        '불러오기': 'load',
        '닫기': 'close',
        '열기': 'open',
        '접기': 'collapse',
        '펼치기': 'expand',
        '숨기기': 'hide',
        '보이기': 'show',
        '전환': 'toggle',
        '선택': 'select',
        '선택해제': 'deselect',
        '클릭': 'click',
        '더블클릭': 'double-click',
        '우클릭': 'right-click',
        '드래그': 'drag',
        '드롭': 'drop',
        '스크롤': 'scroll',
        '확대': 'zoom in',
        '축소': 'zoom out',
        '초기화': 'reset',
        '재설정': 'reset',
        '기본값': 'default',
        '설정값': 'configured value',
        '입력값': 'input value',
        '출력값': 'output value',
        '반환값': 'return value',
        '결과값': 'result value',
        '참': 'true',
        '거짓': 'false',
        '예': 'yes',
        '아니오': 'no',
        '확인': 'confirm',
        '취소': 'cancel',
        '적용': 'apply',
        '승인': 'approve',
        '거부': 'reject',
        '수락': 'accept',
        '거절': 'decline',
        '동의': 'agree',
        '동의안함': 'disagree',
        '계속': 'continue',
        '건너뛰기': 'skip',
        '완료': 'done',
        '실행': 'execute',
        '중단': 'abort',
        '재시도': 'retry',
        '무시': 'ignore',
        '경고무시': 'ignore warning',
    }

    # Apply general Korean word translations
    for korean, english in korean_patterns.items():
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
        content = translate_korean_comprehensive(content)

        # Only write if content changed
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)

            # Check if Korean still remains
            if re.search(r'[가-힣]', content):
                print(f"WARNING: Korean still remains in {filepath}", file=sys.stderr)
                # Show remaining Korean
                lines = content.split('\n')
                for i, line in enumerate(lines, 1):
                    if re.search(r'[가-힣]', line):
                        print(f"  Line {i}: {line.strip()}", file=sys.stderr)
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

    # Find all Kotlin files
    for root, dirs, files in os.walk(backend_dir):
        for file in files:
            if file.endswith('.kt'):
                filepath = os.path.join(root, file)
                total += 1
                if process_file(filepath):
                    processed += 1

    print(f"\nTotal files: {total}")
    print(f"Processed files: {processed}")

    return 0

if __name__ == "__main__":
    sys.exit(main())
