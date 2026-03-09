-- ============================================================
-- 투표 정합성 점검 SQL (VOTE-025)
-- 목적: daily_usage 불일치, summary 불일치, stuck relay 수동 점검
-- 실행: MariaDB 클라이언트에서 수동으로 실행
-- ============================================================

-- ----------------------------------------------------------------
-- 1. daily_vote_usage vs 실제 VOTE activity 건수 불일치 검사
--    daily_vote_usage.used_count 가 실제 VOTE activity 건수와 다른 회원/날짜 조회
-- ----------------------------------------------------------------
SELECT
    d.member_id,
    d.usage_date,
    d.used_count                                          AS recorded,
    COUNT(a.id)                                           AS actual,
    d.used_count - COUNT(a.id)                            AS diff
FROM daily_vote_usage d
LEFT JOIN activities a
       ON a.member_id   = d.member_id
      AND a.activity_type = 'VOTE'
      AND DATE(a.created_at) = d.usage_date
GROUP BY d.member_id, d.usage_date, d.used_count
HAVING diff <> 0
ORDER BY ABS(diff) DESC, d.usage_date DESC
LIMIT 100;


-- ----------------------------------------------------------------
-- 2. vote_summary vs 실제 VOTE activity 건수 불일치 검사
--    yes_count / no_count 가 실제 activity 기준과 다른 질문 조회
-- ----------------------------------------------------------------
SELECT
    vs.question_id,
    vs.yes_count                                             AS s_yes,
    vs.no_count                                              AS s_no,
    vs.total_count                                           AS s_total,
    COALESCE(a_yes.cnt, 0)                                   AS a_yes,
    COALESCE(a_no.cnt,  0)                                   AS a_no,
    COALESCE(a_yes.cnt, 0) + COALESCE(a_no.cnt, 0)          AS a_total,
    vs.yes_count   - COALESCE(a_yes.cnt, 0)                 AS diff_yes,
    vs.no_count    - COALESCE(a_no.cnt,  0)                 AS diff_no,
    vs.total_count - (COALESCE(a_yes.cnt, 0) + COALESCE(a_no.cnt, 0)) AS diff_total
FROM vote_summary vs
LEFT JOIN (
    SELECT question_id, COUNT(*) AS cnt
    FROM activities
    WHERE activity_type = 'VOTE' AND choice = 'YES'
    GROUP BY question_id
) a_yes ON a_yes.question_id = vs.question_id
LEFT JOIN (
    SELECT question_id, COUNT(*) AS cnt
    FROM activities
    WHERE activity_type = 'VOTE' AND choice = 'NO'
    GROUP BY question_id
) a_no  ON a_no.question_id  = vs.question_id
WHERE vs.yes_count   <> COALESCE(a_yes.cnt, 0)
   OR vs.no_count    <> COALESCE(a_no.cnt,  0)
   OR vs.total_count <> (COALESCE(a_yes.cnt, 0) + COALESCE(a_no.cnt, 0))
ORDER BY ABS(diff_total) DESC
LIMIT 100;


-- ----------------------------------------------------------------
-- 3. Stuck relay 점검
--    PENDING 상태가 10분 이상 처리되지 않은 릴레이
--    (스케줄러 장애 또는 예외적 DB 락 상황 점검)
-- ----------------------------------------------------------------
SELECT
    id,
    vote_id,
    member_id,
    question_id,
    choice,
    status,
    retry_count,
    created_at,
    updated_at,
    TIMESTAMPDIFF(MINUTE, created_at, NOW()) AS stuck_minutes
FROM onchain_vote_relays
WHERE status = 'PENDING'
  AND created_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE)
ORDER BY created_at ASC
LIMIT 50;


-- ----------------------------------------------------------------
-- 4. SUBMITTED 상태가 5분 이상 머무는 릴레이 (스케줄러 비정상 종료 의심)
-- ----------------------------------------------------------------
SELECT
    id,
    vote_id,
    status,
    retry_count,
    updated_at,
    TIMESTAMPDIFF(MINUTE, updated_at, NOW()) AS stuck_minutes
FROM onchain_vote_relays
WHERE status = 'SUBMITTED'
  AND updated_at < DATE_SUB(NOW(), INTERVAL 5 MINUTE)
ORDER BY updated_at ASC
LIMIT 50;


-- ----------------------------------------------------------------
-- 5. FAILED_FINAL 릴레이 현황 (운영자 수동 재처리 검토 대상)
-- ----------------------------------------------------------------
SELECT
    id,
    vote_id,
    member_id,
    question_id,
    choice,
    retry_count,
    error_message,
    created_at,
    updated_at
FROM onchain_vote_relays
WHERE status = 'FAILED_FINAL'
ORDER BY updated_at DESC
LIMIT 100;


-- ----------------------------------------------------------------
-- 6. 상태별 릴레이 건수 요약
-- ----------------------------------------------------------------
SELECT
    status,
    COUNT(*)                       AS cnt,
    MIN(created_at)                AS oldest,
    MAX(updated_at)                AS latest
FROM onchain_vote_relays
GROUP BY status
ORDER BY cnt DESC;
