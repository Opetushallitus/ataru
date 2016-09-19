-- name: yesql-get-forms
-- Get all stored forms, without content, latest version
select id, key, name, created_by, created_time from forms f where f.created_time = (select max(created_time) from forms f2 where f2.key = f.key)
order by created_time desc;

-- name: yesql-add-form<!
-- Add form
insert into forms (name, content, created_by, key) values (:name, :content, :created_by, :key);

-- name: yesql-get-by-id
select id, key, name, content, created_by, created_time from forms where id = :id;

-- name: yesql-fetch-latest-version-by-id
with the_key as (
  select key from forms where id = :id
), latest_version as (
  select max(created_time) as latest_time from forms f join the_key tk on f.key = tk.key
)
select id, key, name, content, created_by, created_time from forms f join latest_version lv on f.created_time = lv.latest_time;

-- name: yesql-fetch-latest-version-by-key
with latest_version as (
  select max(created_time) as latest_time from forms f where f.key = :key
)
select id, key, name, content, created_by, created_time from forms f join latest_version lv on f.created_time = lv.latest_time;

-- name: yesql-fetch-latest-version-by-id-lock-for-update
with the_key as (
  select key from forms where id = :id
), latest_version as (
  select max(created_time) as latest_time from forms f join the_key tk on f.key = tk.key
)
select id, key, name, content, created_by, created_time from forms f join latest_version lv on f.created_time = lv.latest_time for update;
