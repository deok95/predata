#!/bin/zsh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${QA_ADMIN_EMAIL:-admin@predata.io}"
PASSWORD="${QA_ADMIN_PASSWORD:-123400}"

LOGIN=$(curl -sS -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
TOKEN=$(echo "$LOGIN" | jq -r '.data.token // empty')
if [ -z "$TOKEN" ]; then
  echo "[qa] login failed"
  echo "$LOGIN"
  exit 1
fi

OPEN_KEY="qa-open-$(date +%s)"
OPEN_RESP=$(curl -sS -X POST "$BASE_URL/api/questions/drafts/open" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Idempotency-Key: $OPEN_KEY" \
  -H "Content-Type: application/json")
DRAFT_ID=$(echo "$OPEN_RESP" | jq -r '.data.draftId // empty')
SUBMIT_KEY=$(echo "$OPEN_RESP" | jq -r '.data.submitIdempotencyKey // .data.idempotencyKey // empty')
if [ -z "$DRAFT_ID" ] || [ -z "$SUBMIT_KEY" ]; then
  echo "[qa] draft open failed"
  echo "$OPEN_RESP"
  exit 1
fi

NOW=$(date +%s)
read -r -d '' PAYLOAD <<EOF || true
{
  "title": "[QA] Payload coverage ${NOW}",
  "category": "TECH",
  "description": "QA submit with all supported optional fields",
  "voteWindowType": "D1",
  "settlementMode": "OBJECTIVE_RULE",
  "resolutionRule": "Resolve YES if official source publishes update by deadline",
  "resolutionSource": "https://openai.com/news",
  "thumbnailUrl": "https://images.unsplash.com/photo-1518770660439-4636190af475",
  "tags": ["qa", "payload", "submit"],
  "sourceLinks": ["https://openai.com/news", "https://status.openai.com"],
  "boostEnabled": true,
  "creatorSplitInPool": 70
}
EOF

SUBMIT_RESP=$(curl -sS -X POST "$BASE_URL/api/questions/drafts/$DRAFT_ID/submit" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: $SUBMIT_KEY" \
  -d "$PAYLOAD")
QUESTION_ID=$(echo "$SUBMIT_RESP" | jq -r '.data.questionId // empty')
if [ -z "$QUESTION_ID" ]; then
  echo "[qa] submit failed"
  echo "$SUBMIT_RESP"
  exit 1
fi

DETAIL=$(curl -sS -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/questions/$QUESTION_ID")
echo "$DETAIL" | jq '{
  questionId: .data.id,
  title: .data.title,
  category: .data.category,
  description: .data.description,
  thumbnailUrl: .data.thumbnailUrl,
  tagsJson: .data.tagsJson,
  sourceLinksJson: .data.sourceLinksJson,
  boostEnabled: .data.boostEnabled,
  creatorSplitInPool: .data.creatorSplitInPool,
  platformFeeShare: .data.platformFeeShare,
  creatorFeeShare: .data.creatorFeeShare,
  voterFeeShare: .data.voterFeeShare
}'
