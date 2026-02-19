#!/bin/bash

# 질문 등록 스크립트
# 5분 투표(300초) + 5분 브레이크(자동) + 5분 베팅(300초)

API_URL="http://localhost:8080/api/admin/questions"

# 색상 코드
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "=== 질문 등록 시작 ==="
echo ""

# 1. sport-A
echo "1/18: 홈팀 승리 여부 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will the home team beat the away team?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Home team wins within regular time (90 minutes)",
    "resolutionSource": "Official match results",
    "category": "SPORTS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 2. sport-B
echo "2/18: 양팀 득점(BTTS) 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will both teams score at least one goal? (BTTS)",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Both home and away teams score at least one goal",
    "resolutionSource": "Official match results",
    "category": "SPORTS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 3. sport-C
echo "3/18: 3골 이상 득점 여부 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will this match have 3 or more total goals?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Combined goals are 3 or more (e.g., 3-0, 2-1, 2-2)",
    "resolutionSource": "Official match results",
    "category": "SPORTS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 4. sport-D
echo "4/18: 홈팀 무실점 여부 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will the home team keep a clean sheet?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Home team finishes with 0 goals conceded",
    "resolutionSource": "Official match results",
    "category": "SPORTS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 5. sport-E
echo "5/18: 홈팀 전반 선취골 여부 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will the home team score first in the first half?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Home team scores the first goal before halftime",
    "resolutionSource": "Official match results",
    "category": "SPORTS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 6. lifestyle-A
echo "6/18: Nike vs Adidas 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "In the 2026 S/S season, will Nike win more '\''Sneaker of the Year'\'' awards than Adidas?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Nike wins more major sneaker awards than Adidas in defined outlets",
    "resolutionSource": "Major sneaker media awards",
    "resolveAt": "2026-06-30T23:59:59",
    "category": "CULTURE",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 7. lifestyle-B
echo "7/18: BTS 솔로 Billboard Top 10 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will any BTS member'\''s solo release enter Billboard Hot 100 Top 10 in 2026?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "At least one BTS member reaches Hot 100 Top 10 in 2026 with solo music",
    "resolutionSource": "Billboard Hot 100 official chart",
    "resolveAt": "2026-12-31T23:59:59",
    "category": "CULTURE",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 8. lifestyle-C
echo "8/18: ChatGPT vs Claude 사용자 수 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "By the end of 2026, will ChatGPT have more monthly active users than Claude?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "ChatGPT MAU is greater than Claude MAU based on credible public data",
    "resolutionSource": "Official announcements or credible third-party analysis",
    "resolveAt": "2026-12-31T23:59:59",
    "category": "TECH",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 9. lifestyle-D
echo "9/18: 마블 영화 한국 박스오피스 1위 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will the next Marvel theatrical release rank #1 at the Korean box office on opening weekend?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Marvel release ranks #1 in Korea for opening weekend",
    "resolutionSource": "Korean Film Council official box office data",
    "category": "CULTURE",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 10. lifestyle-E
echo "10/18: 버블티 vs 탕후루 SNS 언급 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "In summer 2026, will bubble tea have more SNS mentions than tanghulu?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Bubble tea mentions exceed tanghulu mentions from Jun-Aug 2026 on Korean SNS (Instagram/X/TikTok)",
    "resolutionSource": "SNS analytics tools",
    "resolveAt": "2026-08-31T23:59:59",
    "category": "CULTURE",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 11. politics-A
echo "11/18: 러시아-우크라이나 휴전 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will Russia and Ukraine sign an official ceasefire agreement by March 31, 2026?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Both governments formally sign a ceasefire agreement by deadline",
    "resolutionSource": "Official government announcements and major media",
    "resolveAt": "2026-03-31T23:59:59",
    "category": "POLITICS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 12. politics-B
echo "12/18: 트럼프 EU 관세 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will Trump officially impose additional tariffs of 20% or more on the EU by March 31, 2026?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "A formal order/law enacting tariffs of at least 20% takes effect",
    "resolutionSource": "Official US government announcements",
    "resolveAt": "2026-03-31T23:59:59",
    "category": "POLITICS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 13. politics-C
echo "13/18: 비트코인 $120,000 돌파 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will Bitcoin break $120,000 by March 31, 2026?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "BTC reaches at least $120,000 on a designated reference exchange/data source",
    "resolutionSource": "Major cryptocurrency exchange data",
    "resolveAt": "2026-03-31T23:59:59",
    "category": "ECONOMY",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 14. politics-D
echo "14/18: 미국 연준 금리 인하 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will the U.S. Federal Reserve cut rates again by June 30, 2026?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "FOMC announces at least one 25bp or larger cut by deadline",
    "resolutionSource": "Federal Reserve official announcements",
    "resolveAt": "2026-06-30T23:59:59",
    "category": "ECONOMY",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 15. politics-E
echo "15/18: 일론 머스크 DOGE 사임 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will Elon Musk officially step down as DOGE (Department of Government Efficiency) head by March 31, 2026?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Official resignation/removal announcement by deadline",
    "resolutionSource": "Official US government announcements",
    "resolveAt": "2026-03-31T23:59:59",
    "category": "POLITICS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 16. politics-F
echo "16/18: TikTok 미국 복구 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will TikTok be fully restored in U.S. app stores by March 31, 2026?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "TikTok is re-listed and functioning normally in U.S. app stores by deadline",
    "resolutionSource": "App store verification",
    "resolveAt": "2026-03-31T23:59:59",
    "category": "POLITICS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 17. politics-G
echo "17/18: 한국 금투세 폐지 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Will South Korea fully repeal the Financial Investment Income Tax by March 31, 2026?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Full repeal bill passes and is promulgated by deadline",
    "resolutionSource": "National Assembly and government official announcements",
    "resolveAt": "2026-03-31T23:59:59",
    "category": "POLITICS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

# 18. politics-H
echo "18/18: 2026년 6월 지방선거 민주당 승리 등록 중..."
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "In the June 2026 local elections, will the Democratic Party win at least 9 of 17 metropolitan mayor/governor seats?",
    "type": "VERIFIABLE",
    "marketType": "VERIFIABLE",
    "resolutionRule": "Democratic Party wins 9 or more out of 17 targeted seats",
    "resolutionSource": "National Election Commission official results",
    "resolveAt": "2026-06-03T23:59:59",
    "category": "POLITICS",
    "votingDuration": 300,
    "bettingDuration": 300,
    "executionModel": "AMM_FPMM",
    "seedUsdc": 1000,
    "feeRate": 0.01
  }'
echo -e "\n${GREEN}✓ 완료${NC}\n"

echo ""
echo -e "${GREEN}=== 모든 질문 등록 완료! ===${NC}"
echo "- 총 18개 질문 등록됨"
echo "- 각 질문: 5분 투표 → 5분 브레이크 → 5분 베팅"
echo ""
