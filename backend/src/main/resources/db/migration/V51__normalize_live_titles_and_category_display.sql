-- Normalize Korean sports suffix and LIVE category display text to English
UPDATE questions
SET title = REPLACE(title, '승자는?', 'Who will win?')
WHERE title LIKE '%승자는?%';

UPDATE market_categories
SET display_name = 'Live'
WHERE code = 'LIVE';

UPDATE market_categories
SET display_name = CASE code
    WHEN 'SPORTS' THEN 'Sports'
    WHEN 'POLITICS' THEN 'Politics'
    WHEN 'ECONOMY' THEN 'Economy'
    WHEN 'TECH' THEN 'Tech'
    WHEN 'ENTERTAINMENT' THEN 'Entertainment'
    WHEN 'CULTURE' THEN 'Culture'
    WHEN 'INTERNATIONAL' THEN 'International'
    WHEN 'GENERAL' THEN 'General'
    WHEN 'TRENDING' THEN 'Trending (auto-generated only)'
    ELSE display_name
END
WHERE code IN ('SPORTS','POLITICS','ECONOMY','TECH','ENTERTAINMENT','CULTURE','INTERNATIONAL','GENERAL','TRENDING');
