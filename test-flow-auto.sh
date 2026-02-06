#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

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

extract_json() {
    echo "$1" | jq -r "$2" 2>/dev/null
}

# jq 확인
if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: jq is not installed. Install: brew install jq${NC}"
    exit 1
fi

print_header "전체 플로우 자동 테스트 시작"

# ============================================
# Step 0: 테스트 유저 자동 생성
# ============================================
print_step "0" "테스트 유저 자동 생성"

TIMESTAMP=$(date +%s)

# User 1 생성
USER1_RESPONSE=$(curl -s -X POST "$BASE_URL/api/members" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test_auto_user1_'$TIMESTAMP'@test.com",
    "countryCode": "KR",
    "jobCategory": "TECH",
    "ageGroup": 30
  }')

USER1_ID=$(extract_json "$USER1_RESPONSE" '.memberId')

if [ ! -z "$USER1_ID" ] && [ "$USER1_ID" != "null" ]; then
    test_result 0 "User1 생성: ID=$USER1_ID"
else
    test_result 1 "User1 생성 실패"
    echo "Response: $USER1_RESPONSE"
    exit 1
fi

# User 2 생성
USER2_RESPONSE=$(curl -s -X POST "$BASE_URL/api/members" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test_auto_user2_'$TIMESTAMP'@test.com",
    "countryCode": "KR",
    "jobCategory": "FINANCE",
    "ageGroup": 25
  }')

USER2_ID=$(extract_json "$USER2_RESPONSE" '.memberId')

if [ ! -z "$USER2_ID" ] && [ "$USER2_ID" != "null" ]; then
    test_result 0 "User2 생성: ID=$USER2_ID"
else
    test_result 1 "User2 생성 실패"
    exit 1
fi

# User 3 생성
USER3_RESPONSE=$(curl -s -X POST "$BASE_URL/api/members" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test_auto_user3_'$TIMESTAMP'@test.com",
    "countryCode": "KR",
    "jobCategory": "MARKETING",
    "ageGroup": 35
  }')

USER3_ID=$(extract_json "$USER3_RESPONSE" '.memberId')

if [ ! -z "$USER3_ID" ] && [ "$USER3_ID" != "null" ]; then
    test_result 0 "User3 생성: ID=$USER3_ID"
else
    test_result 1 "User3 생성 실패"
    exit 1
fi

echo -e "\n생성된 유저:"
echo "  User1: $USER1_ID (초기 잔액: 10,000P)"
echo "  User2: $USER2_ID (초기 잔액: 10,000P)"
echo "  User3: $USER3_ID (초기 잔액: 10,000P)"

# ============================================
# Step 1: OPINION 타입 질문 생성
# ============================================
print_step "1" "OPINION 타입 질문 생성"

QUESTION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/admin/questions" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "자동 테스트 - 양자컴퓨터가 2026년 내에 상용화될까?",
    "type": "OPINION",
    "category": "TECH",
    "votingDuration": 60,
    "bettingDuration": 60
  }')

QUESTION_ID=$(extract_json "$QUESTION_RESPONSE" '.questionId')

if [ ! -z "$QUESTION_ID" ] && [ "$QUESTION_ID" != "null" ]; then
    test_result 0 "질문 생성: ID=$QUESTION_ID"
else
    test_result 1 "질문 생성 실패"
    echo "Response: $QUESTION_RESPONSE"
    exit 1
fi

sleep 2
STATUS=$(curl -s "$BASE_URL/api/questions/$QUESTION_ID" | jq -r '.status')
[ "$STATUS" == "VOTING" ] && test_result 0 "질문 상태: VOTING" || test_result 1 "질문 상태 오류: $STATUS"

# ============================================
# Step 2: 투표
# ============================================
print_step "2" "투표 진행"

VOTE1=$(curl -s -X POST "$BASE_URL/api/vote" \
  -H "Content-Type: application/json" \
  -d '{"memberId": '$USER1_ID', "questionId": '$QUESTION_ID', "choice": "YES"}')
VOTE1_SUCCESS=$(extract_json "$VOTE1" '.success')
[ "$VOTE1_SUCCESS" == "true" ] && test_result 0 "User1 투표: YES" || test_result 1 "User1 투표 실패: $(extract_json "$VOTE1" '.message')"

VOTE2=$(curl -s -X POST "$BASE_URL/api/vote" \
  -H "Content-Type: application/json" \
  -d '{"memberId": '$USER2_ID', "questionId": '$QUESTION_ID', "choice": "YES"}')
VOTE2_SUCCESS=$(extract_json "$VOTE2" '.success')
[ "$VOTE2_SUCCESS" == "true" ] && test_result 0 "User2 투표: YES" || test_result 1 "User2 투표 실패"

VOTE3=$(curl -s -X POST "$BASE_URL/api/vote" \
  -H "Content-Type: application/json" \
  -d '{"memberId": '$USER3_ID', "questionId": '$QUESTION_ID', "choice": "NO"}')
