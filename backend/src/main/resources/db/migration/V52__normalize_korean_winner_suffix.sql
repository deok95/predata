-- Ensure legacy/generated sports question titles are fully English.
UPDATE questions
SET title = REPLACE(title, ' - 승자는?', ' - Who will win?')
WHERE title LIKE '% - 승자는?%';

UPDATE questions
SET title = REPLACE(title, '승자는?', 'Who will win?')
WHERE title LIKE '%승자는?%';

UPDATE questions
SET title = REPLACE(title, '승자는 ?', 'Who will win?')
WHERE title LIKE '%승자는 ?%';
