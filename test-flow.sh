#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# API Base URL
BASE_URL="http://localhost:8080"

# 테스트 카운터
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 유틸리티 함수
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_step() {
    echo -e "\n${YELLOW}[Step $1] $2${NC}"
}

test_result() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}[PASS]${NC} $2"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}[FAIL]${NC} $2"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# JSON 파싱 함수 (jq 사용)
extract_json() {
    echo "$1" | jq -r "$2" 2>/dev/null
}

# jq 설치 확인
if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: jq is not installed. Please install jq first.${NC}"
    echo "Install: brew install jq"
    exit 1
fi

print_header "전체 플로우 테스트 시작"
echo "Base URL: $BASE_URL"

# ============================================
# 준비: 테스트 유저 생성
# ============================================
print_step "0" "테스트 유저 생성"

# User 1 (BRONZE)
USER1_RESPONSE=$(curl -s -X POST "$BASE_URL/api/members" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test_user1_'$(date +%s)'@test.com",
    "countryCode": "KR",
    "tier": "BRONZE"
  }')
USER1_ID=$(extract_json "$USER1_RESPONSE" '.data.id')

if [ ! -z "$USER1_ID" ] && [ "$USER1_ID" != "null" ]; then
    test_result 0 "User1 (BRONZE) 생성 완료: ID=$USER1_ID"
else
    test_result 1 "User1 생성 실패"
    echo "Response: $USER1_RESPONSE"
    exit 1
fi

# User 2 (GOLD) - 먼저 생성 후 티어 수동 업데이트 필요
USER2_RESPONSE=$(curl -s -X POST "$BASE_URL/api/members" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test_user2_'$(date +%s)'@test.com",
    "countryCode": "KR",
    "tier": "GOLD"
  }')
USER2_ID=$(extract_json "$USER2_RESPONSE" '.data.id')

if [ ! -z "$USER2_ID" ] && [ "$USER2_ID" != "null" ]; then
    test_result 0 "User2 (GOLD) 생성 완료: ID=$USER2_ID"
else
    test_result 1 "User2 생성 실패"
    echo "Response: $USER2_RESPONSE"
    exit 1
fi

# User 3 (PLATINUM)
USER3_RESPONSE=$(curl -s -X POST "$BASE_URL/api/members" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test_user3_'$(date +%s)'@test.com",
    "countryCode": "KR",
    "tier": "PLATINUM"
  }')
USER3_ID=$(extract_json "$USER3_RESPONSE" '.data.id')

if [ ! -z "$USER3_ID" ] && [ "$USER3_ID" != "null" ]; then
    test_result 0 "User3 (PLATINUM) 생성 완료: ID=$USER3_ID"
else
    test_result 1 "User3 생성 실패"
    echo "Response: $USER3_RESPONSE"
    exit 1
fi

echo -e "\n생성된 유저:"
echo "  User1 (BRONZE): $USER1_ID"
echo "  User2 (GOLD): $USER2_ID"
echo "  User3 (PLATINUM): $USER3_ID"

# ============================================
# Step 1: OPINION 타입 질문 생성
# ============================================
print_step "1" "OPINION 타입 질문 생성"

QUESTION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/admin/questions" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "자동 테스트 - 비트코인이 2026년 말까지 20만 달러를 돌파할까?",
    "type": "OPINION",
    "category": "TECH",
    "votingDuration": 60,
    "bettingDuration": 60
  }')

QUESTION_ID=$(extract_json "$QUESTION_RESPONSE" '.questionId')
QUESTION_STATUS=$(extract_json "$QUESTION_RESPONSE" '.message')

if [ ! -z "$QUESTION_ID" ] && [ "$QUESTION_ID" != "null" ]; then
    test_result 0 "질문 생성 완료: ID=$QUESTION_ID"
    echo "  Title: 비트코인이 2026년 말까지 20만 달러를 돌파할까?"
    echo "  Type: OPINION"
    echo "  Voting: 60초, Betting: 60초"
else
    test_result 1 "질문 생성 실패"
    echo "Response: $QUESTION_RESPONSE"
    exit 1
fi

# 질문 상태 확인
sleep 2
QUESTION_DATA=$(curl -s "$BASE_URL/api/questions/$QUESTION_ID")
STATUS=$(extract_json "$QUESTION_DATA" '.status')

if [ "$STATUS" == "VOTING" ]; then
    test_result 0 "질문 상태: VOTING"
else
    test_result 1 "질문 상태가 VOTING이 아님: $STATUS"
fi

# ============================================
# Step 2: 투표
# ============================================
print_step "2" "투표 진행"

