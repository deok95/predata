-- Add performance indexes for Data Center APIs
-- Migration: V2__add_performance_indexes.sql
-- Date: 2026-02-11
-- Purpose: Optimize slow queries in PremiumDataService, PersonaWeightService, AbusingDetectionService

-- 1. 활동 조회 최적화 (question_id + activity_type 복합 인덱스)
-- 효과: findByQuestionIdAndActivityType() 쿼리 성능 향상
CREATE INDEX IF NOT EXISTS idx_activities_question_type
ON activities(question_id, activity_type);

-- 2. 회원 국가별 조회 최적화
-- 효과: findIdsByCountryCode(), countByCountryCode() 쿼리 성능 향상
CREATE INDEX IF NOT EXISTS idx_members_country
ON members(country_code);

-- 3. 회원 직업별 조회 최적화
-- 효과: findIdsByJobCategory(), countByJobCategory() 쿼리 성능 향상
CREATE INDEX IF NOT EXISTS idx_members_job
ON members(job_category);

-- 4. 회원 연령대별 조회 최적화
-- 효과: findIdsByAgeGroupBetween(), countByAgeGroupBetween() 쿼리 성능 향상
CREATE INDEX IF NOT EXISTS idx_members_age
ON members(age_group);