VOTE3_SUCCESS=$(extract_json "$VOTE3" '.success')
[ "$VOTE3_SUCCESS" == "true" ] && test_result 0 "User3 투표: NO" || test_result 1 "User3 투표 실패"

echo -e "\n투표 결과: YES 2표 > NO 1표 → 예상 정산: YES"

# ============================================
# Step 2-1: 중복 투표 테스트
# ============================================
print_step "2-1" "중복 투표 테스트"

DUP=$(curl -s -X POST "$BASE_URL/api/vote" \
  -H "Content-Type: application/json" \
  -d '{"memberId": '$USER1_ID', "questionId": '$QUESTION_ID', "choice": "NO"}')
DUP_SUCCESS=$(extract_json "$DUP" '.success')
[ "$DUP_SUCCESS" == "false" ] && test_result 0 "중복 투표 거부" || test_result 1 "중복 투표 허용됨"

# ============================================
# Step 3: VOTING → BREAK 대기
# ============================================
print_step "3" "VOTING → BREAK 대기 (60초)"

echo "60초 대기 중..."
for i in {60..1}; do
    [ $((i % 10)) -eq 0 ] && echo -ne "\r  ${i}초 남음...   "
    sleep 1
done
echo -e "\n"

for i in {1..70}; do
    sleep 1
    STATUS=$(curl -s "$BASE_URL/api/questions/$QUESTION_ID" | jq -r '.status')
    [ $((i % 5)) -eq 0 ] && echo -ne "\r  체크 ${i}초: $STATUS   "
    [ "$STATUS" == "BREAK" ] && break
done
echo ""

[ "$STATUS" == "BREAK" ] && test_result 0 "BREAK 전환" || test_result 1 "BREAK 전환 실패: $STATUS"

# ============================================
# Step 4: BREAK에서 베팅 시도
# ============================================
print_step "4" "BREAK 상태 베팅 시도"

BET_BREAK=$(curl -s -X POST "$BASE_URL/api/bet" \
  -H "Content-Type: application/json" \
  -d '{"memberId": '$USER1_ID', "questionId": '$QUESTION_ID', "choice": "YES", "amount": 100}')
BET_BREAK_SUCCESS=$(extract_json "$BET_BREAK" '.success')
[ "$BET_BREAK_SUCCESS" == "false" ] && test_result 0 "BREAK 베팅 거부" || test_result 1 "BREAK 베팅 허용됨"

# ============================================
# Step 5: BREAK → BETTING 대기
# ============================================
print_step "5" "BREAK → BETTING 대기 (300초)"

echo "300초 대기 중..."
for i in {300..1}; do
    [ $((i % 30)) -eq 0 ] && echo -ne "\r  ${i}초 남음...   "
    sleep 1
done
echo -e "\n"

for i in {1..70}; do
    sleep 1
    STATUS=$(curl -s "$BASE_URL/api/questions/$QUESTION_ID" | jq -r '.status')
    [ $((i % 5)) -eq 0 ] && echo -ne "\r  체크 ${i}초: $STATUS   "
    [ "$STATUS" == "BETTING" ] && break
done
echo ""

[ "$STATUS" == "BETTING" ] && test_result 0 "BETTING 전환" || test_result 1 "BETTING 전환 실패: $STATUS"

# ============================================
# Step 6: 베팅
# ============================================
print_step "6" "베팅 진행"

USER1_BEFORE=$(curl -s "$BASE_URL/api/members/$USER1_ID" | jq -r '.pointBalance')
echo "User1 베팅 전: ${USER1_BEFORE}P"

BET1=$(curl -s -X POST "$BASE_URL/api/bet" \
  -H "Content-Type: application/json" \
  -d '{"memberId": '$USER1_ID', "questionId": '$QUESTION_ID', "choice": "YES", "amount": 100}')
BET1_SUCCESS=$(extract_json "$BET1" '.success')
[ "$BET1_SUCCESS" == "true" ] && test_result 0 "User1 베팅: YES 100P" || test_result 1 "User1 베팅 실패: $(extract_json "$BET1" '.message')"

BET2=$(curl -s -X POST "$BASE_URL/api/bet" \
  -H "Content-Type: application/json" \
  -d '{"memberId": '$USER2_ID', "questionId": '$QUESTION_ID', "choice": "YES", "amount": 100}')
BET2_SUCCESS=$(extract_json "$BET2" '.success')
[ "$BET2_SUCCESS" == "true" ] && test_result 0 "User2 베팅: YES 100P" || test_result 1 "User2 베팅 실패"

BET3=$(curl -s -X POST "$BASE_URL/api/bet" \
  -H "Content-Type: application/json" \
  -d '{"memberId": '$USER3_ID', "questionId": '$QUESTION_ID', "choice": "NO", "amount": 100}')
