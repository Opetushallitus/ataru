-- name: yesql-get-session-query
-- Get session data
SELECT data
FROM sessions
WHERE key = :key;

-- name: yesql-add-session-query!
-- Add session
INSERT INTO sessions (key, data) VALUES (:key, :data);

-- name: yesql-update-session-query!
-- Update session
UPDATE sessions
SET data = :data
WHERE key = :key;

-- name: yesql-delete-session-query!
-- Deletes session
DELETE FROM sessions
WHERE key = :key;
