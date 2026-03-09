INSERT INTO market_categories (code, display_name, is_active, sort_order)
VALUES ('LIVE', 'Live', 1, 0)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  is_active = VALUES(is_active),
  sort_order = VALUES(sort_order);