BET3_SUCCESS=$(extract_json "$BET3" '.success')
[ "$BET3_SUCCESS" == "true" ] && test_result 0 "User3 베팅: NO 100P" || test_result 1 "User3 베팅 실패"

USER1_AFTER=$(curl -s "$BASE_URL/api/members/$USER1_ID" | jq -r '.pointBalance')
USER1_DIFF=$((USER1_BEFORE - USER1_AFTER))
echo "User1 베팅 후: ${USER1_AFTER}P (차감: ${USER1_DIFF}P)"
[ $USER1_DIFF -eq 100 ] && test_result 0 "포인트 차감" || test_result 1 "포인트 차감 오류"

echo -e "\n베팅 풀: YES 80P, NO 100P, Total 160P"
echo "수수료 1%: 1.6P, 투표자 보상 0.5%: 0.8P"

# ============================================
# Step 6-1: 한도 초과 테스트
# ============================================
print_step "6-1" "한도 초과 베팅 테스트"

OVER=$(curl -s -X POST "$BASE_URL/api/bet" \
  -H "Content-Type: application/json" \
  -d '{"memberId": '$USER1_ID', "questionId": '$QUESTION_ID', "choice": "YES", "amount": 100000}')
OVER_SUCCESS=$(extract_json "$OVER" '.success')
[ "$OVER_SUCCESS" == "false" ] && test_result 0 "한도 초과 거부" || test_result 1 "한도 초과 허용됨"

# ============================================
# Step 7: 정산 대기
# ============================================
print_step "7" "정산 대기 (60초)"

echo "60초 대기 중..."
for i in {60..1}; do
    [ $((i % 10)) -eq 0 ] && echo -ne "\r  ${i}초 남음...   "
    sleep 1
done
echo -e "\n"

for i in {1..70}; do
    sleep 1
    QUESTION_DATA=$(curl -s "$BASE_URL/api/questions/$QUESTION_ID")
    STATUS=$(extract_json "$QUESTION_DATA" '.status')
    RESULT=$(extract_json "$QUESTION_DATA" '.finalResult')
    [ $((i % 5)) -eq 0 ] && echo -ne "\r  체크 ${i}초: status=$STATUS, result=$RESULT   "
    [ "$STATUS" == "SETTLED" ] && break
done
echo ""

[ "$STATUS" == "SETTLED" ] && test_result 0 "SETTLED 전환" || test_result 1 "SETTLED 전환 실패"

# ============================================
# Step 8: 정산 결과 확인
# ============================================
print_step "8" "정산 결과 확인"

sleep 5

FINAL_Q=$(curl -s "$BASE_URL/api/questions/$QUESTION_ID")
FINAL_RESULT=$(extract_json "$FINAL_Q" '.finalResult')
SOURCE_URL=$(extract_json "$FINAL_Q" '.sourceUrl')

echo -e "\n정산 결과: $FINAL_RESULT"
echo "정산 소스: $SOURCE_URL"

[ "$FINAL_RESULT" == "YES" ] && test_result 0 "정산 결과: YES" || test_result 1 "정산 결과 오류: $FINAL_RESULT"
[ "$SOURCE_URL" == "AUTO_SETTLEMENT_OPINION" ] && test_result 0 "정산 소스: AUTO_SETTLEMENT_OPINION" || test_result 1 "정산 소스 오류: $SOURCE_URL"

USER1_FINAL=$(curl -s "$BASE_URL/api/members/$USER1_ID" | jq -r '.pointBalance')
USER2_FINAL=$(curl -s "$BASE_URL/api/members/$USER2_ID" | jq -r '.pointBalance')
USER3_FINAL=$(curl -s "$BASE_URL/api/members/$USER3_ID" | jq -r '.pointBalance')

echo -e "\n최종 잔액:"
echo "  User1: ${USER1_FINAL}P (변화: $((USER1_FINAL - USER1_BEFORE))P)"
echo "  User2: ${USER2_FINAL}P (변화: $((USER2_FINAL - 10000))P)"
echo "  User3: ${USER3_FINAL}P (변화: $((USER3_FINAL - 10000))P)"

# 승자 확인 (YES가 이겼으므로 User1, User2가 배당금 받아야 함)
if [ $USER1_FINAL -gt $USER1_BEFORE ]; then
    test_result 0 "User1 (승자) 배당금 지급"
else
    test_result 1 "User1 배당금 미지급"
fi

if [ $USER2_FINAL -gt 10000 ]; then
    test_result 0 "User2 (승자) 배당금 지급"
else
    test_result 1 "User2 배당금 미지급"
fi

# User3는 패자이지만 투표 보상은 받음
if [ $USER3_FINAL -lt 10000 ]; then
    test_result 0 "User3 (패자) 베팅 손실"
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
    echo -e "\n${RED}✗ 일부 테스트 실패 (${FAILED_TESTS}개)${NC}"
    exit 1
fi
