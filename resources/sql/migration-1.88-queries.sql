-- name: yesql-get-1_88-forms
SELECT *
FROM latest_forms
WHERE deleted IS NULL OR NOT deleted
ORDER BY created_time DESC;

-- name: yesql-insert-1_88-form<!
INSERT INTO forms (name, content, created_by, key, languages, organization_oid, deleted)
VALUES (:name, :content, :created_by, :key, :languages, :organization_oid, :deleted);