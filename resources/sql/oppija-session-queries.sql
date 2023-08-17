-- name: yesql-read-oppija-session-query
-- Get session data
SELECT data
FROM oppija_sessions
WHERE key = :key;

-- name: yesql-add-oppija-session-query!
-- Add session
INSERT INTO oppija_sessions (key, ticket, data) VALUES (:key, :ticket, :data);

-- name: yesql-update-oppija-session-query!
-- Update session
UPDATE oppija_sessions
SET data = :data
WHERE key = :key;

-- name: yesql-delete-oppija-session-by-ticket-query!
-- Deletes session
DELETE FROM oppija_sessions
WHERE ticket = :ticket;
