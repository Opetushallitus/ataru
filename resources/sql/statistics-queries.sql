-- name: yesql-get-application-stats
SELECT
  form_id,
  created_time
FROM applications
WHERE created_time > :start_time
ORDER BY created_time;