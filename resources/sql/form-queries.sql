-- name: get-forms-query
-- Get all stored forms
select id, name from forms

-- name: add-form-query!
-- Add form
insert into forms (id, name) values (:id, :name)

-- name: form-exists-query
-- Get single form
select id from forms where id = :id

-- name: update-form-query!
-- Update form
update forms set name = :name where id = :id
