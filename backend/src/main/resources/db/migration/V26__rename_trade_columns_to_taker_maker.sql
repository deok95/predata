-- V26: Rename buy_order_id/sell_order_id to taker_order_id/maker_order_id
-- Taker = 주문을 넣어서 체결을 일으킨 쪽
-- Maker = 오더북에 대기 중이던 쪽

ALTER TABLE trades RENAME COLUMN buy_order_id TO taker_order_id;
ALTER TABLE trades RENAME COLUMN sell_order_id TO maker_order_id;
