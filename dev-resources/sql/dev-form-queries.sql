-- name: yesql-set-form-id!
-- Add form
update forms set id = :new_id where id = :old_id

-- name: yesql-delete-all-forms!
-- Delete all forms
delete from forms;
