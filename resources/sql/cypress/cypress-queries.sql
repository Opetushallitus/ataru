-- name: yesql-remove-form!
DELETE FROM forms WHERE key = :form_key;
