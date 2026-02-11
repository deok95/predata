-- Sports domain foundation: leagues, matches 테이블 + questions phase/match_id 컬럼
-- V4__add_sports_domain_tables.sql

-- 1. leagues 테이블
CREATE TABLE IF NOT EXISTS leagues (
    league_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sport_type VARCHAR(20) NOT NULL,
    country_code VARCHAR(2) NULL,
    external_league_id VARCHAR(50) NULL,
    provider VARCHAR(50) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_leagues_sport_type ON leagues(sport_type);
CREATE UNIQUE INDEX idx_leagues_ext_provider ON leagues(external_league_id, provider);

-- 2. matches 테이블
CREATE TABLE IF NOT EXISTS matches (
    match_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    league_id BIGINT NOT NULL,
    home_team VARCHAR(100) NOT NULL,
    away_team VARCHAR(100) NOT NULL,
    home_score INT NULL,
    away_score INT NULL,
    match_status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    match_time DATETIME NOT NULL,
    external_match_id VARCHAR(50) NULL,
    provider VARCHAR(50) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_matches_league FOREIGN KEY (league_id) REFERENCES leagues(league_id)
);

CREATE INDEX idx_matches_status ON matches(match_status);
CREATE INDEX idx_matches_league_time ON matches(league_id, match_time);
CREATE UNIQUE INDEX idx_matches_ext_provider ON matches(external_match_id, provider);

-- 3. questions 테이블에 phase + match_id 추가
ALTER TABLE questions ADD COLUMN phase VARCHAR(20) NULL;
ALTER TABLE questions ADD COLUMN match_id BIGINT NULL;
ALTER TABLE questions ADD CONSTRAINT fk_questions_match FOREIGN KEY (match_id) REFERENCES matches(match_id);
CREATE INDEX idx_questions_phase ON questions(phase);
CREATE INDEX idx_questions_match_id ON questions(match_id);
