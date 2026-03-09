-- ============================================================
-- 마켓 배치 정합성 검증 SQL
-- 목적: 배치 집계 수치 vs 실제 후보 수 불일치 탐지
-- 실행: DBA 수동 또는 운영 모니터링 주기 실행
-- ============================================================

-- 1. 배치 상태 개요
SELECT
    status,
    COUNT(*)               AS batch_count,
    SUM(total_candidates)  AS total_candidates,
    SUM(selected_count)    AS total_selected,
    SUM(opened_count)      AS total_opened,
    SUM(failed_count)      AS total_failed
FROM market_open_batches
GROUP BY status
ORDER BY FIELD(status, 'PENDING', 'SELECTED', 'OPENING', 'COMPLETED', 'PARTIAL_FAILED', 'FAILED');


-- 2. 배치 집계 vs 실제 후보 수 불일치 탐지
--    batch.selected_count ≠ actual SELECTED_TOP3 count → 집계 오류
SELECT
    b.id                                                   AS batch_id,
    b.cutoff_slot_utc,
    b.status,
    b.selected_count                                       AS batch_selected_count,
    COUNT(CASE WHEN c.selection_status = 'SELECTED_TOP3' THEN 1 END) AS actual_selected_count,
    b.opened_count                                         AS batch_opened_count,
    COUNT(CASE WHEN c.open_status = 'OPENED'       THEN 1 END) AS actual_opened_count,
    b.failed_count                                         AS batch_failed_count,
    COUNT(CASE WHEN c.open_status = 'OPEN_FAILED'  THEN 1 END) AS actual_failed_count
FROM market_open_batches b
LEFT JOIN question_market_candidates c ON c.batch_id = b.id
GROUP BY b.id, b.cutoff_slot_utc, b.status, b.selected_count, b.opened_count, b.failed_count
HAVING
    b.selected_count <> COUNT(CASE WHEN c.selection_status = 'SELECTED_TOP3' THEN 1 END)
    OR b.opened_count  <> COUNT(CASE WHEN c.open_status = 'OPENED'      THEN 1 END)
    OR b.failed_count  <> COUNT(CASE WHEN c.open_status = 'OPEN_FAILED' THEN 1 END)
ORDER BY b.id DESC;


-- 3. 오픈됐으나 질문이 아직 VOTING 상태인 건 (orphan 탐지)
SELECT
    c.batch_id,
    c.question_id,
    c.open_status,
    q.status AS question_status
FROM question_market_candidates c
JOIN questions q ON q.question_id = c.question_id
WHERE c.open_status = 'OPENED'
  AND q.status = 'VOTING';


-- 4. SELECTED_TOP3 이지만 open_status가 null인 건 (미처리 탐지)
SELECT
    c.batch_id,
    c.question_id,
    c.category,
    c.rank_in_category,
    b.status AS batch_status,
    b.finished_at
FROM question_market_candidates c
JOIN market_open_batches b ON b.id = c.batch_id
WHERE c.selection_status = 'SELECTED_TOP3'
  AND c.open_status IS NULL
  AND b.status NOT IN ('PENDING', 'SELECTED', 'OPENING')
ORDER BY c.batch_id DESC, c.category, c.rank_in_category;


-- 5. 최근 7일 배치 요약
SELECT
    DATE(b.started_at)  AS batch_date,
    COUNT(*)            AS batch_count,
    SUM(b.opened_count) AS total_opened,
    SUM(b.failed_count) AS total_failed
FROM market_open_batches b
WHERE b.started_at >= UTC_TIMESTAMP() - INTERVAL 7 DAY
GROUP BY DATE(b.started_at)
ORDER BY batch_date DESC;
