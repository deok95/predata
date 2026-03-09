-- ============================================================
-- PRE(D)ATA Demo Seed Data — Pitch Deck Screenshots (Global / English)
-- Run: mysql -u root -p1234 -h 127.0.0.1 -P 3306 predata < scripts/seed_demo_data.sql
-- Members: 10130-10144 | Questions: 5001-5009
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. MEMBERS — global community feel
-- ============================================================
INSERT INTO members (member_id, email, username, display_name, country_code, tier, tier_weight,
                     accuracy_score, total_predictions, correct_predictions,
                     level, usdc_balance, point_balance, has_voting_pass, role,
                     referral_code, created_at)
VALUES
  (10130, 'arsenalways@demo.io',   'arsenalways',   'Arsenal Till I Die', 'GB', 'DIAMOND',  3.00, 89, 142, 126, 5, 4820.50, 3200.00, 1, 'USER', 'DEMO001', NOW() - INTERVAL 180 DAY),
  (10131, 'btcmaxi99@demo.io',     'btcmaxi99',     'BTC Maximalist',     'US', 'PLATINUM', 2.50, 82, 98,  80,  4, 2350.00, 1800.00, 1, 'USER', 'DEMO002', NOW() - INTERVAL 150 DAY),
  (10132, 'predictiongod@demo.io', 'predictiongod', 'PredictionGod',      'SG', 'GOLD',     2.00, 76, 214, 163, 4, 1240.00, 980.00,  1, 'USER', 'DEMO003', NOW() - INTERVAL 120 DAY),
  (10133, 'crypto_whale@demo.io',  'crypto_whale',  'Crypto Whale',       'AE', 'GOLD',     2.00, 71, 87,  62,  3, 3100.00, 2100.00, 1, 'USER', 'DEMO004', NOW() - INTERVAL 90  DAY),
  (10134, 'soccerhead@demo.io',    'soccerhead',    'Soccer Head',        'ES', 'SILVER',   1.50, 68, 56,  38,  3, 580.00,  420.00,  0, 'USER', 'DEMO005', NOW() - INTERVAL 75  DAY),
  (10135, 'stocksniper@demo.io',   'stocksniper',   'Stock Sniper',       'JP', 'SILVER',   1.50, 65, 73,  47,  3, 890.00,  610.00,  1, 'USER', 'DEMO006', NOW() - INTERVAL 60  DAY),
  (10136, 'popculture42@demo.io',  'popculture42',  'Pop Culture 42',     'AU', 'SILVER',   1.50, 61, 45,  27,  2, 310.00,  240.00,  0, 'USER', 'DEMO007', NOW() - INTERVAL 55  DAY),
  (10137, 'marketwizard@demo.io',  'marketwizard',  'Market Wizard',      'DE', 'GOLD',     2.00, 78, 109, 85,  4, 1750.00, 1200.00, 1, 'USER', 'DEMO008', NOW() - INTERVAL 100 DAY),
  (10138, 'ethmaxi@demo.io',       'ethmaxi',       'ETH Maxi',           'US', 'PLATINUM', 2.50, 80, 63,  50,  4, 2100.00, 1500.00, 1, 'USER', 'DEMO009', NOW() - INTERVAL 130 DAY),
  (10139, 'sportsbrain@demo.io',   'sportsbrain',   'Sports Brain',       'BR', 'GOLD',     2.00, 74, 132, 97,  3, 960.00,  720.00,  1, 'USER', 'DEMO010', NOW() - INTERVAL 80  DAY),
  (10140, 'freshmint@demo.io',     'freshmint',     'Fresh Mint',         'IN', 'BRONZE',   1.00, 50, 12,  6,   1, 150.00,  80.00,   0, 'USER', 'DEMO011', NOW() - INTERVAL 10  DAY),
  (10141, 'politicaledge@demo.io', 'politicaledge', 'Political Edge',     'US', 'SILVER',   1.50, 66, 89,  59,  3, 430.00,  310.00,  0, 'USER', 'DEMO012', NOW() - INTERVAL 65  DAY),
  (10142, 'genz_trader@demo.io',   'genz_trader',   'GenZ Trader',        'KR', 'SILVER',   1.50, 63, 38,  24,  2, 270.00,  180.00,  0, 'USER', 'DEMO013', NOW() - INTERVAL 30  DAY),
  (10143, 'dataoracle@demo.io',    'dataoracle',    'Data Oracle',        'CA', 'GOLD',     2.00, 79, 167, 132, 4, 1480.00, 1050.00, 1, 'USER', 'DEMO014', NOW() - INTERVAL 110 DAY),
  (10144, 'moonshot2026@demo.io',  'moonshot2026',  'Moonshot 2026',      'MX', 'BRONZE',   1.00, 52, 21,  11,  1, 200.00,  120.00,  0, 'USER', 'DEMO015', NOW() - INTERVAL 20  DAY)
