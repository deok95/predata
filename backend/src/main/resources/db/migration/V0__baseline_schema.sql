-- Baseline schema for clean DB bootstrap
-- Generated from current predata schema (excluding flyway_schema_history)
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `activities` (
  `activity_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `activity_type` enum('VOTE','BET','BET_SELL') NOT NULL,
  `choice` enum('YES','NO') NOT NULL,
  `amount` bigint(20) DEFAULT 0,
  `latency_ms` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `ip_address` varchar(45) DEFAULT NULL,
  `parent_bet_id` bigint(20) DEFAULT NULL COMMENT 'BET_SELLì¼ ë•Œ ì›ë³¸ ë² íŒ… ID ì¶”ì ',
  PRIMARY KEY (`activity_id`),
  UNIQUE KEY `uk_member_question_type` (`member_id`,`question_id`,`activity_type`),
  UNIQUE KEY `uk_activities_parent_bet_id` (`parent_bet_id`),
  KEY `idx_parent_bet_id` (`parent_bet_id`),
  KEY `idx_activity_question_type` (`question_id`,`activity_type`),
  KEY `idx_activity_ip` (`ip_address`),
  KEY `idx_activity_member_question` (`member_id`,`question_id`),
  KEY `idx_activity_type` (`activity_type`),
  KEY `idx_activity_parent_bet` (`parent_bet_id`),
  KEY `idx_activities_question_type` (`question_id`,`activity_type`),
  CONSTRAINT `fk_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`),
  CONSTRAINT `fk_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20084 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `audit_logs` (
  `audit_log_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) DEFAULT NULL COMMENT '회원 ID (NULL 가능 - 시스템 작업)',
  `action` varchar(50) NOT NULL COMMENT '감사 액션 (ORDER_CREATE, SETTLE, etc.)',
  `entity_type` varchar(50) NOT NULL COMMENT '엔티티 타입 (ORDER, QUESTION, POSITION, etc.)',
  `entity_id` bigint(20) DEFAULT NULL COMMENT '엔티티 ID',
  `detail` text DEFAULT NULL COMMENT '상세 정보',
  `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP 주소 (IPv4/IPv6)',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '생성 시간',
  PRIMARY KEY (`audit_log_id`),
  KEY `idx_audit_member` (`member_id`,`created_at` DESC),
  KEY `idx_audit_action` (`action`,`created_at` DESC),
  KEY `idx_audit_entity` (`entity_type`,`entity_id`),
  CONSTRAINT `fk_audit_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=131 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='감사 로그';

CREATE TABLE IF NOT EXISTS `badge_definitions` (
  `badge_id` varchar(40) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text NOT NULL,
  `category` varchar(30) NOT NULL,
  `rarity` varchar(20) NOT NULL DEFAULT 'COMMON',
  `icon_url` varchar(255) DEFAULT NULL,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`badge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE IF NOT EXISTS `bet_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` bigint(20) NOT NULL,
  `bet_id` bigint(20) NOT NULL,
  `member_id` bigint(20) NOT NULL,
  `position` enum('LONG','SHORT') NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `recorded_at` datetime(6) NOT NULL,
  `tx_hash` varchar(64) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `daily_faucets` (
  `faucet_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `claimed` tinyint(1) DEFAULT 0,
  `amount` bigint(20) DEFAULT 100,
  `reset_date` date NOT NULL,
  PRIMARY KEY (`faucet_id`),
  UNIQUE KEY `uk_faucet_member_date` (`member_id`,`reset_date`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `daily_tickets` (
  `ticket_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `remaining_count` int(11) DEFAULT 5,
  `reset_date` date NOT NULL,
  PRIMARY KEY (`ticket_id`),
  UNIQUE KEY `uk_member_date` (`member_id`,`reset_date`),
  KEY `idx_daily_ticket_member_date` (`member_id`,`reset_date`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `email_verifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `code` varchar(6) NOT NULL,
  `expires_at` datetime NOT NULL,
  `verified` tinyint(1) DEFAULT 0,
  `attempts` int(11) DEFAULT 0,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_email_verification_email` (`email`),
  KEY `idx_email_verification_email_created` (`email`,`created_at` DESC),
  KEY `idx_email_verification_expires` (`expires_at`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fee_pool_ledgers` (
  `ledger_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fee_pool_id` bigint(20) NOT NULL,
  `action` enum('FEE_COLLECTED','PLATFORM_WITHDRAWN','REWARD_DISTRIBUTED','RESERVE_ALLOCATED') NOT NULL,
  `amount` decimal(18,6) NOT NULL,
  `balance` decimal(18,6) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  PRIMARY KEY (`ledger_id`),
  KEY `idx_fee_pool_ledger_fee_pool_id` (`fee_pool_id`),
  KEY `idx_fee_pool_ledger_action` (`action`),
  KEY `idx_fee_pool_ledger_created_at` (`created_at`),
  CONSTRAINT `1` FOREIGN KEY (`fee_pool_id`) REFERENCES `fee_pools` (`fee_pool_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fee_pools` (
  `fee_pool_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL,
  `total_fees` decimal(18,6) NOT NULL DEFAULT 0.000000,
  `platform_share` decimal(18,6) NOT NULL DEFAULT 0.000000,
  `reward_pool_share` decimal(18,6) NOT NULL DEFAULT 0.000000,
  `reserve_share` decimal(18,6) NOT NULL DEFAULT 0.000000,
  `created_at` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  `updated_at` datetime(6) NOT NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`fee_pool_id`),
  UNIQUE KEY `question_id` (`question_id`),
  CONSTRAINT `1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `idempotency_keys` (
  `idempotency_key_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `idempotency_key` varchar(255) NOT NULL COMMENT 'X-Idempotency-Key 헤더값',
  `member_id` bigint(20) NOT NULL COMMENT '회원 ID',
  `endpoint` varchar(100) NOT NULL COMMENT 'API 엔드포인트',
  `request_hash` varchar(64) NOT NULL COMMENT '요청 본문 SHA-256 해시',
  `response_body` text DEFAULT NULL COMMENT '저장된 응답 (JSON)',
  `response_status` int(11) NOT NULL COMMENT 'HTTP 상태 코드',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `expires_at` datetime NOT NULL COMMENT '만료 시각 (24시간)',
  PRIMARY KEY (`idempotency_key_id`),
  UNIQUE KEY `uk_idempotency_key_member` (`idempotency_key`,`member_id`,`endpoint`),
  KEY `idx_idempotency_key` (`idempotency_key`,`member_id`),
  KEY `idx_idempotency_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='멱등성 키';

CREATE TABLE IF NOT EXISTS `leagues` (
  `league_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `sport_type` enum('FOOTBALL','BASKETBALL','BASEBALL','ESPORTS') NOT NULL,
  `country_code` varchar(2) DEFAULT NULL,
  `external_league_id` varchar(50) DEFAULT NULL,
  `provider` varchar(50) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`league_id`),
  UNIQUE KEY `idx_leagues_ext_provider` (`external_league_id`,`provider`),
  KEY `idx_leagues_sport_type` (`sport_type`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `market_pools` (
  `question_id` bigint(20) NOT NULL,
  `yes_shares` decimal(38,18) NOT NULL,
  `no_shares` decimal(38,18) NOT NULL,
  `fee_rate` decimal(6,5) NOT NULL,
  `collateral_locked` decimal(38,18) NOT NULL DEFAULT 0.000000000000000000,
  `total_volume_usdc` decimal(38,18) NOT NULL DEFAULT 0.000000000000000000,
  `total_fees_usdc` decimal(38,18) NOT NULL DEFAULT 0.000000000000000000,
  `status` enum('ACTIVE','PAUSED','SETTLED') NOT NULL,
  `version` bigint(20) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`question_id`),
  KEY `idx_market_pools_status` (`status`),
  CONSTRAINT `fk_market_pools_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_market_pools_status` CHECK (`status` in ('ACTIVE','PAUSED','SETTLED')),
  CONSTRAINT `chk_market_pools_shares_positive` CHECK (`yes_shares` > 0 and `no_shares` > 0),
  CONSTRAINT `chk_market_pools_fee_rate` CHECK (`fee_rate` >= 0 and `fee_rate` < 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `matches` (
  `match_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `league_id` bigint(20) NOT NULL,
  `home_team` varchar(100) NOT NULL,
  `away_team` varchar(100) NOT NULL,
  `home_score` int(11) DEFAULT NULL,
  `away_score` int(11) DEFAULT NULL,
  `match_status` enum('SCHEDULED','LIVE','HALFTIME','FINISHED','POSTPONED','CANCELLED') NOT NULL,
  `match_time` datetime NOT NULL,
  `external_match_id` varchar(50) DEFAULT NULL,
  `provider` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`match_id`),
  UNIQUE KEY `idx_matches_ext_provider` (`external_match_id`,`provider`),
  KEY `idx_matches_status` (`match_status`),
  KEY `idx_matches_league_time` (`league_id`,`match_time`),
  CONSTRAINT `fk_matches_league` FOREIGN KEY (`league_id`) REFERENCES `leagues` (`league_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `member_badges` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `badge_id` varchar(40) NOT NULL,
  `progress` int(11) NOT NULL DEFAULT 0,
  `target` int(11) NOT NULL DEFAULT 1,
  `awarded_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_member_badge` (`member_id`,`badge_id`),
  KEY `idx_member_badges_member` (`member_id`),
  KEY `fk_member_badges_badge` (`badge_id`),
  CONSTRAINT `fk_member_badges_badge` FOREIGN KEY (`badge_id`) REFERENCES `badge_definitions` (`badge_id`),
  CONSTRAINT `fk_member_badges_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE IF NOT EXISTS `members` (
  `member_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `wallet_address` varchar(42) DEFAULT NULL,
  `country_code` varchar(2) NOT NULL,
  `job_category` varchar(50) DEFAULT NULL,
  `age_group` int(11) DEFAULT NULL,
  `gender` enum('MALE','FEMALE','OTHER') DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `tier` varchar(20) DEFAULT 'BRONZE',
  `tier_weight` decimal(3,2) NOT NULL DEFAULT 1.00,
  `accuracy_score` int(11) NOT NULL DEFAULT 0,
  `total_predictions` int(11) NOT NULL DEFAULT 0,
  `correct_predictions` int(11) NOT NULL DEFAULT 0,
  `created_at` datetime DEFAULT current_timestamp(),
  `is_banned` tinyint(1) DEFAULT 0,
  `ban_reason` varchar(500) DEFAULT NULL,
  `banned_at` datetime DEFAULT NULL,
  `signup_ip` varchar(45) DEFAULT NULL,
  `last_ip` varchar(45) DEFAULT NULL,
  `role` varchar(10) NOT NULL DEFAULT 'USER',
  `referral_code` varchar(12) DEFAULT NULL,
  `referred_by` bigint(20) DEFAULT NULL,
  `google_id` varchar(255) DEFAULT NULL,
  `usdc_balance` decimal(18,6) DEFAULT 0.000000,
  `has_voting_pass` tinyint(1) NOT NULL DEFAULT 0,
  `level` int(11) NOT NULL DEFAULT 1 COMMENT '레벨 (1~5, 보상 가중치 계산용)',
  `point_balance` decimal(18,6) NOT NULL DEFAULT 0.000000 COMMENT '포인트 잔액 (리워드 수령)',
  `version` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `wallet_address` (`wallet_address`),
  UNIQUE KEY `referral_code` (`referral_code`),
  UNIQUE KEY `UK_pta369mmitbsdf8iy3t07rc0q` (`google_id`),
  UNIQUE KEY `google_id` (`google_id`),
  KEY `idx_member_signup_ip` (`signup_ip`),
  KEY `idx_member_last_ip` (`last_ip`),
  KEY `idx_member_wallet` (`wallet_address`),
  KEY `idx_member_tier` (`tier`),
  KEY `idx_member_accuracy` (`accuracy_score` DESC),
  KEY `idx_members_country` (`country_code`),
  KEY `idx_members_job` (`job_category`),
  KEY `idx_members_age` (`age_group`),
  KEY `idx_google_id` (`google_id`),
  KEY `idx_members_gender` (`gender`),
  KEY `idx_members_birth_date` (`birth_date`),
  KEY `idx_members_age_group` (`age_group`),
  KEY `idx_members_job_category` (`job_category`),
  CONSTRAINT `chk_members_gender` CHECK (`gender` in ('MALE','FEMALE','OTHER'))
) ENGINE=InnoDB AUTO_INCREMENT=10027 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notifications` (
  `notification_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `type` varchar(30) NOT NULL,
  `title` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `related_question_id` bigint(20) DEFAULT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  PRIMARY KEY (`notification_id`),
  KEY `idx_notifications_member` (`member_id`),
  KEY `idx_notifications_member_read` (`member_id`,`is_read`),
  CONSTRAINT `fk_notifications_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE IF NOT EXISTS `orders` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `member_id` bigint(20) NOT NULL,
  `order_type` enum('BUY','SELL') NOT NULL,
  `price` decimal(4,2) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `remaining_amount` bigint(20) NOT NULL,
  `side` enum('YES','NO') NOT NULL,
  `status` enum('OPEN','FILLED','PARTIAL','CANCELLED') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `direction` enum('BUY','SELL') NOT NULL,
  PRIMARY KEY (`order_id`),
  KEY `idx_orders_direction` (`direction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `payment_transactions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` decimal(18,6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `member_id` bigint(20) NOT NULL,
  `quantity` int(11) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `tx_hash` varchar(66) NOT NULL,
  `type` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_tx_hash` (`tx_hash`),
  KEY `idx_member_payments` (`member_id`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `positions` (
  `position_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `side` enum('YES','NO') NOT NULL,
  `quantity` decimal(19,2) NOT NULL COMMENT '포지션 수량',
  `reserved_quantity` decimal(19,2) NOT NULL DEFAULT 0.00 COMMENT '미체결 SELL 주문으로 예약된 수량',
  `avg_price` decimal(4,2) NOT NULL COMMENT '평균 매수가',
  `version` bigint(20) NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
  `settled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '정산 완료 여부',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`position_id`),
  UNIQUE KEY `uk_position_member_question_side` (`member_id`,`question_id`,`side`),
  UNIQUE KEY `UKjva5wegm980et88frj50k6rug` (`member_id`,`question_id`,`side`),
  KEY `idx_positions_member_id` (`member_id`),
  KEY `idx_positions_question_id` (`question_id`),
  KEY `idx_positions_settled` (`question_id`,`settled`)
) ENGINE=InnoDB AUTO_INCREMENT=131 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='포지션 원장';

CREATE TABLE IF NOT EXISTS `price_history` (
  `price_history_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL,
  `mid_price` decimal(4,2) DEFAULT NULL COMMENT '중간 가격 (bestBid + bestAsk) / 2',
  `last_trade_price` decimal(4,2) DEFAULT NULL COMMENT '최근 체결가',
  `spread` decimal(4,2) DEFAULT NULL COMMENT '스프레드 (bestAsk - bestBid)',
  `timestamp` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`price_history_id`),
  KEY `idx_price_history_question_id` (`question_id`),
  KEY `idx_price_history_timestamp` (`question_id`,`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='가격 이력';

CREATE TABLE IF NOT EXISTS `question_generation_batches` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_id` varchar(64) NOT NULL,
  `subcategory` varchar(50) NOT NULL,
  `target_date` date NOT NULL,
  `status` varchar(20) NOT NULL,
  `requested_count` int(11) NOT NULL DEFAULT 3,
  `accepted_count` int(11) NOT NULL DEFAULT 0,
  `rejected_count` int(11) NOT NULL DEFAULT 0,
  `dry_run` tinyint(1) NOT NULL DEFAULT 0,
  `message` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_generation_batch_id` (`batch_id`),
  UNIQUE KEY `uk_generation_batch_daily` (`subcategory`,`target_date`),
  KEY `idx_generation_batches_status_created` (`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE IF NOT EXISTS `question_generation_items` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_id` varchar(64) NOT NULL,
  `draft_id` varchar(64) NOT NULL,
  `title` text NOT NULL,
  `category` varchar(50) NOT NULL,
  `subcategory` varchar(50) NOT NULL,
  `market_type` varchar(20) NOT NULL,
  `question_type` varchar(20) NOT NULL,
  `vote_result_settlement` tinyint(1) NOT NULL DEFAULT 0,
  `resolution_rule` text NOT NULL,
  `resolution_source` varchar(500) DEFAULT NULL,
  `resolve_at` datetime NOT NULL,
  `voting_end_at` datetime NOT NULL,
  `break_minutes` int(11) NOT NULL,
  `betting_start_at` datetime NOT NULL,
  `betting_end_at` datetime NOT NULL,
  `reveal_start_at` datetime NOT NULL,
  `reveal_end_at` datetime NOT NULL,
  `confidence` decimal(5,4) NOT NULL DEFAULT 0.0000,
  `duplicate_score` decimal(5,4) NOT NULL DEFAULT 0.0000,
  `rationale` text NOT NULL,
  `references_json` text DEFAULT NULL,
  `risk_flags_json` text DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `reject_reason` text DEFAULT NULL,
  `published_question_id` bigint(20) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_generation_item_draft` (`draft_id`),
  KEY `idx_generation_items_batch_status` (`batch_id`,`status`),
  KEY `idx_generation_items_published` (`published_question_id`),
  CONSTRAINT `fk_generation_item_batch` FOREIGN KEY (`batch_id`) REFERENCES `question_generation_batches` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE IF NOT EXISTS `questions` (
  `question_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` text NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `category_weight` decimal(3,2) DEFAULT 1.00,
  `status` enum('VOTING','BREAK','BETTING','SETTLED') NOT NULL DEFAULT 'VOTING',
  `type` enum('VERIFIABLE','OPINION') NOT NULL DEFAULT 'VERIFIABLE',
  `voting_end_at` datetime NOT NULL,
  `betting_start_at` datetime NOT NULL,
  `betting_end_at` datetime NOT NULL,
  `total_bet_pool` bigint(20) DEFAULT 0,
  `yes_bet_pool` bigint(20) DEFAULT 0,
  `no_bet_pool` bigint(20) DEFAULT 0,
  `final_result` enum('YES','NO','PENDING') DEFAULT 'PENDING',
  `expired_at` datetime NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `source_url` text DEFAULT NULL,
  `dispute_deadline` datetime DEFAULT NULL,
  `market_type` varchar(20) NOT NULL DEFAULT 'VERIFIABLE',
  `resolution_rule` text NOT NULL,
  `resolution_source` varchar(500) DEFAULT NULL,
  `resolve_at` datetime DEFAULT NULL,
  `dispute_until` datetime DEFAULT NULL,
  `voting_phase` varchar(30) NOT NULL DEFAULT 'VOTING_COMMIT_OPEN' COMMENT '투표 단계 (VOTING_COMMIT_OPEN, VOTING_REVEAL_OPEN, etc.)',
  `initial_yes_pool` bigint(20) NOT NULL DEFAULT 500,
  `initial_no_pool` bigint(20) NOT NULL DEFAULT 500,
  `phase` enum('UPCOMING','VOTING','BETTING','LIVE','FINISHED','SETTLED') DEFAULT NULL,
  `execution_model` varchar(20) NOT NULL DEFAULT 'ORDERBOOK_LEGACY',
  `match_id` bigint(20) DEFAULT NULL,
  `vote_result_settlement` tinyint(1) NOT NULL DEFAULT 0,
  `view_count` bigint(20) NOT NULL DEFAULT 0,
  `sponsor_name` varchar(100) DEFAULT NULL COMMENT 'Sponsor name for BRANDED questions',
  `sponsor_logo` varchar(500) DEFAULT NULL COMMENT 'Sponsor logo URL for BRANDED questions',
  `resolution_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL or AUTO settlement type',
  `resolution_config` text DEFAULT NULL COMMENT 'JSON config for auto-resolution (matchId, asset, condition, etc)',
  PRIMARY KEY (`question_id`),
  KEY `idx_questions_status` (`status`),
  KEY `idx_questions_type` (`type`),
  KEY `idx_questions_voting_end_at` (`voting_end_at`),
  KEY `idx_questions_betting_start_at` (`betting_start_at`),
  KEY `idx_questions_betting_end_at` (`betting_end_at`),
  KEY `idx_question_status` (`status`),
  KEY `idx_question_category` (`category`),
  KEY `idx_question_expired_at` (`expired_at`),
  KEY `idx_question_betting_times` (`betting_start_at`,`betting_end_at`),
  KEY `idx_question_betting_available` (`status`,`expired_at`),
  KEY `idx_question_voting_available` (`status`,`voting_end_at`),
  KEY `idx_question_settled` (`status`,`final_result`),
  KEY `idx_questions_voting_phase` (`voting_phase`),
  KEY `idx_questions_phase` (`phase`),
  KEY `idx_questions_match_id` (`match_id`),
  KEY `idx_questions_execution_model` (`execution_model`),
  KEY `idx_questions_resolution_type_status` (`resolution_type`,`status`),
  KEY `idx_questions_category` (`category`),
  CONSTRAINT `fk_questions_match` FOREIGN KEY (`match_id`) REFERENCES `matches` (`match_id`),
  CONSTRAINT `chk_questions_execution_model` CHECK (`execution_model` in ('AMM_FPMM','ORDERBOOK_LEGACY'))
) ENGINE=InnoDB AUTO_INCREMENT=481 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `referrals` (
  `referral_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `referrer_id` bigint(20) NOT NULL,
  `referee_id` bigint(20) NOT NULL,
  `referral_code` varchar(12) NOT NULL,
  `referrer_reward` bigint(20) NOT NULL DEFAULT 500,
  `referee_reward` bigint(20) NOT NULL DEFAULT 500,
  `created_at` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  PRIMARY KEY (`referral_id`),
  UNIQUE KEY `uq_referee` (`referee_id`),
  KEY `idx_referrals_referrer` (`referrer_id`),
  KEY `idx_referrals_code` (`referral_code`),
  CONSTRAINT `fk_referrals_referee` FOREIGN KEY (`referee_id`) REFERENCES `members` (`member_id`),
  CONSTRAINT `fk_referrals_referrer` FOREIGN KEY (`referrer_id`) REFERENCES `members` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE IF NOT EXISTS `reward_distributions` (
  `reward_distribution_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL,
  `member_id` bigint(20) NOT NULL,
  `amount` decimal(18,6) NOT NULL,
  `status` enum('PENDING','SUCCESS','FAILED') NOT NULL,
  `idempotency_key` varchar(255) NOT NULL,
  `attempts` int(11) NOT NULL DEFAULT 0,
  `error_message` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  `completed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`reward_distribution_id`),
  UNIQUE KEY `idempotency_key` (`idempotency_key`),
  UNIQUE KEY `uk_reward_distribution_idempotency` (`idempotency_key`),
  KEY `idx_reward_distribution_question_id` (`question_id`),
  KEY `idx_reward_distribution_member_id` (`member_id`),
  KEY `idx_reward_distribution_status` (`status`),
  CONSTRAINT `1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE,
  CONSTRAINT `2` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sports_matches` (
  `match_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) DEFAULT NULL,
  `external_api_id` varchar(100) NOT NULL,
  `sport_type` varchar(50) NOT NULL,
  `league_name` varchar(100) NOT NULL,
  `home_team` varchar(100) NOT NULL,
  `away_team` varchar(100) NOT NULL,
  `match_date` datetime NOT NULL,
  `result` varchar(20) DEFAULT NULL,
  `home_score` int(11) DEFAULT NULL,
  `away_score` int(11) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'SCHEDULED',
  `betting_suspended_until` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`match_id`),
  UNIQUE KEY `external_api_id` (`external_api_id`),
  KEY `idx_external_api_id` (`external_api_id`),
  KEY `idx_status` (`status`),
  KEY `idx_match_date` (`match_date`),
  KEY `idx_question_id` (`question_id`),
  CONSTRAINT `fk_sports_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `swap_history` (
  `swap_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `action` enum('BUY','SELL') NOT NULL,
  `outcome` enum('YES','NO') NOT NULL,
  `usdc_in` decimal(38,18) NOT NULL,
  `usdc_out` decimal(38,18) NOT NULL,
  `shares_in` decimal(38,18) NOT NULL,
  `shares_out` decimal(38,18) NOT NULL,
  `fee_usdc` decimal(38,18) NOT NULL,
  `price_before_yes` decimal(6,4) NOT NULL,
  `price_after_yes` decimal(6,4) NOT NULL,
  `yes_before` decimal(38,18) NOT NULL,
  `no_before` decimal(38,18) NOT NULL,
  `yes_after` decimal(38,18) NOT NULL,
  `no_after` decimal(38,18) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`swap_id`),
  KEY `idx_swap_history_question_created` (`question_id`,`created_at`),
  KEY `idx_swap_history_member_created` (`member_id`,`created_at`),
  CONSTRAINT `fk_swap_history_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_swap_history_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_swap_history_action` CHECK (`action` in ('BUY','SELL')),
  CONSTRAINT `chk_swap_history_outcome` CHECK (`outcome` in ('YES','NO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_settings` (
  `setting_key` varchar(100) NOT NULL,
  `setting_value` text NOT NULL,
  `updated_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `trades` (
  `trade_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` bigint(20) NOT NULL,
  `taker_order_id` bigint(20) NOT NULL,
  `executed_at` datetime(6) NOT NULL,
  `price` decimal(4,2) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `maker_order_id` bigint(20) NOT NULL,
  `side` enum('YES','NO') NOT NULL,
  PRIMARY KEY (`trade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `transaction_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `type` varchar(20) NOT NULL,
  `amount` decimal(18,6) NOT NULL,
  `balance_after` decimal(18,6) NOT NULL,
  `description` varchar(255) NOT NULL,
  `question_id` bigint(20) DEFAULT NULL,
  `tx_hash` varchar(66) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_th_member` (`member_id`,`created_at` DESC),
  KEY `idx_th_member_type` (`member_id`,`type`,`created_at` DESC),
  CONSTRAINT `fk_th_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE IF NOT EXISTS `trend_signals` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `signal_date` date NOT NULL,
  `subcategory` varchar(50) NOT NULL,
  `keyword` varchar(255) NOT NULL,
  `trend_score` int(11) NOT NULL,
  `region` varchar(10) NOT NULL DEFAULT 'US',
  `source` varchar(50) NOT NULL DEFAULT 'GOOGLE_TRENDS',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_trend_signals_date_subcategory` (`signal_date`,`subcategory`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE IF NOT EXISTS `user_shares` (
  `member_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `outcome` enum('YES','NO') NOT NULL,
  `shares` decimal(38,18) NOT NULL DEFAULT 0.000000000000000000,
  `cost_basis_usdc` decimal(38,18) NOT NULL DEFAULT 0.000000000000000000,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`member_id`,`question_id`,`outcome`),
  KEY `idx_user_shares_question_outcome` (`question_id`,`outcome`),
  KEY `idx_user_shares_member` (`member_id`),
  CONSTRAINT `fk_user_shares_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_shares_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_user_shares_outcome` CHECK (`outcome` in ('YES','NO')),
  CONSTRAINT `chk_user_shares_shares_nonnegative` CHECK (`shares` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `vote_commits` (
  `vote_commit_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL COMMENT '회원 ID',
  `question_id` bigint(20) NOT NULL COMMENT '질문 ID',
  `commit_hash` varchar(64) NOT NULL COMMENT 'SHA-256 해시 (choice + salt)',
  `revealed_choice` enum('YES','NO') DEFAULT NULL,
  `committed_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '커밋 시각',
  `revealed_at` datetime DEFAULT NULL COMMENT '공개 시각',
  `status` enum('COMMITTED','REVEALED','EXPIRED') NOT NULL,
  `version` bigint(20) DEFAULT 0 COMMENT '낙관적 잠금 버전',
  PRIMARY KEY (`vote_commit_id`),
  UNIQUE KEY `uk_vote_commit_member_question` (`question_id`,`member_id`),
  KEY `idx_vote_commit_status` (`status`,`committed_at`),
  KEY `idx_vote_commit_question` (`question_id`),
  KEY `fk_vote_commit_member` (`member_id`),
  CONSTRAINT `fk_vote_commit_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vote_commit_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Commit-Reveal 투표';

CREATE TABLE IF NOT EXISTS `vote_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `choice` enum('YES','NO') NOT NULL,
  `member_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `recorded_at` datetime(6) NOT NULL,
  `tx_hash` varchar(64) NOT NULL,
  `vote_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
