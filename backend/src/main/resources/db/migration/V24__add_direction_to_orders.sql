-- Add direction column to orders table for BUY/SELL order model
-- BUY: 매수 주문 (USDC 예치)
-- SELL: 매도 주문 (포지션 담보)

ALTER TABLE orders ADD COLUMN direction VARCHAR(4) NOT NULL DEFAULT 'BUY';

-- Create index for efficient filtering by direction
CREATE INDEX idx_orders_direction ON orders(direction);

-- All existing orders are BUY orders (기존 데이터는 모두 BUY)
-- DEFAULT 'BUY' handles this automatically
