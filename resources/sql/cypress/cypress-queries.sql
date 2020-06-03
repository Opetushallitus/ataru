-- name: yesql-remove-applications-of-form!
delete from applications where form_id in
  (select id from forms where key = :form_key);

-- name: yesql-remove-form!
DELETE FROM forms WHERE key = :form_key;
