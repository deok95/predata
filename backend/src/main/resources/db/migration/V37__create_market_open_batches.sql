-- V37: market_open_batches 테이블 생성
-- 목적: 카테고리별 Top3 선별 → BETTING 오픈 배치 실행 단위 추적
-- 멱등성: IF NOT EXISTS 보장

CREATE TABLE IF NOT EXISTS market_open_batches (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    cutoff_slot_utc DATETIME     NOT NULL COMMENT '배치 기준 UTC 시각 슬롯 (유니크 키)',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|SELECTED|OPENING|COMPLETED|PARTIAL_FAILED|FAILED',
    started_at      DATETIME     NOT NULL COMMENT '배치 시작 시각 (UTC)',
    finished_at     DATETIME     NULL     COMMENT '배치 완료 시각 (UTC)',
    total_candidates INT         NOT NULL DEFAULT 0 COMMENT '후보 질문 총 수',
    selected_count   INT         NOT NULL DEFAULT 0 COMMENT 'Top3 선별 수',
    opened_count     INT         NOT NULL DEFAULT 0 COMMENT '오픈 성공 수',
    failed_count     INT         NOT NULL DEFAULT 0 COMMENT '오픈 실패 수',
    error_summary    TEXT        NULL     COMMENT '오류 요약 (배치 레벨)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mob_cutoff_slot (cutoff_slot_utc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
