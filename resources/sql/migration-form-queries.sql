--name: yesql-migration-get-forms-query
-- Get stored forms, without content, filtered by what's allowed for the viewing user. Use the latest version.
SELECT
  id,
  key,
  name,
  deleted,
  created_by,
  created_time,
  languages
FROM latest_forms
WHERE (deleted IS NULL OR deleted = FALSE)
      AND (:query_type = 'ALL' OR organization_oid IN (:authorized_organization_oids))
ORDER BY created_time DESC;
