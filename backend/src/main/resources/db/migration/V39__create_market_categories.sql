-- V39: market_categories 테이블 생성 및 초기 데이터
-- 목적: 카테고리 단일 소스 (Top3 선별 기준)
-- 멱등성: IF NOT EXISTS + INSERT IGNORE

CREATE TABLE IF NOT EXISTS market_categories (
    code         VARCHAR(50)  NOT NULL COMMENT '카테고리 코드 (PK)',
    display_name VARCHAR(100) NOT NULL COMMENT '표시 이름',
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '활성 여부',
    sort_order   INT          NOT NULL DEFAULT 0    COMMENT '정렬 순서',
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기본 카테고리 시드 데이터 (INSERT IGNORE → 재실행 안전)
INSERT IGNORE INTO market_categories (code, display_name, is_active, sort_order) VALUES
    ('SPORTS',         '스포츠',        TRUE,  1),
    ('POLITICS',       '정치',          TRUE,  2),
    ('ECONOMY',        '경제',          TRUE,  3),
    ('TECH',           '기술',          TRUE,  4),
    ('ENTERTAINMENT',  '엔터테인먼트',  TRUE,  5),
    ('CULTURE',        '문화',          TRUE,  6),
    ('INTERNATIONAL',  '국제',          TRUE,  7),
    ('GENERAL',        '일반',          TRUE, 99);