# User1 → YES
VOTE1=$(curl -s -X POST "$BASE_URL/api/betting/vote" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$USER1_ID',
    "questionId": '$QUESTION_ID',
    "choice": "YES"
  }')
VOTE1_SUCCESS=$(extract_json "$VOTE1" '.success')

if [ "$VOTE1_SUCCESS" == "true" ]; then
    test_result 0 "User1 투표 성공: YES"
else
    test_result 1 "User1 투표 실패"
    echo "Response: $VOTE1"
fi

# User2 → YES
VOTE2=$(curl -s -X POST "$BASE_URL/api/betting/vote" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$USER2_ID',
    "questionId": '$QUESTION_ID',
    "choice": "YES"
  }')
VOTE2_SUCCESS=$(extract_json "$VOTE2" '.success')

if [ "$VOTE2_SUCCESS" == "true" ]; then
    test_result 0 "User2 투표 성공: YES"
else
    test_result 1 "User2 투표 실패"
    echo "Response: $VOTE2"
fi

# User3 → NO
VOTE3=$(curl -s -X POST "$BASE_URL/api/betting/vote" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$USER3_ID',
    "questionId": '$QUESTION_ID',
    "choice": "NO"
  }')
VOTE3_SUCCESS=$(extract_json "$VOTE3" '.success')

if [ "$VOTE3_SUCCESS" == "true" ]; then
    test_result 0 "User3 투표 성공: NO"
else
    test_result 1 "User3 투표 실패"
    echo "Response: $VOTE3"
fi

echo -e "\n투표 결과: YES 2표, NO 1표 → 예상 결과: YES"

# ============================================
# Step 2-1: 중복 투표 테스트
# ============================================
print_step "2-1" "중복 투표 테스트"

DUPLICATE_VOTE=$(curl -s -X POST "$BASE_URL/api/betting/vote" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$USER1_ID',
    "questionId": '$QUESTION_ID',
    "choice": "NO"
  }')
DUPLICATE_SUCCESS=$(extract_json "$DUPLICATE_VOTE" '.success')

if [ "$DUPLICATE_SUCCESS" == "false" ]; then
    test_result 0 "중복 투표 정상적으로 거부됨"
else
    test_result 1 "중복 투표가 허용됨 (오류)"
    echo "Response: $DUPLICATE_VOTE"
fi

# ============================================
# Step 3: 상태 전환 대기 (VOTING → BREAK)
# ============================================
print_step "3" "상태 전환 대기 (VOTING → BREAK → BETTING)"

echo -e "60초 대기 중 (투표 종료)..."
for i in {60..1}; do
    echo -ne "\r  ${i}초 남음...   "
    sleep 1
done
echo -e "\n"

# 상태 확인 (최대 70초까지 체크)
echo "BREAK 상태 확인 중..."
BREAK_FOUND=0
for i in {1..70}; do
    sleep 1
    QUESTION_DATA=$(curl -s "$BASE_URL/api/questions/$QUESTION_ID")
    STATUS=$(extract_json "$QUESTION_DATA" '.status')
    echo -ne "\r  체크 ${i}초: status=$STATUS   "
    if [ "$STATUS" == "BREAK" ]; then
        BREAK_FOUND=1
        break
    fi
done
echo ""

if [ $BREAK_FOUND -eq 1 ]; then
    test_result 0 "VOTING → BREAK 전환 완료"
else
    test_result 1 "BREAK 상태로 전환되지 않음: $STATUS"
fi

# ============================================
# Step 4: BREAK 상태에서 베팅 시도 (실패 예상)
# ============================================
print_step "4" "BREAK 상태에서 베팅 시도 (실패 예상)"

BET_IN_BREAK=$(curl -s -X POST "$BASE_URL/api/betting/bet" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$USER1_ID',
    "questionId": '$QUESTION_ID',
    "choice": "YES",
    "amount": 100
  }')
BET_IN_BREAK_SUCCESS=$(extract_json "$BET_IN_BREAK" '.success')

if [ "$BET_IN_BREAK_SUCCESS" == "false" ]; then
    test_result 0 "BREAK 상태에서 베팅 정상적으로 거부됨"
else
    test_result 1 "BREAK 상태에서 베팅이 허용됨 (오류)"
    echo "Response: $BET_IN_BREAK"
fi

# ============================================
# Step 5: BETTING 상태 전환 대기
# ============================================
echo -e "\n300초 대기 중 (베팅 시작)..."
for i in {300..1}; do
    echo -ne "\r  ${i}초 남음...   "
    sleep 1
done
echo -e "\n"

