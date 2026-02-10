-- Migration: 004_create_orders_table.sql
-- Description: 오더북 시스템을 위한 orders, trades 테이블 생성
-- Date: 2026-02-07

USE predata;

-- 주문(Order) 테이블
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,

    -- 주문 타입: BUY(매수), SELL(매도)
    order_type ENUM('BUY', 'SELL') NOT NULL,

    -- 포지션: YES 또는 NO
    side ENUM('YES', 'NO') NOT NULL,

    -- 가격 (0.01 ~ 0.99, 소수점 2자리)
    price DECIMAL(4, 2) NOT NULL,

    -- 수량 (포인트 단위)
    amount BIGINT NOT NULL,

    -- 남은 수량 (부분 체결 시)
    remaining_amount BIGINT NOT NULL,

    -- 주문 상태: OPEN(활성), FILLED(완료), PARTIAL(부분체결), CANCELLED(취소)
    status ENUM('OPEN', 'FILLED', 'PARTIAL', 'CANCELLED') NOT NULL DEFAULT 'OPEN',

    -- 타임스탬프
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 인덱스
    INDEX idx_orders_question_status (question_id, status),
    INDEX idx_orders_member (member_id),
    INDEX idx_orders_question_side_price (question_id, side, price),

    -- 외래 키
    FOREIGN KEY (member_id) REFERENCES members(member_id),
    FOREIGN KEY (question_id) REFERENCES questions(question_id)
);

-- 체결(Trade) 테이블
CREATE TABLE IF NOT EXISTS trades (
    trade_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,

    -- 매칭된 주문 정보
    buy_order_id BIGINT NOT NULL,
    sell_order_id BIGINT NOT NULL,

    -- 체결 정보
    price DECIMAL(4, 2) NOT NULL,
    amount BIGINT NOT NULL,
    side ENUM('YES', 'NO') NOT NULL,

    -- 타임스탬프
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 인덱스
    INDEX idx_trades_question (question_id),
    INDEX idx_trades_buy_order (buy_order_id),
    INDEX idx_trades_sell_order (sell_order_id),
    INDEX idx_trades_executed_at (executed_at DESC),

    -- 외래 키
    FOREIGN KEY (question_id) REFERENCES questions(question_id),
    FOREIGN KEY (buy_order_id) REFERENCES orders(order_id),
    FOREIGN KEY (sell_order_id) REFERENCES orders(order_id)
);
