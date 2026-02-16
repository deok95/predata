-- V11: AuditLog 테이블 생성 (감사 로그)

-- Check if table already exists
SET @table_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'audit_logs'
);

-- Create table if it doesn't exist
SET @sql = IF(@table_exists = 0,
    'CREATE TABLE audit_logs (
        audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        member_id BIGINT DEFAULT NULL COMMENT ''회원 ID (NULL 가능 - 시스템 작업)'',
        action VARCHAR(50) NOT NULL COMMENT ''감사 액션 (ORDER_CREATE, SETTLE, etc.)'',
        entity_type VARCHAR(50) NOT NULL COMMENT ''엔티티 타입 (ORDER, QUESTION, POSITION, etc.)'',
        entity_id BIGINT DEFAULT NULL COMMENT ''엔티티 ID'',
        detail TEXT COMMENT ''상세 정보'',
        ip_address VARCHAR(45) COMMENT ''IP 주소 (IPv4/IPv6)'',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''생성 시간'',
        INDEX idx_audit_member (member_id, created_at DESC),
        INDEX idx_audit_action (action, created_at DESC),
        INDEX idx_audit_entity (entity_type, entity_id),
        CONSTRAINT fk_audit_member FOREIGN KEY (member_id)
            REFERENCES members(member_id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''감사 로그''',
    'SELECT "Table audit_logs already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
