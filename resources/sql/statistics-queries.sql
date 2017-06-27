-- name: yesql-get-application-stats
SELECT
  a.form_id,
  a.created_time,
  f.key,
  (SELECT name
   FROM forms f2
   WHERE f2.key = f.key
   ORDER BY id DESC
   LIMIT 1) as form_name
FROM applications a
  JOIN forms f ON f.id = a.form_id
WHERE a.created_time > :start_time
ORDER BY a.created_time;