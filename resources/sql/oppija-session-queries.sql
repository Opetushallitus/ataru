-- name: yesql-read-oppija-session-query
-- Get session data
SELECT data,
       (created_at + interval '3 hours' - interval '5 minutes' <= now()
            and created_at + interval '3 hours' > now()) as expires_soon
FROM oppija_sessions
WHERE key = :key
and created_at + interval '3 hours'>= now();

-- name: yesql-add-oppija-session-query!
-- Add session
INSERT INTO oppija_sessions (key, ticket, data) VALUES (:key, :ticket, :data);

-- name: yesql-update-oppija-session-query!
-- Update session
UPDATE oppija_sessions
SET data = :data
WHERE key = :key;

-- name: yesql-delete-oppija-session-by-ticket-query!
-- Deletes session by ticket
DELETE FROM oppija_sessions
WHERE ticket = :ticket;

-- name: yesql-delete-oppija-session-query!
-- Deletes session
DELETE FROM oppija_sessions
WHERE key = :key;
