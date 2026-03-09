-- Wallet/Treasury integrity checks
-- Run manually in production for reconciliation.

-- 1) members.usdc_balance 와 member_wallets.available_balance 불일치
SELECT
    m.member_id,
    m.usdc_balance AS member_balance,
    w.available_balance AS wallet_balance
FROM members m
JOIN member_wallets w ON w.member_id = m.member_id
WHERE m.usdc_balance <> w.available_balance;

-- 2) wallet_ledger 최신 balance_after 와 wallet 현재 잔액 불일치
WITH latest_wallet_ledger AS (
    SELECT wl.wallet_id, wl.balance_after, wl.created_at
    FROM wallet_ledgers wl
    JOIN (
        SELECT wallet_id, MAX(created_at) AS max_created_at
        FROM wallet_ledgers
        GROUP BY wallet_id
    ) x ON x.wallet_id = wl.wallet_id AND x.max_created_at = wl.created_at
)
SELECT
    w.wallet_id,
    w.member_id,
    w.available_balance,
    l.balance_after AS latest_ledger_balance
FROM member_wallets w
LEFT JOIN latest_wallet_ledger l ON l.wallet_id = w.wallet_id
WHERE l.wallet_id IS NOT NULL
  AND w.available_balance <> l.balance_after;

-- 3) 음수 wallet 잔액 검사
SELECT wallet_id, member_id, available_balance, locked_balance
FROM member_wallets
WHERE available_balance < 0 OR locked_balance < 0;

-- 4) treasury 순유입/순유출 요약
SELECT
    SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END) AS total_inflow,
    SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END) AS total_outflow,
    SUM(amount) AS net_flow
FROM treasury_ledgers;

-- 5) 참조 없는 wallet ledger 검사 (reference_type 있는데 reference_id null)
SELECT ledger_id, member_id, tx_type, reference_type, reference_id, created_at
FROM wallet_ledgers
WHERE reference_type IS NOT NULL
  AND reference_id IS NULL;
