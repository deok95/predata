-- 일일 투표 티켓 테이블 생성
CREATE TABLE IF NOT EXISTS daily_tickets (
    ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    remaining_count INT NOT NULL DEFAULT 5,
    reset_date DATE NOT NULL,
    CONSTRAINT uk_member_date UNIQUE (member_id, reset_date),
    CONSTRAINT fk_daily_ticket_member FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 추가 (조회 성능 향상)
SET @index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'daily_tickets'
      AND INDEX_NAME = 'idx_daily_ticket_member_date'
);

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_daily_ticket_member_date ON daily_tickets(member_id, reset_date)',
    'SELECT "Index idx_daily_ticket_member_date already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
