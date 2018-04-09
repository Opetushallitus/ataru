--name: yesql-get-forms-1_90-query
SELECT id
FROM latest_forms
ORDER BY created_time DESC;

-- name: yesql-get-by-id-1_90-query
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  languages,
  deleted,
  organization_oid
FROM forms
WHERE id = :id;


