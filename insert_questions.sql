-- 18개 질문 직접 삽입 스크립트
-- 5분 투표(300초) + 5분 브레이크 + 5분 베팅(300초)

-- 1. Will the home team beat the away team?
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will the home team beat the away team?',
    'SPORTS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Home team wins within regular time (90 minutes)',
    'Official match results',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 2. Will both teams score at least one goal? (BTTS)
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will both teams score at least one goal? (BTTS)',
    'SPORTS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Both home and away teams score at least one goal',
    'Official match results',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 3. Will this match have 3 or more total goals?
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will this match have 3 or more total goals?',
    'SPORTS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Combined goals are 3 or more (e.g., 3-0, 2-1, 2-2)',
    'Official match results',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 4. Will the home team keep a clean sheet?
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will the home team keep a clean sheet?',
    'SPORTS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Home team finishes with 0 goals conceded',
    'Official match results',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 5. Will the home team score first in the first half?
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will the home team score first in the first half?',
    'SPORTS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Home team scores the first goal before halftime',
    'Official match results',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 6. Nike vs Adidas
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'In the 2026 S/S season, will Nike win more ''Sneaker of the Year'' awards than Adidas?',
    'CULTURE', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Nike wins more major sneaker awards than Adidas in defined outlets',
    'Major sneaker media awards',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-06-30 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 7. BTS Billboard
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will any BTS member''s solo release enter Billboard Hot 100 Top 10 in 2026?',
    'CULTURE', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'At least one BTS member reaches Hot 100 Top 10 in 2026 with solo music',
    'Billboard Hot 100 official chart',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-12-31 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 8. ChatGPT vs Claude
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'By the end of 2026, will ChatGPT have more monthly active users than Claude?',
    'TECH', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'ChatGPT MAU is greater than Claude MAU based on credible public data',
    'Official announcements or credible third-party analysis',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-12-31 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 9. Marvel box office
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will the next Marvel theatrical release rank #1 at the Korean box office on opening weekend?',
    'CULTURE', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Marvel release ranks #1 in Korea for opening weekend',
    'Korean Film Council official box office data',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 10. Bubble tea vs tanghulu
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'In summer 2026, will bubble tea have more SNS mentions than tanghulu?',
    'CULTURE', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Bubble tea mentions exceed tanghulu mentions from Jun-Aug 2026 on Korean SNS (Instagram/X/TikTok)',
    'SNS analytics tools',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-08-31 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 11. Russia-Ukraine ceasefire
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will Russia and Ukraine sign an official ceasefire agreement by March 31, 2026?',
    'POLITICS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Both governments formally sign a ceasefire agreement by deadline',
    'Official government announcements and major media',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-03-31 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 12. Trump EU tariffs
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will Trump officially impose additional tariffs of 20% or more on the EU by March 31, 2026?',
    'POLITICS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'A formal order/law enacting tariffs of at least 20% takes effect',
    'Official US government announcements',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-03-31 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 13. Bitcoin $120,000
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will Bitcoin break $120,000 by March 31, 2026?',
    'ECONOMY', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'BTC reaches at least $120,000 on a designated reference exchange/data source',
    'Major cryptocurrency exchange data',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-03-31 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 14. Federal Reserve rate cut
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will the U.S. Federal Reserve cut rates again by June 30, 2026?',
    'ECONOMY', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'FOMC announces at least one 25bp or larger cut by deadline',
    'Federal Reserve official announcements',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-06-30 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 15. Elon Musk DOGE
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will Elon Musk officially step down as DOGE (Department of Government Efficiency) head by March 31, 2026?',
    'POLITICS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Official resignation/removal announcement by deadline',
    'Official US government announcements',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-03-31 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 16. TikTok restoration
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will TikTok be fully restored in U.S. app stores by March 31, 2026?',
    'POLITICS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'TikTok is re-listed and functioning normally in U.S. app stores by deadline',
    'App store verification',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-03-31 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 17. Korea Financial Investment Income Tax
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'Will South Korea fully repeal the Financial Investment Income Tax by March 31, 2026?',
    'POLITICS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Full repeal bill passes and is promulgated by deadline',
    'National Assembly and government official announcements',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-03-31 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- 18. Korea local elections
INSERT INTO questions (
    title, category, status, type, market_type, resolution_rule, resolution_source,
    voting_end_at, betting_start_at, betting_end_at, expired_at, resolve_at,
    total_bet_pool, yes_bet_pool, no_bet_pool, initial_yes_pool, initial_no_pool,
    execution_model, created_at, vote_result_settlement
) VALUES (
    'In the June 2026 local elections, will the Democratic Party win at least 9 of 17 metropolitan mayor/governor seats?',
    'POLITICS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE',
    'Democratic Party wins 9 or more out of 17 targeted seats',
    'National Election Commission official results',
    NOW() + INTERVAL 5 MINUTE,
    NOW() + INTERVAL 10 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    NOW() + INTERVAL 15 MINUTE,
    '2026-06-03 23:59:59',
    0, 0, 0, 0, 0,
    'AMM_FPMM', NOW(), FALSE
);

-- AMM 풀 시딩은 별도로 수행 필요
SELECT '질문 등록 완료! AMM 풀 시딩을 위해 각 질문에 대해 /api/pool/seed API를 호출하세요.' AS message;
