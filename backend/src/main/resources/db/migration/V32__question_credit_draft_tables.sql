-- V32: 크레딧 기반 질문 생성 시스템
-- 테이블: question_credit_accounts, question_credit_ledgers, question_draft_sessions
-- questions 테이블: creator_member_id, question_normalized_hash 컬럼 추가

-- ── question_credit_accounts ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS question_credit_accounts (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    member_id         BIGINT          NOT NULL,
    available_credits INT             NOT NULL DEFAULT 0,
    version           BIGINT          NOT NULL DEFAULT 0,
    created_at        DATETIME(6)     NOT NULL,
    updated_at        DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_qca_member_id UNIQUE (member_id),
    CONSTRAINT fk_qca_member FOREIGN KEY (member_id) REFERENCES members (member_id)
);

-- ── question_credit_ledgers ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS question_credit_ledgers (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    member_id     BIGINT      NOT NULL,
    question_id   BIGINT      NULL,
    delta         INT         NOT NULL,
    reason        VARCHAR(50) NOT NULL,
    balance_after INT         NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_qcl_member   FOREIGN KEY (member_id)   REFERENCES members (member_id),
    CONSTRAINT fk_qcl_question FOREIGN KEY (question_id) REFERENCES questions (question_id)
);

CREATE INDEX IF NOT EXISTS idx_qcl_member_created
    ON question_credit_ledgers (member_id, created_at);

-- ── question_draft_sessions ─────────────────────────────────────────────────
-- active_member_id: OPEN 상태일 때만 memberId 설정, UNIQUE 제약으로 회원당 OPEN 1개 DB 보장
-- NULL은 유니크 제약에서 제외(SQL 표준) → CONSUMED/EXPIRED/CANCELLED 는 NULL 허용
CREATE TABLE IF NOT EXISTS question_draft_sessions (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    draft_id                VARCHAR(36) NOT NULL,
    member_id               BIGINT      NOT NULL,
    active_member_id        BIGINT      NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    expires_at              DATETIME(6) NOT NULL,
    submit_idempotency_key  VARCHAR(36) NOT NULL,
    submitted_question_id   BIGINT      NULL,
    consumed_at             DATETIME(6) NULL,
    created_at              DATETIME(6) NOT NULL,
    updated_at              DATETIME(6) NOT NULL,
    version                 BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_qds_draft_id       UNIQUE (draft_id),
    CONSTRAINT uk_qds_member_idem    UNIQUE (member_id, submit_idempotency_key),
    CONSTRAINT uk_qds_active_member  UNIQUE (active_member_id),
    CONSTRAINT fk_qds_member         FOREIGN KEY (member_id)             REFERENCES members (member_id),
    CONSTRAINT fk_qds_question       FOREIGN KEY (submitted_question_id) REFERENCES questions (question_id)
);

CREATE INDEX IF NOT EXISTS idx_qds_member_status_expires
    ON question_draft_sessions (member_id, status, expires_at);

-- 기존 테이블 호환: version 컬럼 누락 시 추가
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'question_draft_sessions'
      AND COLUMN_NAME  = 'version'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE question_draft_sessions ADD COLUMN version BIGINT NOT NULL DEFAULT 0',
    'SELECT "Column version already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── questions: creator_member_id 컬럼 추가 ──────────────────────────────────
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'creator_member_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN creator_member_id BIGINT NULL COMMENT ''크레딧 기반 생성자 memberId (어드민 생성 질문은 NULL)''',
    'SELECT "Column creator_member_id already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── questions: question_normalized_hash 컬럼 추가 ───────────────────────────
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'question_normalized_hash'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN question_normalized_hash VARCHAR(255) NULL COMMENT ''중복 질문 방지용 정규화 해시''',
    'SELECT "Column question_normalized_hash already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── questions: question_normalized_hash 유니크 인덱스 ──────────────────────
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND INDEX_NAME   = 'uk_questions_normalized_hash'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE UNIQUE INDEX uk_questions_normalized_hash ON questions (question_normalized_hash)',
    'SELECT "Index uk_questions_normalized_hash already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
