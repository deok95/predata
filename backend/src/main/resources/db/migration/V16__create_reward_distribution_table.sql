-- members 테이블에 level, point_balance 컬럼 추가
ALTER TABLE members
ADD COLUMN level INT NOT NULL DEFAULT 1 COMMENT '레벨 (1~5, 보상 가중치 계산용)',
ADD COLUMN point_balance DECIMAL(18, 6) NOT NULL DEFAULT 0.000000 COMMENT '포인트 잔액 (리워드 수령)';

-- 리워드 분배 테이블
CREATE TABLE reward_distributions (
    reward_distribution_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    amount DECIMAL(18, 6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    attempts INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6),
    FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE,
    INDEX idx_reward_distribution_question_id (question_id),
    INDEX idx_reward_distribution_member_id (member_id),
    INDEX idx_reward_distribution_status (status),
    CONSTRAINT uk_reward_distribution_idempotency UNIQUE (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