echo "BETTING 상태 확인 중..."
BETTING_FOUND=0
for i in {1..70}; do
    sleep 1
    QUESTION_DATA=$(curl -s "$BASE_URL/api/questions/$QUESTION_ID")
    STATUS=$(extract_json "$QUESTION_DATA" '.status')
    echo -ne "\r  체크 ${i}초: status=$STATUS   "
    if [ "$STATUS" == "BETTING" ]; then
        BETTING_FOUND=1
        break
    fi
done
echo ""

if [ $BETTING_FOUND -eq 1 ]; then
    test_result 0 "BREAK → BETTING 전환 완료"
else
    test_result 1 "BETTING 상태로 전환되지 않음: $STATUS"
fi

# ============================================
# Step 6: 베팅 진행
# ============================================
print_step "5" "베팅 진행"

# User1 잔액 확인
USER1_BEFORE=$(curl -s "$BASE_URL/api/members/$USER1_ID")
USER1_BALANCE_BEFORE=$(extract_json "$USER1_BEFORE" '.data.pointBalance')
echo "User1 베팅 전 잔액: ${USER1_BALANCE_BEFORE}P"

# User1 → YES 50P
BET1=$(curl -s -X POST "$BASE_URL/api/betting/bet" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$USER1_ID',
    "questionId": '$QUESTION_ID',
    "choice": "YES",
    "amount": 50
  }')
BET1_SUCCESS=$(extract_json "$BET1" '.success')

if [ "$BET1_SUCCESS" == "true" ]; then
    test_result 0 "User1 베팅 성공: YES 50P"
else
    test_result 1 "User1 베팅 실패"
    echo "Response: $BET1"
fi

# User2 → YES 30P
BET2=$(curl -s -X POST "$BASE_URL/api/betting/bet" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$USER2_ID',
    "questionId": '$QUESTION_ID',
    "choice": "YES",
    "amount": 30
  }')
BET2_SUCCESS=$(extract_json "$BET2" '.success')

if [ "$BET2_SUCCESS" == "true" ]; then
    test_result 0 "User2 베팅 성공: YES 30P"
else
    test_result 1 "User2 베팅 실패"
    echo "Response: $BET2"
fi

# User3 → NO 80P
BET3=$(curl -s -X POST "$BASE_URL/api/betting/bet" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$USER3_ID',
    "questionId": '$QUESTION_ID',
    "choice": "NO",
    "amount": 80
  }')
BET3_SUCCESS=$(extract_json "$BET3" '.success')

if [ "$BET3_SUCCESS" == "true" ]; then
    test_result 0 "User3 베팅 성공: NO 80P"
else
    test_result 1 "User3 베팅 실패"
    echo "Response: $BET3"
fi

# 잔액 확인
USER1_AFTER=$(curl -s "$BASE_URL/api/members/$USER1_ID")
USER1_BALANCE_AFTER=$(extract_json "$USER1_AFTER" '.data.pointBalance')
USER1_DIFF=$((USER1_BALANCE_BEFORE - USER1_BALANCE_AFTER))

echo -e "\nUser1 베팅 후 잔액: ${USER1_BALANCE_AFTER}P (차감: ${USER1_DIFF}P)"

if [ $USER1_DIFF -eq 50 ]; then
    test_result 0 "User1 포인트 정상 차감: 50P"
else
    test_result 1 "User1 포인트 차감 오류: 예상 50P, 실제 ${USER1_DIFF}P"
fi

echo -e "\n베팅 풀 현황:"
echo "  YES: 50 + 30 = 80P"
echo "  NO: 80P"
echo "  Total: 160P"
echo "  수수료 (1%): 1.6P"
echo "  투표자 보상 (수수료 50%): 0.8P"

# ============================================
# Step 6-1: 베팅 한도 초과 테스트
# ============================================
print_step "5-1" "베팅 한도 초과 테스트"

OVER_BET=$(curl -s -X POST "$BASE_URL/api/betting/bet" \
  -H "Content-Type: application/json" \
  -d '{
    "memberId": '$USER1_ID',
    "questionId": '$QUESTION_ID',
    "choice": "YES",
    "amount": 100000
  }')
OVER_BET_SUCCESS=$(extract_json "$OVER_BET" '.success')

if [ "$OVER_BET_SUCCESS" == "false" ]; then
    test_result 0 "한도 초과 베팅 정상적으로 거부됨"
else
    test_result 1 "한도 초과 베팅이 허용됨 (오류)"
    echo "Response: $OVER_BET"
fi

# ============================================
# Step 7: 정산 대기
# ============================================
print_step "6" "정산 대기 (베팅 종료 → SETTLED)"

echo -e "60초 대기 중 (베팅 종료)..."
for i in {60..1}; do
    echo -ne "\r  ${i}초 남음...   "
    sleep 1
