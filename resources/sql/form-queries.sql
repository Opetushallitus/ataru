-- name: yesql-get-forms-query
-- Get stored forms, without content, filtered by what's allowed for the viewing user. Use the latest version.
select id, key, name, created_by, created_time, languages
from forms f
where f.created_time = (select max(created_time) from forms f2 where f2.key = f.key)
and   f.organization_oid in (:authorized_organization_oids)
and   (f.deleted is null or f.deleted = false)
order by created_time desc;

-- name: yesql-get-forms-with-deleteds-in-use-query
-- Get stored forms, without content, filtered by what's allowed for the viewing user. Use the latest version. Includes deleted forms that have been used in application.
SELECT
  f.id,
  f.key,
  f.name,
  f.created_by,
  f.created_time,
  f.languages,
  f.deleted
FROM forms f
WHERE f.key IN (SELECT f2.key
                FROM forms f2
                  LEFT JOIN applications a ON f2.id = a.form_id
                GROUP BY f2.key
                HAVING (count(a.id) > 0) OR every(f2.deleted IS NOT TRUE))
      AND f.organization_oid IN (:authorized_organization_oids)
      AND f.created_time = (SELECT max(created_time)
                            FROM forms f3
                            WHERE f.key = f3.key);

-- name: yesql-get-all-forms-query
-- Get all stored forms, without content. Use the latest version.
select id, key, name, created_by, created_time, languages
from forms f
where f.created_time = (select max(created_time) from forms f2 where f2.key = f.key)
and   (f.deleted is null or f.deleted = false)
order by created_time desc;

-- name: yesql-get-all-forms-with-deleteds-in-use-query
-- Get all stored forms, without content. Use the latest version. Includes deleted forms that have been used in application.
SELECT
  f.id,
  f.key,
  f.name,
  f.created_by,
  f.created_time,
  f.languages,
  f.deleted
FROM forms f
WHERE f.key IN (SELECT f2.key
                FROM forms f2
                  LEFT JOIN applications a ON f2.id = a.form_id
                GROUP BY f2.key
                HAVING (count(a.id) > 0) OR every(f2.deleted IS NOT TRUE))
      AND f.created_time = (SELECT max(created_time)
                            FROM forms f3
                            WHERE f.key = f3.key);

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
select
  f.id,
  f.key,
  f.name,
  f.content,
  f.created_by,
  f.created_time,
  f.languages,
  f.deleted,
  f.organization_oid,
  count(a.id) as application_count
from forms f
join latest_version lv on f.created_time = lv.latest_time
left join applications a on (a.form_id in (select id from forms where key = f.key) and a.hakukohde is null and a.haku is null)
group by f.id, f.key, f.name, f.content, f.created_by, f.created_time, f.languages, f.deleted, f.organization_oid;

-- name: yesql-fetch-latest-version-by-key
with latest_version as (
  select max(created_time) as latest_time from forms f where f.key = :key
)
select
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  languages,
  deleted,
  organization_oid
from forms f
join latest_version lv on f.created_time = lv.latest_time;

-- name: yesql-fetch-latest-version-by-id-lock-for-update
with the_key as (
  select key from forms where id = :id
), latest_version as (
  select max(created_time) as latest_time from forms f join the_key tk on f.key = tk.key
)
select
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  organization_oid,
  languages,
  deleted
from forms f
join latest_version lv on f.created_time = lv.latest_time for update;

-- name: yesql-get-latest-version-organization-by-key
with latest_version as (
  select max(created_time) as latest_time from forms f where f.key = :key
)
select organization_oid from forms f join latest_version lv on f.created_time = lv.latest_time;

-- name: yesql-get-latest-version-organization-by-id
select organization_oid from forms f where id = :id;