ON DUPLICATE KEY UPDATE
  username            = VALUES(username),
  display_name        = VALUES(display_name),
  tier                = VALUES(tier),
  tier_weight         = VALUES(tier_weight),
  accuracy_score      = VALUES(accuracy_score),
  total_predictions   = VALUES(total_predictions),
  correct_predictions = VALUES(correct_predictions),
  level               = VALUES(level),
  usdc_balance        = VALUES(usdc_balance),
  point_balance       = VALUES(point_balance),
  has_voting_pass     = VALUES(has_voting_pass);

-- ============================================================
-- 2. MEMBER WALLETS
-- ============================================================
INSERT INTO member_wallets (member_id, available_balance, locked_balance, version, created_at, updated_at)
VALUES
  (10130, 4820.50, 0.00, 0, NOW(), NOW()),
  (10131, 2350.00, 0.00, 0, NOW(), NOW()),
  (10132, 1240.00, 0.00, 0, NOW(), NOW()),
  (10133, 3100.00, 0.00, 0, NOW(), NOW()),
  (10134,  580.00, 0.00, 0, NOW(), NOW()),
  (10135,  890.00, 0.00, 0, NOW(), NOW()),
  (10136,  310.00, 0.00, 0, NOW(), NOW()),
  (10137, 1750.00, 0.00, 0, NOW(), NOW()),
  (10138, 2100.00, 0.00, 0, NOW(), NOW()),
  (10139,  960.00, 0.00, 0, NOW(), NOW()),
  (10140,  150.00, 0.00, 0, NOW(), NOW()),
  (10141,  430.00, 0.00, 0, NOW(), NOW()),
  (10142,  270.00, 0.00, 0, NOW(), NOW()),
  (10143, 1480.00, 0.00, 0, NOW(), NOW()),
  (10144,  200.00, 0.00, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  available_balance = VALUES(available_balance);

-- ============================================================
-- 3. QUESTIONS — English, global topics, IDs 5001-5009
--    voting_end_at 7+ days out so the lifecycle scheduler won't touch them
-- ============================================================
INSERT INTO questions (
  question_id, title, category, status, type, market_type,
  voting_phase, execution_model, vote_result_settlement,
  voting_end_at, betting_start_at, betting_end_at, expired_at,
  resolution_rule, resolution_source,
  initial_yes_pool, initial_no_pool,
  platform_fee_share, creator_fee_share, voter_fee_share,
  creator_split_in_pool, vote_window_type,
  creator_member_id, created_at
) VALUES

-- Sports
(5001,
 'Will Arsenal clinch the Premier League title before Matchday 36?',
 'SPORTS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE', 'VOTING_COMMIT_OPEN', 'AMM_FPMM', 0,
 DATE_ADD(NOW(), INTERVAL 7 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 7 DAY), INTERVAL 5 MINUTE),
 DATE_ADD(NOW(), INTERVAL 10 DAY),
 DATE_ADD(NOW(), INTERVAL 11 DAY),
 'YES if Arsenal are officially confirmed Premier League champions with 3+ games to spare (before MD36). Source: premierleague.com',
 'https://premierleague.com',
 500, 500, 0.20, 0.40, 0.40, 50, 'D3', 10130, NOW() - INTERVAL 6 HOUR),

