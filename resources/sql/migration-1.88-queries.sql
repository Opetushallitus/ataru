-- name: yesql-get-1_88-form
SELECT *
FROM forms
WHERE id = :id;

-- name: yesql-get-1_88-form-ids
SELECT id
FROM forms
ORDER BY created_time ASC;

-- name: yesql-update-1_88-form<!
UPDATE forms SET content = :content
WHERE id = :id;
