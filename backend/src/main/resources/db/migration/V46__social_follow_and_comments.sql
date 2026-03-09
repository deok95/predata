-- V46: Social features (profile fields, follows, comments)

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS username VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS bio VARCHAR(300) NULL,
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500) NULL;

-- Existing users: fallback display name from email prefix if empty
UPDATE members
SET display_name = SUBSTRING_INDEX(email, '@', 1)
WHERE display_name IS NULL OR display_name = '';

CREATE UNIQUE INDEX uk_members_username ON members (username);

CREATE TABLE IF NOT EXISTS follows (
    follow_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES members(member_id),
    CONSTRAINT fk_follows_following FOREIGN KEY (following_id) REFERENCES members(member_id),
    CONSTRAINT uk_follows_pair UNIQUE (follower_id, following_id),
    CONSTRAINT chk_follows_not_self CHECK (follower_id <> following_id),
    INDEX idx_follows_follower_created (follower_id, created_at),
    INDEX idx_follows_following_created (following_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question_comments (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    content TEXT NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_question_comments_question FOREIGN KEY (question_id) REFERENCES questions(question_id),
    CONSTRAINT fk_question_comments_member FOREIGN KEY (member_id) REFERENCES members(member_id),
    CONSTRAINT fk_question_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES question_comments(comment_id),
    INDEX idx_qcomments_question_created (question_id, created_at),
    INDEX idx_qcomments_member_created (member_id, created_at),
    INDEX idx_qcomments_parent (parent_comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