(5002,
 'Will the NBA MVP award go to a player under 25 years old this season?',
 'SPORTS', 'VOTING', 'VERIFIABLE', 'VERIFIABLE', 'VOTING_COMMIT_OPEN', 'AMM_FPMM', 0,
 DATE_ADD(NOW(), INTERVAL 8 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 8 DAY), INTERVAL 5 MINUTE),
 DATE_ADD(NOW(), INTERVAL 11 DAY),
 DATE_ADD(NOW(), INTERVAL 12 DAY),
 'YES if the official 2025-26 NBA MVP award winner is 24 or younger on the award date. Source: nba.com',
 'https://nba.com',
 500, 500, 0.20, 0.40, 0.40, 50, 'D3', 10139, NOW() - INTERVAL 4 HOUR),

-- Culture / Fun
(5003,
 'Will Coca-Cola outscore Pepsi in global brand preference polls in 2026?',
 'CULTURE', 'VOTING', 'VERIFIABLE', 'VERIFIABLE', 'VOTING_COMMIT_OPEN', 'AMM_FPMM', 0,
 DATE_ADD(NOW(), INTERVAL 9 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 9 DAY), INTERVAL 5 MINUTE),
 DATE_ADD(NOW(), INTERVAL 12 DAY),
 DATE_ADD(NOW(), INTERVAL 13 DAY),
 'YES if Coca-Cola scores higher than Pepsi in the 2026 YouGov BrandIndex global ranking. Source: yougov.com/brandindex',
 'https://yougov.com',
 500, 500, 0.20, 0.40, 0.40, 50, 'D1', 10136, NOW() - INTERVAL 8 HOUR),

(5004,
 'Will GTA VI sell 10 million copies in its first 72 hours of release?',
 'CULTURE', 'VOTING', 'VERIFIABLE', 'VERIFIABLE', 'VOTING_COMMIT_OPEN', 'AMM_FPMM', 0,
 DATE_ADD(NOW(), INTERVAL 7 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 7 DAY), INTERVAL 5 MINUTE),
 DATE_ADD(NOW(), INTERVAL 10 DAY),
 DATE_ADD(NOW(), INTERVAL 11 DAY),
 'YES if Rockstar Games officially reports 10M+ copies sold within 72 hours of GTA VI launch.',
 'https://rockstargames.com',
 500, 500, 0.20, 0.40, 0.40, 50, 'D1', 10143, NOW() - INTERVAL 90 MINUTE),

-- Crypto / Tech
(5005,
 'Will Bitcoin break $120,000 before the end of June 2026?',
 'TECH', 'VOTING', 'VERIFIABLE', 'VERIFIABLE', 'VOTING_COMMIT_OPEN', 'AMM_FPMM', 0,
 DATE_ADD(NOW(), INTERVAL 7 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 7 DAY), INTERVAL 5 MINUTE),
 DATE_ADD(NOW(), INTERVAL 10 DAY),
 DATE_ADD(NOW(), INTERVAL 11 DAY),
 'YES if BTC/USD closing price on CoinGecko reaches $120,000 or above before July 1, 2026.',
 'https://coingecko.com/bitcoin',
 500, 500, 0.20, 0.40, 0.40, 50, 'D1', 10131, NOW() - INTERVAL 3 HOUR),

(5006,
 'Will Ethereum hit $5,000 at any point in 2026?',
 'TECH', 'VOTING', 'VERIFIABLE', 'VERIFIABLE', 'VOTING_COMMIT_OPEN', 'AMM_FPMM', 0,
 DATE_ADD(NOW(), INTERVAL 6 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 6 DAY), INTERVAL 5 MINUTE),
 DATE_ADD(NOW(), INTERVAL 9 DAY),
 DATE_ADD(NOW(), INTERVAL 10 DAY),
 'YES if ETH/USD closing price on CoinGecko reaches $5,000 or above at any point in calendar year 2026.',
 'https://coingecko.com/ethereum',
 500, 500, 0.20, 0.40, 0.40, 50, 'D1', 10138, NOW() - INTERVAL 5 HOUR),

