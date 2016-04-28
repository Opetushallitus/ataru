-- name: get-forms-query
-- Get all stored forms
select id, name, modified_time from forms order by modified_time desc;

-- name: add-form-query<!
-- Add form
insert into forms (name) values (:name);

-- name: form-exists-query
-- Get single form
select id from forms where id = :id;

-- name: get-by-id
select * from forms where id = :id;

-- name: update-form-query!
-- Update form
update forms set
  name = :name, modified_time = now()
  where id = :id;
