-- name: yesql-clean-up-old-sessions!
DELETE FROM sessions WHERE created_at < (NOW() - interval '2 days');
