-- V38: question_market_candidates 테이블 생성
-- 목적: 질문별 선별/오픈 결과 저장 (배치당 질문 단위 추적)
-- 멱등성: IF NOT EXISTS 보장

CREATE TABLE IF NOT EXISTS question_market_candidates (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    batch_id         BIGINT      NOT NULL COMMENT '소속 배치 ID (FK → market_open_batches)',
    question_id      BIGINT      NOT NULL COMMENT '대상 질문 ID (FK → questions)',
    category         VARCHAR(50) NOT NULL COMMENT '질문 카테고리',
    vote_count       BIGINT      NOT NULL DEFAULT 0 COMMENT '투표 총 수 (선별 시점 스냅샷)',
    rank_in_category INT         NOT NULL COMMENT '카테고리 내 랭킹 (1-based)',
    selection_status VARCHAR(20) NOT NULL COMMENT 'ELIGIBLE|SELECTED_TOP3|NOT_SELECTED',
    open_status      VARCHAR(20) NULL     COMMENT 'OPENED|OPEN_FAILED (null=미처리)',
    open_error       TEXT        NULL     COMMENT '오픈 실패 시 에러 메시지',
    created_at       DATETIME    NOT NULL,
    updated_at       DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qmc_batch_question (batch_id, question_id),
    CONSTRAINT fk_qmc_batch
        FOREIGN KEY (batch_id) REFERENCES market_open_batches (id),
    CONSTRAINT fk_qmc_question
        FOREIGN KEY (question_id) REFERENCES questions (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
