#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="$1"
QUESTION_ID="$2"
USDC_IN="${3:-10}"
PASSWORD="${PASSWORD:-123400}"

TOKEN="$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | jq -r '.token // empty')"

if [[ -z "$TOKEN" ]]; then
  echo "LOGIN_FAIL $EMAIL"
  exit 0
fi

if (( RANDOM % 2 == 0 )); then
  SIDE="YES"
else
  SIDE="NO"
fi

RESP="$(curl -s -X POST "$BASE_URL/api/swap" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"questionId\":${QUESTION_ID},\"action\":\"BUY\",\"outcome\":\"${SIDE}\",\"usdcIn\":${USDC_IN}}")"

OK="$(echo "$RESP" | jq -r '.success // false')"
if [[ "$OK" == "true" ]]; then
  echo "OK $EMAIL $SIDE"
else
  MSG="$(echo "$RESP" | jq -r '.message // .error // "unknown"')"
  echo "FAIL $EMAIL $SIDE $MSG"
fi
