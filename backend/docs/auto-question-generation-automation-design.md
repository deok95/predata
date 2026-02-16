# Auto Question Generation Automation Design

## 1. Goal
- Subcategory마다 하루 3문제 자동 생성
- 구성: VERIFIABLE 2 + OPINION 1
- OPINION은 "시장은 ...라고 생각할까요?" 템플릿 강제
- OPINION은 투표 결과로 베팅 승패를 판정(vote_result_settlement=true)

## 2. End-to-end flow
1. TrendCollectorJob
- Google Trends 수집
- subcategory별 상위 신호 저장

2. DraftGenerationJob
- OpenAI structured output 호출
- 배치별 3개 초안 생성

3. ValidationJob
- 템플릿/개수/타입비율/중복/정산가능성 검증
- 실패 항목은 REJECTED + reason 기록

4. PublishJob
- VALIDATED 초안만 Question으로 게시
- lifecycle 시간 필드 자동 계산

5. LifecycleJob
- 생성 후 24h 투표
- break 후 betting 진입
- betting 종료 후 reveal
- reveal 종료 후 정산

6. SettlementJob
- VERIFIABLE: resolutionSource 기반 자동 판정
- OPINION: reveal 결과 기반 판정

## 3. Timing policy
- votingDurationHours: 24
- breakMinutes: 30 (운영에서 설정 가능)
- bettingDurationHours: 24 (시장 유형별 override 가능)
- revealDurationMinutes: 30

## 4. Deterministic scheduling
- timezone: UTC 고정(내부 저장)
- daily batch trigger: 00:05 UTC
- subcategory 단위 idempotency key:
  - question_gen:{date}:{subcategory}

## 5. Validation checklist
- 총 3개인지
- OPINION 정확히 1개인지
- VERIFIABLE 정확히 2개인지
- OPINION 템플릿 준수 여부
- OPINION voteResultSettlement=true 여부
- VERIFIABLE resolutionSource 존재 여부
- resolveAt > now + minimum lead time 여부
- 최근 30일 제목 유사도 임계치 초과 여부

## 6. Ops safety
- dryRun 모드 지원
- 재시도 최대 3회 + 지수 백오프
- 실패 배치 알림 조건:
  - 실패율 >= 30%
  - 연속 실패 3회
- 관리자 수동 게시 endpoint 제공

## 7. Data model (proposed)
- trend_signal
  - id, date, subcategory, keyword, trend_score, region, source, created_at
- generation_batch
  - batch_id, subcategory, target_date, status, requested_count, accepted_count, rejected_count, created_at
- generation_item
  - id, batch_id, draft_id, payload_json, status, reject_reason, duplicate_score, published_question_id, created_at
- job_execution_log
  - id, job_name, run_at, status, duration_ms, error_message

## 8. Alert routing
- WARN: validation warning only (ops dashboard)
- ERROR: batch hard fail (Slack/Email)
- CRITICAL: 24h 이상 생성 없음 (Pager)

## 9. Rollout plan
1. Phase 1: dryRun only (7 days)
2. Phase 2: subcategory 1~2개만 auto publish
3. Phase 3: 전체 subcategory rollout

## 10. Definition of done
- 7일 연속 배치 성공률 >= 99%
- 중복 질문률 < 1%
- OPINION 템플릿 위반 0건
- 정산 불가 질문 0건
