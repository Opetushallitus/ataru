--name: yesql-get-forms-1_90-query
SELECT
  id,
  key,
  name,
  deleted,
  created_by,
  created_time,
  languages
FROM forms
WHERE (deleted IS NULL OR deleted = FALSE)
ORDER BY created_time DESC;


