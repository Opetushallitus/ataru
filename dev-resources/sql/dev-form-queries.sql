-- name: yesql-set-form-id!
-- Add form
update forms set id = :new_id where id = :old_id
