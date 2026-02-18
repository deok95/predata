-- 회원 추가 정보 컬럼 추가

-- 1. gender 컬럼 추가 (MALE, FEMALE, OTHER)
ALTER TABLE members
ADD COLUMN gender VARCHAR(10) NULL
AFTER age_group;

ALTER TABLE members
ADD CONSTRAINT chk_members_gender
CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));

-- 2. birth_date 컬럼 추가
ALTER TABLE members
ADD COLUMN birth_date DATE NULL
AFTER gender;

-- 3. 인덱스 추가 (통계 쿼리 최적화)
CREATE INDEX idx_members_gender ON members(gender);
CREATE INDEX idx_members_birth_date ON members(birth_date);
CREATE INDEX idx_members_age_group ON members(age_group);
CREATE INDEX idx_members_job_category ON members(job_category);
