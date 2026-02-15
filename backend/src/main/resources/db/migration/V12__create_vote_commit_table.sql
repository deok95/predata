-- V12: vote_commits 테이블 생성 (Commit-Reveal 투표 시스템)

-- Check if table already exists
SET @table_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'vote_commits'
);

-- Create table if it doesn't exist
SET @sql = IF(@table_exists = 0,
    'CREATE TABLE vote_commits (
        vote_commit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        member_id BIGINT NOT NULL COMMENT ''회원 ID'',
        question_id BIGINT NOT NULL COMMENT ''질문 ID'',
        commit_hash VARCHAR(64) NOT NULL COMMENT ''SHA-256 해시 (choice + salt)'',
        salt VARCHAR(64) NOT NULL COMMENT ''클라이언트 생성 salt (검증용)'',
        revealed_choice VARCHAR(3) DEFAULT NULL COMMENT ''공개된 선택 (YES/NO)'',
        committed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''커밋 시각'',
        revealed_at DATETIME DEFAULT NULL COMMENT ''공개 시각'',
        status VARCHAR(20) NOT NULL DEFAULT ''COMMITTED'' COMMENT ''상태 (COMMITTED/REVEALED/EXPIRED)'',
        version BIGINT DEFAULT 0 COMMENT ''낙관적 잠금 버전'',

        UNIQUE KEY uk_vote_commit_member_question (question_id, member_id),
        INDEX idx_vote_commit_status (status, committed_at),
        INDEX idx_vote_commit_question (question_id),

        CONSTRAINT fk_vote_commit_member FOREIGN KEY (member_id)
            REFERENCES members(member_id) ON DELETE CASCADE,
        CONSTRAINT fk_vote_commit_question FOREIGN KEY (question_id)
            REFERENCES questions(question_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''Commit-Reveal 투표''',
    'SELECT "Table vote_commits already exists, skipping" as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
