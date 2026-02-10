/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-12.1.2-MariaDB, for osx10.20 (arm64)
--
-- Host: localhost    Database: predata
-- ------------------------------------------------------
-- Server version	12.1.2-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Current Database: `predata`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `predata` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `predata`;

--
-- Table structure for table `activities`
--

DROP TABLE IF EXISTS `activities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `activities` (
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
  KEY `fk_question` (`question_id`),
  KEY `idx_parent_bet_id` (`parent_bet_id`),
  CONSTRAINT `fk_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`),
  CONSTRAINT `fk_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20078 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `badge_definitions`
--

DROP TABLE IF EXISTS `badge_definitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `badge_definitions` (
  `badge_id` varchar(40) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text NOT NULL,
  `category` varchar(30) NOT NULL,
  `rarity` varchar(20) NOT NULL DEFAULT 'COMMON',
  `icon_url` varchar(255) DEFAULT NULL,
  `sort_order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`badge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bet_records`
--

DROP TABLE IF EXISTS `bet_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `bet_records` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `daily_faucets`
--

DROP TABLE IF EXISTS `daily_faucets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_faucets` (
  `faucet_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `claimed` tinyint(1) DEFAULT 0,
  `amount` bigint(20) DEFAULT 100,
  `reset_date` date NOT NULL,
  PRIMARY KEY (`faucet_id`),
  UNIQUE KEY `uk_faucet_member_date` (`member_id`,`reset_date`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `daily_tickets`
--

DROP TABLE IF EXISTS `daily_tickets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_tickets` (
  `ticket_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `remaining_count` int(11) DEFAULT 5,
  `reset_date` date NOT NULL,
  PRIMARY KEY (`ticket_id`),
  UNIQUE KEY `uk_member_date` (`member_id`,`reset_date`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `email_verifications`
--

DROP TABLE IF EXISTS `email_verifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_verifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `code` varchar(6) NOT NULL,
  `expires_at` datetime NOT NULL,
  `verified` tinyint(1) DEFAULT 0,
  `attempts` int(11) DEFAULT 0,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_email_verification_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member_badges`
--

DROP TABLE IF EXISTS `member_badges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_badges` (
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `members` (
  `member_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `wallet_address` varchar(42) DEFAULT NULL,
  `country_code` varchar(2) NOT NULL,
  `job_category` varchar(50) DEFAULT NULL,
  `age_group` int(11) DEFAULT NULL,
  `tier` varchar(20) DEFAULT 'BRONZE',
  `tier_weight` decimal(3,2) DEFAULT 1.00,
  `accuracy_score` int(11) DEFAULT 0,
  `total_predictions` int(11) DEFAULT 0,
  `correct_predictions` int(11) DEFAULT 0,
  `point_balance` bigint(20) DEFAULT 0,
  `created_at` datetime DEFAULT current_timestamp(),
  `is_banned` tinyint(1) DEFAULT 0,
  `ban_reason` varchar(500) DEFAULT NULL,
  `banned_at` datetime DEFAULT NULL,
  `signup_ip` varchar(45) DEFAULT NULL,
  `last_ip` varchar(45) DEFAULT NULL,
  `role` varchar(10) NOT NULL DEFAULT 'USER',
  `referral_code` varchar(12) DEFAULT NULL,
  `referred_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `wallet_address` (`wallet_address`),
  UNIQUE KEY `referral_code` (`referral_code`)
) ENGINE=InnoDB AUTO_INCREMENT=10023 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `questions`
--

DROP TABLE IF EXISTS `questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `questions` (
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
  PRIMARY KEY (`question_id`),
  KEY `idx_questions_status` (`status`),
  KEY `idx_questions_type` (`type`),
  KEY `idx_questions_voting_end_at` (`voting_end_at`),
  KEY `idx_questions_betting_start_at` (`betting_start_at`),
  KEY `idx_questions_betting_end_at` (`betting_end_at`)
) ENGINE=InnoDB AUTO_INCREMENT=89 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `referrals`
--

DROP TABLE IF EXISTS `referrals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `referrals` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sports_matches`
--

DROP TABLE IF EXISTS `sports_matches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `sports_matches` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_settings`
--

DROP TABLE IF EXISTS `system_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_settings` (
  `setting_key` varchar(100) NOT NULL,
  `setting_value` text NOT NULL,
  `updated_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vote_records`
--

DROP TABLE IF EXISTS `vote_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `vote_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `choice` enum('YES','NO') NOT NULL,
  `member_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `recorded_at` datetime(6) NOT NULL,
  `tx_hash` varchar(64) NOT NULL,
  `vote_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `predata_betting`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `predata_betting` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

USE `predata_betting`;

--
-- Table structure for table `activities`
--

DROP TABLE IF EXISTS `activities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `activities` (
  `activity_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `activity_type` enum('VOTE','BET') NOT NULL,
  `amount` bigint(20) NOT NULL,
  `choice` enum('YES','NO') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `latency_ms` bigint(20) DEFAULT NULL,
  `member_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  PRIMARY KEY (`activity_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `predata_member`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `predata_member` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

USE `predata_member`;

--
-- Table structure for table `daily_tickets`
--

DROP TABLE IF EXISTS `daily_tickets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_tickets` (
  `ticket_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) NOT NULL,
  `remaining_count` int(11) DEFAULT NULL,
  `reset_date` date NOT NULL,
  PRIMARY KEY (`ticket_id`),
  UNIQUE KEY `uk_member_date` (`member_id`,`reset_date`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `members` (
  `member_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `accuracy_score` int(11) DEFAULT NULL,
  `age_group` int(11) DEFAULT NULL,
  `correct_predictions` int(11) DEFAULT NULL,
  `country_code` varchar(2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `job_category` varchar(50) DEFAULT NULL,
  `point_balance` bigint(20) DEFAULT NULL,
  `tier` enum('BRONZE','SILVER','GOLD','PLATINUM') DEFAULT NULL,
  `tier_weight` decimal(3,2) DEFAULT NULL,
  `total_predictions` int(11) DEFAULT NULL,
  `wallet_address` varchar(42) DEFAULT NULL,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `UK_9d30a9u1qpg8eou0otgkwrp5d` (`email`),
  UNIQUE KEY `UK_s93mn0kiy1g3xc8uadvfvyc4w` (`wallet_address`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `predata_question`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `predata_question` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

USE `predata_question`;

--
-- Table structure for table `questions`
--

DROP TABLE IF EXISTS `questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `questions` (
  `question_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `category` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `final_result` enum('YES','NO','PENDING') DEFAULT NULL,
  `no_bet_pool` bigint(20) DEFAULT NULL,
  `status` enum('OPEN','CLOSED','SETTLED') DEFAULT NULL,
  `type` varchar(20) NOT NULL DEFAULT 'VERIFIABLE',
  `voting_end_at` datetime NOT NULL DEFAULT current_timestamp(),
  `betting_start_at` datetime NOT NULL DEFAULT current_timestamp(),
  `betting_end_at` datetime NOT NULL DEFAULT current_timestamp(),
  `title` varchar(500) NOT NULL,
  `total_bet_pool` bigint(20) DEFAULT NULL,
  `yes_bet_pool` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `predata_settlement`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `predata_settlement` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

USE `predata_settlement`;

--
-- Table structure for table `rewards`
--

DROP TABLE IF EXISTS `rewards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `rewards` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `member_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `reward_type` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `settlements`
--

DROP TABLE IF EXISTS `settlements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `settlements` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `final_result` varchar(255) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `settled_at` datetime(6) NOT NULL,
  `total_bets` int(11) DEFAULT NULL,
  `total_payout` bigint(20) DEFAULT NULL,
  `total_winners` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_blt141if71ung52j7ousu5lf8` (`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `predata_sports`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `predata_sports` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

USE `predata_sports`;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-02-08 10:12:04
