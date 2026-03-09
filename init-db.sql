-- PRE(D)ATA 모놀리식 데이터베이스 초기화
-- 사용법: mysql -u root -p < init-db.sql

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS predata
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 애플리케이션 전용 유저 생성 (prod 권장)
-- CREATE USER IF NOT EXISTS 'predata'@'localhost' IDENTIFIED BY '<STRONG_PASSWORD>';
-- GRANT ALL PRIVILEGES ON predata.* TO 'predata'@'localhost';
-- FLUSH PRIVILEGES;

-- 참고: 테이블 생성은 Flyway가 자동 처리합니다.
-- 이 파일은 DB/유저 생성만 담당합니다.
