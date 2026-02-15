-- V14: idempotency_keys 테이블 생성 (리플레이 공격 방지)

SET @table_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'idempotency_keys'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE idempotency_keys (
        idempotency_key_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        idempotency_key VARCHAR(255) NOT NULL COMMENT ''X-Idempotency-Key 헤더값'',
        member_id BIGINT NOT NULL COMMENT ''회원 ID'',
        endpoint VARCHAR(100) NOT NULL COMMENT ''API 엔드포인트'',
        request_hash VARCHAR(64) NOT NULL COMMENT ''요청 본문 SHA-256 해시'',
        response_body TEXT COMMENT ''저장된 응답 (JSON)'',
        response_status INT NOT NULL COMMENT ''HTTP 상태 코드'',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        expires_at DATETIME NOT NULL COMMENT ''만료 시각 (24시간)'',
        INDEX idx_idempotency_key (idempotency_key, member_id),
        INDEX idx_idempotency_expires (expires_at),
        UNIQUE KEY uk_idempotency_key_member (idempotency_key, member_id, endpoint)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''멱등성 키''',
    'SELECT "Table idempotency_keys already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
