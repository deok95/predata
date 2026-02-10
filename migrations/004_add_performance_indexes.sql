-- Migration 004: Performance Indexes
-- 목적: 쿼리 성능 최적화를 위한 인덱스 추가
-- 날짜: 2026-02-10

-- 1. Activity 테이블 인덱스
-- 질문별 활동 조회 최적화 (activity_type 포함)
CREATE INDEX IF NOT EXISTS idx_activity_question_type
ON activities(question_id, activity_type);

-- IP 기반 어뷰징 탐지 최적화
CREATE INDEX IF NOT EXISTS idx_activity_ip
ON activities(ip_address);

-- 멤버별 질문 활동 조회 최적화 (중복 베팅/투표 체크)
CREATE INDEX IF NOT EXISTS idx_activity_member_question
ON activities(member_id, question_id);

-- 활동 타입별 조회 최적화
CREATE INDEX IF NOT EXISTS idx_activity_type
ON activities(activity_type);

-- 베팅 판매 추적 최적화 (parent_bet_id가 있는 경우)
CREATE INDEX IF NOT EXISTS idx_activity_parent_bet
ON activities(parent_bet_id);

-- 2. Member 테이블 인덱스
-- IP 기반 중복 계정 탐지 최적화
CREATE INDEX IF NOT EXISTS idx_member_signup_ip
ON members(signup_ip);

CREATE INDEX IF NOT EXISTS idx_member_last_ip
ON members(last_ip);

-- 지갑 주소 기반 조회 최적화
CREATE INDEX IF NOT EXISTS idx_member_wallet
ON members(wallet_address);

-- Tier별 조회 최적화 (리더보드 등)
CREATE INDEX IF NOT EXISTS idx_member_tier
ON members(tier);

-- 정확도 순 정렬 최적화
CREATE INDEX IF NOT EXISTS idx_member_accuracy
ON members(accuracy_score DESC);

-- 3. Question 테이블 인덱스
-- 상태별 질문 조회 최적화
CREATE INDEX IF NOT EXISTS idx_question_status
ON questions(status);

-- 카테고리별 질문 조회 최적화
CREATE INDEX IF NOT EXISTS idx_question_category
ON questions(category);

-- 만료 시간 기반 조회 최적화 (스케줄러)
CREATE INDEX IF NOT EXISTS idx_question_expired_at
ON questions(expired_at);

-- 베팅 시작/종료 시간 조회 최적화
CREATE INDEX IF NOT EXISTS idx_question_betting_times
ON questions(betting_start_at, betting_end_at);

-- 4. EmailVerification 테이블 인덱스
-- 이메일 기반 최신 인증 코드 조회 최적화
CREATE INDEX IF NOT EXISTS idx_email_verification_email_created
ON email_verifications(email, created_at DESC);

-- 만료 시간 기반 정리 최적화
CREATE INDEX IF NOT EXISTS idx_email_verification_expires
ON email_verifications(expires_at);

-- 5. 복합 인덱스 (특정 쿼리 패턴 최적화)
-- 베팅 가능한 질문 조회 (status + expired_at)
CREATE INDEX IF NOT EXISTS idx_question_betting_available
ON questions(status, expired_at);

-- 투표 가능한 질문 조회 (status + voting_end_at)
CREATE INDEX IF NOT EXISTS idx_question_voting_available
ON questions(status, voting_end_at);

-- 정산 완료 질문 조회
CREATE INDEX IF NOT EXISTS idx_question_settled
ON questions(status, final_result);

-- 인덱스 생성 완료 확인
-- 아래 쿼리로 생성된 인덱스 확인 가능:
-- SELECT * FROM information_schema.statistics
-- WHERE table_schema = 'predata'
-- ORDER BY table_name, index_name;