done
echo -e "\n"

echo "SETTLED 상태 확인 중..."
SETTLED_FOUND=0
for i in {1..70}; do
    sleep 1
    QUESTION_DATA=$(curl -s "$BASE_URL/api/questions/$QUESTION_ID")
    STATUS=$(extract_json "$QUESTION_DATA" '.status')
    FINAL_RESULT=$(extract_json "$QUESTION_DATA" '.finalResult')
    echo -ne "\r  체크 ${i}초: status=$STATUS, result=$FINAL_RESULT   "
    if [ "$STATUS" == "SETTLED" ]; then
        SETTLED_FOUND=1
        break
    fi
done
echo ""

if [ $SETTLED_FOUND -eq 1 ]; then
    test_result 0 "BETTING → SETTLED 전환 완료"
else
    test_result 1 "SETTLED 상태로 전환되지 않음: $STATUS"
fi

# ============================================
# Step 8: 정산 결과 확인
# ============================================
print_step "7" "정산 결과 확인"

sleep 5  # 정산 완료 대기

# 질문 정보 조회
FINAL_QUESTION=$(curl -s "$BASE_URL/api/questions/$QUESTION_ID")
FINAL_RESULT=$(extract_json "$FINAL_QUESTION" '.finalResult')
SOURCE_URL=$(extract_json "$FINAL_QUESTION" '.sourceUrl')

echo -e "\n정산 결과:"
echo "  최종 결과: $FINAL_RESULT"
echo "  정산 소스: $SOURCE_URL"

# 투표 결과 기반 정산 확인 (YES 2표 > NO 1표)
if [ "$FINAL_RESULT" == "YES" ]; then
    test_result 0 "정산 결과: YES (투표 결과 반영 정상)"
else
    test_result 1 "정산 결과 오류: 예상 YES, 실제 $FINAL_RESULT"
fi

if [ "$SOURCE_URL" == "AUTO_SETTLEMENT_OPINION" ]; then
    test_result 0 "정산 소스: AUTO_SETTLEMENT_OPINION (OPINION 타입 자동 정산)"
else
    test_result 1 "정산 소스 오류: 예상 AUTO_SETTLEMENT_OPINION, 실제 $SOURCE_URL"
fi

# 유저 최종 잔액 조회
echo -e "\n최종 잔액 확인:"

USER1_FINAL=$(curl -s "$BASE_URL/api/members/$USER1_ID")
USER1_BALANCE_FINAL=$(extract_json "$USER1_FINAL" '.data.pointBalance')
echo "  User1: ${USER1_BALANCE_FINAL}P (승자, YES 50P 베팅 + 투표 보상)"

USER2_FINAL=$(curl -s "$BASE_URL/api/members/$USER2_ID")
USER2_BALANCE_FINAL=$(extract_json "$USER2_FINAL" '.data.pointBalance')
echo "  User2: ${USER2_BALANCE_FINAL}P (승자, YES 30P 베팅 + 투표 보상)"

USER3_FINAL=$(curl -s "$BASE_URL/api/members/$USER3_ID")
USER3_BALANCE_FINAL=$(extract_json "$USER3_FINAL" '.data.pointBalance')
echo "  User3: ${USER3_BALANCE_FINAL}P (패자, NO 80P 베팅 손실 + 투표 보상)"

# 승자 확인
if [ $USER1_BALANCE_FINAL -gt $USER1_BALANCE_BEFORE ]; then
    test_result 0 "User1 (승자) 배당금 지급 확인"
else
    test_result 1 "User1 배당금 미지급"
fi

if [ $USER2_BALANCE_FINAL -gt 10000 ]; then
    test_result 0 "User2 (승자) 배당금 지급 확인"
else
    test_result 1 "User2 배당금 미지급"
fi

# 패자 확인 (베팅 손실했지만 투표 보상은 받음)
if [ $USER3_BALANCE_FINAL -lt 10000 ]; then
    test_result 0 "User3 (패자) 베팅 손실 확인"
else
    test_result 1 "User3 베팅 손실 미반영"
fi

# ============================================
# 최종 결과
# ============================================
print_header "테스트 완료"

echo -e "\n${BLUE}전체 테스트 결과:${NC}"
echo -e "  전체: ${TOTAL_TESTS}개"
echo -e "  ${GREEN}성공: ${PASSED_TESTS}개${NC}"
echo -e "  ${RED}실패: ${FAILED_TESTS}개${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}✓ 모든 테스트 통과!${NC}"
    exit 0
else
    echo -e "\n${RED}✗ 일부 테스트 실패${NC}"
    exit 1
fi
