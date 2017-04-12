--name: yesql-get-forms-query
-- Get stored forms, without content, filtered by what's allowed for the viewing user. Use the latest version.
WITH latest_forms AS (
    SELECT
      key,
      MAX(id) AS max_id
    FROM forms
    WHERE (:query_type = 'ALL' OR organization_oid IN (:authorized_organization_oids))
          AND (deleted IS NULL OR deleted = FALSE)
    GROUP BY key
)
SELECT
  f.id,
  f.key,
  f.name,
  f.created_by,
  f.created_time,
  f.languages
FROM forms f
  JOIN latest_forms lf ON f.id = lf.max_id
ORDER BY created_time DESC;

-- name: yesql-add-form<!
-- Add form
INSERT INTO forms (name, content, created_by, key, languages, organization_oid, deleted)
VALUES (:name, :content, :created_by, :key, :languages, :organization_oid, :deleted);

-- name: yesql-get-by-id
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  languages,
  deleted
FROM forms
WHERE id = :id;

-- name: yesql-fetch-latest-version-by-id
WITH the_key AS (
    SELECT key
    FROM forms
    WHERE id = :id
), latest_version AS (
    SELECT max(created_time) AS latest_time
    FROM forms f
      JOIN the_key tk ON f.key = tk.key
)
SELECT
  f.id,
  f.key,
  f.name,
  f.content,
  f.created_by,
  f.created_time,
  f.languages,
  f.deleted,
  f.organization_oid,
  count(a.id) AS application_count
FROM forms f
  JOIN latest_version lv ON f.created_time = lv.latest_time
  LEFT JOIN applications a ON (a.form_id IN (SELECT id
                                             FROM forms
                                             WHERE key = f.key) AND a.hakukohde IS NULL AND a.haku IS NULL)
GROUP BY f.id, f.key, f.name, f.content, f.created_by, f.created_time, f.languages, f.deleted, f.organization_oid;

-- name: yesql-fetch-latest-version-by-key
WITH latest_version AS (
    SELECT max(created_time) AS latest_time
    FROM forms f
    WHERE f.key = :key
)
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  languages,
  deleted,
  organization_oid
FROM forms f
  JOIN latest_version lv ON f.created_time = lv.latest_time;

-- name: yesql-fetch-latest-version-by-id-lock-for-update
WITH the_key AS (
    SELECT key
    FROM forms
    WHERE id = :id
), latest_version AS (
    SELECT max(created_time) AS latest_time
    FROM forms f
      JOIN the_key tk ON f.key = tk.key
)
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  organization_oid,
  languages,
  deleted
FROM forms f
  JOIN latest_version lv ON f.created_time = lv.latest_time
FOR UPDATE;

-- name: yesql-get-latest-version-organization-by-key
WITH latest_version AS (
    SELECT max(created_time) AS latest_time
    FROM forms f
    WHERE f.key = :key
)
SELECT organization_oid
FROM forms f
  JOIN latest_version lv ON f.created_time = lv.latest_time;

-- name: yesql-get-latest-version-organization-by-id
SELECT organization_oid
FROM forms f
WHERE id = :id;