-- Economy / Politics
(5007,
 'Will the Fed cut interest rates at least twice before the end of 2026?',
 'ECONOMY', 'VOTING', 'VERIFIABLE', 'VERIFIABLE', 'VOTING_COMMIT_OPEN', 'AMM_FPMM', 0,
 DATE_ADD(NOW(), INTERVAL 8 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 8 DAY), INTERVAL 5 MINUTE),
 DATE_ADD(NOW(), INTERVAL 11 DAY),
 DATE_ADD(NOW(), INTERVAL 12 DAY),
 'YES if the Federal Reserve announces 2 or more rate cuts (any size) in 2026. Source: federalreserve.gov',
 'https://federalreserve.gov',
 500, 500, 0.20, 0.40, 0.40, 50, 'D3', 10137, NOW() - INTERVAL 2 HOUR),

(5008,
 'Will OpenAI release GPT-5 as a publicly available model before July 2026?',
 'TECH', 'VOTING', 'VERIFIABLE', 'VERIFIABLE', 'VOTING_COMMIT_OPEN', 'AMM_FPMM', 0,
 DATE_ADD(NOW(), INTERVAL 6 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 6 DAY), INTERVAL 5 MINUTE),
 DATE_ADD(NOW(), INTERVAL 9 DAY),
 DATE_ADD(NOW(), INTERVAL 10 DAY),
 'YES if OpenAI officially releases a model branded "GPT-5" for public API or ChatGPT access before July 1, 2026.',
 'https://openai.com',
 500, 500, 0.20, 0.40, 0.40, 50, 'D1', 10141, NOW() - INTERVAL 1 HOUR),

(5009,
 'Will Apple announce a foldable iPhone at WWDC 2026?',
 'TECH', 'VOTING', 'VERIFIABLE', 'VERIFIABLE', 'VOTING_COMMIT_OPEN', 'AMM_FPMM', 0,
 DATE_ADD(NOW(), INTERVAL 7 DAY),
 DATE_ADD(DATE_ADD(NOW(), INTERVAL 7 DAY), INTERVAL 5 MINUTE),
 DATE_ADD(NOW(), INTERVAL 10 DAY),
 DATE_ADD(NOW(), INTERVAL 11 DAY),
 'YES if Apple officially reveals a foldable iPhone product at WWDC 2026 keynote. Source: apple.com',
 'https://apple.com',
 500, 500, 0.20, 0.40, 0.40, 50, 'D1', 10132, NOW() - INTERVAL 7 HOUR)

ON DUPLICATE KEY UPDATE
  title         = VALUES(title),
  category      = VALUES(category),
  status        = VALUES(status),
  voting_phase  = VALUES(voting_phase),
  voting_end_at = VALUES(voting_end_at),
  betting_start_at = VALUES(betting_start_at),
  betting_end_at   = VALUES(betting_end_at),
  expired_at       = VALUES(expired_at);

-- ============================================================
-- 4. VOTE SUMMARY — compelling distributions
-- ============================================================
INSERT INTO vote_summary (question_id, yes_count, no_count, total_count, updated_at)
VALUES
  (5001, 134, 47,  181, NOW()),  -- Arsenal: strong YES
  (5002,  61, 89,  150, NOW()),  -- NBA MVP u25: leaning NO
  (5003, 142, 38,  180, NOW()),  -- Coke vs Pepsi: Coke wins
  (5004, 198, 52,  250, NOW()),  -- GTA VI: huge hype
  (5005, 103, 58,  161, NOW()),  -- BTC 120k: moderate YES
  (5006,  79, 63,  142, NOW()),  -- ETH 5k: close call
  (5007,  88, 71,  159, NOW()),  -- Fed rate cut: uncertain
  (5008, 167, 34,  201, NOW()),  -- GPT-5: community believes YES
  (5009,  92, 104, 196, NOW())   -- Foldable iPhone: skeptical
ON DUPLICATE KEY UPDATE
  yes_count   = VALUES(yes_count),
  no_count    = VALUES(no_count),
  total_count = VALUES(total_count),
  updated_at  = VALUES(updated_at);

