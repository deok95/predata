DELIMITER $$

CREATE PROCEDURE GenerateDummyData()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE random_country CHAR(2);
    DECLARE random_job VARCHAR(50);
    DECLARE random_choice ENUM('YES', 'NO');
    
    -- 1. 10,000명의 유저 생성 (다양한 페르소나)
    WHILE i <= 10000 DO
        SET random_country = ELT(FLOOR(1 + (RAND() * 5)), 'KR', 'US', 'JP', 'SG', 'VN');
        SET random_job = ELT(FLOOR(1 + (RAND() * 5)), 'IT', 'Finance', 'Student', 'Medical', 'Service');
        
        INSERT INTO members (email, country_code, job_category, age_group, tier_weight, point_balance)
        VALUES (CONCAT('user', i, '@predata.world'), random_country, random_job, 
                FLOOR(20 + (RAND() * 30)), -- 20~50대
                1.0 + (RAND() * 0.5), -- 숙련도 가중치 1.0~1.5
                FLOOR(10000 + (RAND() * 90000))); -- 초기 포인트
        SET i = i + 1;
    END WHILE;

    -- 2. 테스트용 질문 생성 (예: 글로벌 금리 인하 여부)
    INSERT INTO questions (title, category, category_weight, status, expired_at)
    VALUES ('Will the Fed cut interest rates in Q1 2026?', 'ECONOMY', 2.0, 'OPEN', NOW() + INTERVAL 7 DAY);

    -- 3. 1만 명의 활동 데이터 (지표 괴리 연출)
    -- 투표는 70%가 YES라고 하지만, 베팅은 30%만 YES에 거는 상황 시뮬레이션
    SET i = 1;
    WHILE i <= 10000 DO
        -- 70% 확률로 투표는 YES
        IF RAND() < 0.7 THEN SET random_choice = 'YES'; ELSE SET random_choice = 'NO'; END IF;
        
        INSERT INTO activities (member_id, question_id, activity_type, choice, latency_ms)
        VALUES (i, 1, 'VOTE', random_choice, FLOOR(5000 + (RAND() * 10000)));

        -- 베팅은 반대로 30%만 YES (돈은 정직하게 거는 상황)
        IF RAND() < 0.3 THEN SET random_choice = 'YES'; ELSE SET random_choice = 'NO'; END IF;
        
        INSERT INTO activities (member_id, question_id, activity_type, choice, amount)
        VALUES (i, 1, 'BET', random_choice, FLOOR(100 + (RAND() * 1000)));
        
        SET i = i + 1;
    END WHILE;

    -- 4. 질문 테이블의 풀(Pool) 합계 업데이트
    UPDATE questions q 
    SET yes_bet_pool = (SELECT SUM(amount) FROM activities WHERE question_id = q.question_id AND activity_type = 'BET' AND choice = 'YES'),
        no_bet_pool = (SELECT SUM(amount) FROM activities WHERE question_id = q.question_id AND activity_type = 'BET' AND choice = 'NO'),
        total_bet_pool = (SELECT SUM(amount) FROM activities WHERE question_id = q.question_id AND activity_type = 'BET');

END$$

DELIMITER ;

-- 프로시저 실행
CALL GenerateDummyData();
