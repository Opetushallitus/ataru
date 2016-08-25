-- name: yesql-get-forms
-- Get all stored forms, without content, latest version
select id, name, created_by, created_time from forms f where f.created_time = (select max(created_time) from forms f2 where f2.key = f.key);

-- name: yesql-add-form<!
-- Add form
insert into forms (name, content, created_by, :key) values (:name, :content, :created_by. :key);

-- name: yesql-fetch-latest-version-by-key
select id, key, name, content, created_by, created_time from forms f where f.created_time = (select max(created_time) from forms f2 where f2.key = f.key)
and key = :key;


-- name: yesql-fetch-latest-version-by-id
select id, key, name, content, created_by, created_time from forms f where f.created_time = (select max(created_time) from forms f2 where f2.key = f.key and f2.id = :id);

-- name: yesql-get-by-id
select id, key, name, content, created_by, created_time from forms where id = :id;

-- name: yesql-fetch-latest-version-by-id-lock-for-update
select id, key, name, content, created_by, created_time from forms f where f.created_time = (select max(created_time) from forms f2 where f2.key = f.key and f2.id = :id) for update;
