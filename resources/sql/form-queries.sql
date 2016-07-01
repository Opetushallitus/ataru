-- name: yesql-get-forms-query
-- Get all stored forms, without content
select id, name, modified_by, modified_time from forms order by modified_time desc;

-- name: yesql-add-form-query<!
-- Add form
insert into forms (name, content, modified_by) values (:name, :content, :modified_by);

-- name: yesql-form-exists-query
-- Get single form
select id from forms where id = :id;

-- name: yesql-get-by-id
select * from forms where id = :id;

-- name: yesql-update-form-query!
-- Update form
update forms set
  name = :name,
  modified_time = now(),
  modified_by = :modified_by,
  content = cast(:content as jsonb)
  where id = :id;
