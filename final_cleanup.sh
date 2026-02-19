#!/bin/bash
cd /Users/harrykim/Desktop/predata/backend/src/main/kotlin

# Common Korean words cleanup
find . -name "*.kt" -type f -exec sed -i '' \
  -e 's/트랜잭션/transaction/g' \
  -e 's/게시/publish/g' \
  -e 's/특정/specific/g' \
  -e 's/괴리율/divergence rate/g' \
  -e 's/동일/same/g' \
  -e 's/복구/recovery/g' \
  -e 's/가중치/weight/g' \
  -e 's/체크/check/g' \
  -e 's/탐지/detection/g' \
  -e 's/레퍼럴/referral/g' \
  -e 's/배당금/dividend/g' \
  -e 's/보유/held/g' \
  -e 's/분포/distribution/g' \
  -e 's/폴링/polling/g' \
  -e 's/보상/reward/g' \
  -e 's/서버 오류가 발생했습니다/server error occurred/g' \
  -e 's/발생했습니다/occurred/g' \
  -e 's/구간/interval/g' \
  -e 's/누적/cumulative/g' \
  -e 's/초과/exceeded/g' \
  -e 's/미만/less than/g' \
  -e 's/예정/scheduled/g' \
  -e 's/대기/pending/g' \
  -e 's/진행/progress/g' \
  -e 's/종료/ended/g' \
  -e 's/개선/improvement/g' \
  -e 's/최적화/optimization/g' \
  -e 's/성능/performance/g' \
  -e 's/효율/efficiency/g' \
  -e 's/안정/stable/g' \
  -e 's/장애/failure/g' \
  -e 's/재시도/retry/g' \
  -e 's/재전송/resend/g' \
  -e 's/대상/target/g' \
  -e 's/목표/goal/g' \
  -e 's/기간/period/g' \
  -e 's/시작/start/g' \
  -e 's/추가/add/g' \
  -e 's/삭제/delete/g' \
  -e 's/변경/change/g' \
  -e 's/수정/modify/g' \
  -e 's/갱신/update/g' \
  -e 's/조회/retrieve/g' \
  -e 's/통계/statistics/g' \
  -e 's/분석/analysis/g' \
  -e 's/계산/calculation/g' \
  {} \;

# Clean up particles and common endings
find . -name "*.kt" -type f -exec sed -i '' \
  -e 's/는 /  /g' \
  -e 's/은 /  /g' \
  -e 's/가 /  /g' \
  -e 's/이 /  /g' \
  -e 's/을 /  /g' \
  -e 's/를 /  /g' \
  -e 's/에 /  /g' \
  -e 's/에서 /  from /g' \
  -e 's/로 /  to /g' \
  -e 's/으로 /  to /g' \
  -e 's/와 / and /g' \
  -e 's/과 / and /g' \
  -e 's/의 / of /g' \
  -e 's/도 / also /g' \
  -e 's/만 / only /g' \
  -e 's/부터 / from /g' \
  -e 's/까지 / until /g' \
  -e 's/에게 / to /g' \
  -e 's/한 / a /g' \
  -e 's/개 /  items /g' \
  -e 's/회 / times /g' \
  -e 's/번 / times /g' \
  -e 's/시 / when /g' \
  -e 's/시간 / hours /g' \
  -e 's/분 / minutes /g' \
  -e 's/초 / seconds /g' \
  -e 's/일 / days /g' \
  -e 's/주 / weeks /g' \
  -e 's/건 / cases /g' \
  {} \;

count=$(find . -name "*.kt" -type f | xargs grep -l '[가-힣]' 2>/dev/null | wc -l | tr -d ' ')
echo "Remaining files with Korean: $count"
