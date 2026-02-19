-- AMM 풀 시딩 (각 질문에 대해)
-- seedUsdc: 1000, feeRate: 0.01

-- SwapService.seedPool을 직접 호출할 수 없으므로
-- 풀 데이터를 직접 삽입합니다

-- 질문 277-294에 대한 AMM 풀 데이터 삽입
INSERT INTO market_pools (question_id, yes_shares, no_shares, fee_rate, collateral_locked, total_volume_usdc, total_fees_usdc, status, version, created_at, updated_at)
SELECT
    question_id,
    1000.000000000000000000 as yes_shares,
    1000.000000000000000000 as no_shares,
    0.01000 as fee_rate,
    2000.000000000000000000 as collateral_locked,
    0.000000000000000000 as total_volume_usdc,
    0.000000000000000000 as total_fees_usdc,
    'ACTIVE' as status,
    0 as version,
    NOW() as created_at,
    NOW() as updated_at
FROM questions
WHERE question_id BETWEEN 277 AND 294
  AND execution_model = 'AMM_FPMM'
ON DUPLICATE KEY UPDATE
    yes_shares = 1000.000000000000000000,
    no_shares = 1000.000000000000000000,
    fee_rate = 0.01000,
    collateral_locked = 2000.000000000000000000,
    status = 'ACTIVE',
    updated_at = NOW();

SELECT CONCAT('AMM 풀 시딩 완료: ', COUNT(*), '개 질문') AS message
FROM market_pools
WHERE question_id BETWEEN 277 AND 294;
