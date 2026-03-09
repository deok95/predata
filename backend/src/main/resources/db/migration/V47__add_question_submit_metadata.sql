-- V47: question submit metadata columns (description/tags/source links/thumbnail/boost)

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'description'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN description TEXT NULL',
    'SELECT "Column description already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'thumbnail_url'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN thumbnail_url VARCHAR(500) NULL',
    'SELECT "Column thumbnail_url already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'tags_json'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN tags_json TEXT NULL',
    'SELECT "Column tags_json already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'source_links_json'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN source_links_json TEXT NULL',
    'SELECT "Column source_links_json already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'questions'
      AND COLUMN_NAME  = 'boost_enabled'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE questions ADD COLUMN boost_enabled BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT "Column boost_enabled already exists" AS message'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

