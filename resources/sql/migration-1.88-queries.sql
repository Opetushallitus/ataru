-- name: yesql-get-1_88-forms
SELECT *
FROM forms
ORDER BY created_time ASC;

-- name: yesql-update-1_88-form<!
UPDATE forms SET content = :content
WHERE id = :id;