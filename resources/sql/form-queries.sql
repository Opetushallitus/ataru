-- name: yesql-get-forms-query
-- Get stored forms, without content, filtered by what's allowed for the viewing user. Use the latest version.
select id, key, name, created_by, created_time, languages
from forms f
where f.created_time = (select max(created_time) from forms f2 where f2.key = f.key)
and   (f.organization_oid in (:authorized_organization_oids) or f.organization_oid is null)
and   (f.deleted is null or f.deleted = false)
order by created_time desc;

-- name: yesql-get-all-forms-query
-- Get all stored forms, without content. Use the latest version.
select id, key, name, created_by, created_time, languages
from forms f
where f.created_time = (select max(created_time) from forms f2 where f2.key = f.key)
and   (f.deleted is null or f.deleted = false)
order by created_time desc;

-- name: yesql-add-form<!
-- Add form
insert into forms (name, content, created_by, key, languages, organization_oid, deleted) values (:name, :content, :created_by, :key, :languages, :organization_oid, :deleted);

-- name: yesql-get-by-id
select id, key, name, content, created_by, created_time, languages, deleted from forms where id = :id;

-- name: yesql-fetch-latest-version-by-id
with the_key as (
  select key from forms where id = :id
), latest_version as (
  select max(created_time) as latest_time from forms f join the_key tk on f.key = tk.key
)
select id, key, name, content, created_by, created_time, languages, deleted from forms f join latest_version lv on f.created_time = lv.latest_time;

-- name: yesql-fetch-latest-version-by-key
with latest_version as (
  select max(created_time) as latest_time from forms f where f.key = :key
)
select id, key, name, content, created_by, created_time, languages, deleted from forms f join latest_version lv on f.created_time = lv.latest_time;

-- name: yesql-fetch-latest-version-by-id-lock-for-update
with the_key as (
  select key from forms where id = :id
), latest_version as (
  select max(created_time) as latest_time from forms f join the_key tk on f.key = tk.key
)
select id, key, name, content, created_by, created_time, organization_oid, languages, deleted from forms f join latest_version lv on f.created_time = lv.latest_time for update;

-- name: yesql-get-latest-version-organization-by-key
with latest_version as (
  select max(created_time) as latest_time from forms f where f.key = :key
)
select organization_oid from forms f join latest_version lv on f.created_time = lv.latest_time;

-- name: yesql-get-latest-version-organization-by-id
select organization_oid from forms f where id = :id;
