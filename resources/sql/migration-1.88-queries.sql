-- name: yesql-get-1_88-forms
SELECT *
FROM forms
ORDER BY created_time ASC;

-- name: yesql-insert-1_88-form<!
INSERT INTO forms (name, content, created_by, key, languages, organization_oid, deleted)
VALUES (:name, :content, :created_by, :key, :languages, :organization_oid, :deleted);