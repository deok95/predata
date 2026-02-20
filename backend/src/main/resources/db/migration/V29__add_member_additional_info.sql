-- 회원 추가 정보 컬럼 추가

-- 1. gender 컬럼 추가 (MALE, FEMALE, OTHER)
SET @gender_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'members'
      AND COLUMN_NAME = 'gender'
);
SET @sql = IF(@gender_exists = 0,
    'ALTER TABLE members ADD COLUMN gender VARCHAR(10) NULL AFTER age_group',
    'SELECT "Column gender already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @constraint_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'members'
      AND CONSTRAINT_NAME = 'chk_members_gender'
);
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE members ADD CONSTRAINT chk_members_gender CHECK (gender IN (''MALE'', ''FEMALE'', ''OTHER''))',
    'SELECT "Constraint chk_members_gender already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. birth_date 컬럼 추가
SET @birth_date_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'members'
      AND COLUMN_NAME = 'birth_date'
);
SET @sql = IF(@birth_date_exists = 0,
    'ALTER TABLE members ADD COLUMN birth_date DATE NULL AFTER gender',
    'SELECT "Column birth_date already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 인덱스 추가 (통계 쿼리 최적화)
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'members'
      AND INDEX_NAME = 'idx_members_gender'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_members_gender ON members(gender)',
    'SELECT "Index idx_members_gender already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'members'
      AND INDEX_NAME = 'idx_members_birth_date'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_members_birth_date ON members(birth_date)',
    'SELECT "Index idx_members_birth_date already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'members'
      AND INDEX_NAME = 'idx_members_age_group'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_members_age_group ON members(age_group)',
    'SELECT "Index idx_members_age_group already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'members'
      AND INDEX_NAME = 'idx_members_job_category'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_members_job_category ON members(job_category)',
    'SELECT "Index idx_members_job_category already exists, skipping" as message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
