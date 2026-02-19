#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-mac}"
DB_PASS="${DB_PASS:-predata}"
DB_NAME="${DB_NAME:-predata}"

ACCOUNT_COUNT="${ACCOUNT_COUNT:-100}"
BET_AMOUNT="${BET_AMOUNT:-10}"
TEST_PASSWORD="${TEST_PASSWORD:-123400}"

QUESTION_ID="${QUESTION_ID:-}"

workdir="$(cd "$(dirname "$0")" && pwd)"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "missing command: $1"
    exit 1
  }
}

need_cmd curl
need_cmd jq
need_cmd mariadb

status_of_question() {
  local qid="$1"
  curl -s "$BASE_URL/api/questions/$qid" | jq -r '.data.status // .status // empty'
}

login_token() {
  local email="$1"
  local password="$2"
  curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$password\"}" | jq -r '.token // empty'
}

wait_for_status() {
  local qid="$1"
  local expect="$2"
  local timeout="${3:-180}"
  local start_ts now status
  start_ts="$(date +%s)"
  while true; do
    status="$(status_of_question "$qid")"
    if [[ "$status" == "$expect" ]]; then
      echo "question=$qid reached status=$expect"
      return 0
    fi
    now="$(date +%s)"
    if (( now - start_ts > timeout )); then
      echo "timeout waiting status=$expect current=$status"
      return 1
    fi
    sleep 2
  done
}

echo "[1/9] admin login"
ADMIN_TOKEN="$(login_token "admin@predata.io" "123400")"
if [[ -z "$ADMIN_TOKEN" ]]; then
  echo "admin login failed"
  exit 1
fi

if [[ -z "$QUESTION_ID" ]]; then
  echo "[2/9] create 1m voting + 1m betting question"
  create_resp="$(curl -s -X POST "$BASE_URL/api/admin/questions" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "title":"시장은 테스트 100계정 부하에서 YES가 우세할 것이라고 생각할까요?",
      "type":"OPINION",
      "marketType":"OPINION",
      "resolutionRule":"Resolve YES if vote majority is YES at voting end; otherwise NO.",
      "voteResultSettlement":true,
      "category":"TECH",
      "votingDuration":60,
      "bettingDuration":60,
      "executionModel":"AMM_FPMM",
      "seedUsdc":1000,
      "feeRate":0.01
    }')"
  ok="$(echo "$create_resp" | jq -r '.success // false')"
  if [[ "$ok" != "true" ]]; then
    echo "question create failed: $create_resp"
    exit 1
  fi
  QUESTION_ID="$(echo "$create_resp" | jq -r '.questionId')"

  echo "[3/9] force break 1 minute (DB time patch)"
  mariadb -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -D "$DB_NAME" -e "
    UPDATE questions
    SET betting_start_at = DATE_ADD(voting_end_at, INTERVAL 1 MINUTE),
        betting_end_at = DATE_ADD(DATE_ADD(voting_end_at, INTERVAL 1 MINUTE), INTERVAL 1 MINUTE),
        expired_at = DATE_ADD(DATE_ADD(voting_end_at, INTERVAL 1 MINUTE), INTERVAL 1 MINUTE)
    WHERE question_id = ${QUESTION_ID};"
fi

echo "question_id=$QUESTION_ID"

echo "[4/9] login ${ACCOUNT_COUNT} users (test001..)"
tokens_file="$tmp_dir/tokens.tsv"
> "$tokens_file"
for n in $(seq 1 "$ACCOUNT_COUNT"); do
  email="$(printf "test%03d@predata.io" "$n")"
  token="$(login_token "$email" "$TEST_PASSWORD")"
  if [[ -n "$token" ]]; then
    echo -e "${email}\t${token}" >> "$tokens_file"
  fi
done
token_count="$(wc -l < "$tokens_file" | tr -d ' ')"
if [[ "$token_count" -lt "$ACCOUNT_COUNT" ]]; then
  echo "warning: logged in $token_count/$ACCOUNT_COUNT"
fi

echo "[5/9] vote phase (random YES/NO)"
vote_ok=0
vote_fail=0
while IFS=$'\t' read -r email token; do
  choice=$((RANDOM % 2))
  if [[ "$choice" -eq 0 ]]; then side="YES"; else side="NO"; fi
  resp="$(curl -s -X POST "$BASE_URL/api/vote" \
    -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" \
    -d "{\"questionId\":${QUESTION_ID},\"choice\":\"${side}\"}")"
  if [[ "$(echo "$resp" | jq -r '.success // false')" == "true" ]]; then
    vote_ok=$((vote_ok + 1))
  else
    vote_fail=$((vote_fail + 1))
  fi
done < "$tokens_file"
echo "vote_ok=$vote_ok vote_fail=$vote_fail"

echo "[6/9] wait BREAK then BETTING"
wait_for_status "$QUESTION_ID" "BREAK" 180
wait_for_status "$QUESTION_ID" "BETTING" 180

echo "[7/9] AMM buy phase (${BET_AMOUNT} USDC each, random YES/NO)"
bet_ok=0
bet_fail=0
while IFS=$'\t' read -r email token; do
  choice=$((RANDOM % 2))
  if [[ "$choice" -eq 0 ]]; then side="YES"; else side="NO"; fi
  resp="$(curl -s -X POST "$BASE_URL/api/swap" \
    -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" \
    -d "{\"questionId\":${QUESTION_ID},\"action\":\"BUY\",\"outcome\":\"${side}\",\"usdcIn\":${BET_AMOUNT}}")"
  if [[ "$(echo "$resp" | jq -r '.success // false')" == "true" ]]; then
    bet_ok=$((bet_ok + 1))
  else
    bet_fail=$((bet_fail + 1))
  fi
done < "$tokens_file"
echo "bet_ok=$bet_ok bet_fail=$bet_fail"

echo "[8/9] wait settlement"
wait_for_status "$QUESTION_ID" "SETTLED" 240

echo "[9/9] summary (swap + fee + reward)"
mariadb -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -D "$DB_NAME" -e "
SELECT question_id,title,status,final_result,total_bet_pool,yes_bet_pool,no_bet_pool,voting_end_at,betting_start_at,betting_end_at
FROM questions WHERE question_id=${QUESTION_ID};

SELECT COUNT(*) AS swap_count,SUM(usdc_in) AS total_usdc_in,SUM(fee_usdc) AS total_fee_usdc
FROM swap_history
WHERE question_id=${QUESTION_ID};

SELECT action,COUNT(*) AS cnt,SUM(usdc_in) AS usdc_in_sum,SUM(fee_usdc) AS fee_sum
FROM swap_history
WHERE question_id=${QUESTION_ID}
GROUP BY action
ORDER BY action;

SELECT question_id,total_fees,platform_share,reward_pool_share,reserve_share
FROM fee_pools
WHERE question_id=${QUESTION_ID};

SELECT question_id,COUNT(*) AS reward_rows,SUM(amount) AS reward_sum,
       SUM(CASE WHEN status='COMPLETED' THEN amount ELSE 0 END) AS reward_completed
FROM reward_distributions
WHERE question_id=${QUESTION_ID}
GROUP BY question_id;
"

echo "done question_id=${QUESTION_ID} vote_ok=${vote_ok} bet_ok=${bet_ok}"
