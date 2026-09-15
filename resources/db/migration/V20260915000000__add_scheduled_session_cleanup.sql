CREATE EXTENSION IF NOT EXISTS pg_cron;

SELECT cron.schedule('session-cleanup', '0 0 */2 * *', $$DELETE FROM sessions WHERE created_at < now() - interval '2 days'$$);