-- ============================================================
-- 5. VOTE RECORDS
-- ============================================================
INSERT INTO vote_records (member_id, question_id, choice, recorded_at, tx_hash, vote_id)
VALUES
  (10130, 5001, 'YES', NOW() - INTERVAL 5 HOUR,   '0xdemo0001', 20001),
  (10139, 5001, 'YES', NOW() - INTERVAL 4 HOUR,   '0xdemo0002', 20002),
  (10134, 5001, 'YES', NOW() - INTERVAL 3 HOUR,   '0xdemo0003', 20003),
  (10131, 5005, 'YES', NOW() - INTERVAL 2 HOUR,   '0xdemo0004', 20004),
  (10138, 5006, 'YES', NOW() - INTERVAL 90 MINUTE,'0xdemo0005', 20005),
  (10144, 5005, 'YES', NOW() - INTERVAL 60 MINUTE,'0xdemo0006', 20006),
  (10142, 5009, 'NO',  NOW() - INTERVAL 30 MINUTE,'0xdemo0007', 20007),
  (10136, 5003, 'YES', NOW() - INTERVAL 7 HOUR,   '0xdemo0008', 20008),
  (10143, 5008, 'YES', NOW() - INTERVAL 3 HOUR,   '0xdemo0009', 20009),
  (10135, 5007, 'NO',  NOW() - INTERVAL 6 HOUR,   '0xdemo0010', 20010),
  (10137, 5004, 'YES', NOW() - INTERVAL 4 HOUR,   '0xdemo0011', 20011),
  (10141, 5008, 'YES', NOW() - INTERVAL 50 MINUTE,'0xdemo0012', 20012),
  (10132, 5009, 'NO',  NOW() - INTERVAL 40 MINUTE,'0xdemo0013', 20013);

-- ============================================================
-- 6. ACTIVITIES
-- ============================================================
INSERT INTO activities (member_id, question_id, activity_type, choice, amount, created_at)
VALUES
  (10130, 5001, 'VOTE', 'YES', 0, NOW() - INTERVAL 5 HOUR),
  (10139, 5001, 'VOTE', 'YES', 0, NOW() - INTERVAL 4 HOUR),
  (10134, 5001, 'VOTE', 'YES', 0, NOW() - INTERVAL 3 HOUR),
  (10131, 5005, 'VOTE', 'YES', 0, NOW() - INTERVAL 2 HOUR),
  (10138, 5006, 'VOTE', 'YES', 0, NOW() - INTERVAL 90 MINUTE),
  (10144, 5005, 'VOTE', 'YES', 0, NOW() - INTERVAL 60 MINUTE),
  (10142, 5009, 'VOTE', 'NO',  0, NOW() - INTERVAL 30 MINUTE),
  (10136, 5003, 'VOTE', 'YES', 0, NOW() - INTERVAL 7 HOUR),
  (10143, 5008, 'VOTE', 'YES', 0, NOW() - INTERVAL 3 HOUR),
  (10141, 5008, 'VOTE', 'YES', 0, NOW() - INTERVAL 50 MINUTE);

-- ============================================================
-- 7. Update existing test USER members
-- ============================================================
UPDATE members SET username = 'alpha_user', display_name = 'Alpha Tester', tier = 'SILVER',
  level = 2, usdc_balance = 300.00, total_predictions = 15, correct_predictions = 9, accuracy_score = 60
WHERE member_id = 10128;

UPDATE members SET username = 'beta_user', display_name = 'Beta Tester', tier = 'BRONZE',
  level = 1, usdc_balance = 120.00, total_predictions = 5, correct_predictions = 2, accuracy_score = 40
WHERE member_id = 10129;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- Verification
-- ============================================================
SELECT 'Done!' AS result;
SELECT q.title, q.category, vs.yes_count, vs.no_count, vs.total_count
FROM questions q JOIN vote_summary vs ON q.question_id = vs.question_id
WHERE q.question_id BETWEEN 5001 AND 5009
ORDER BY vs.total_count DESC;
