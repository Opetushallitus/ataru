--name: yesql-get-forms
-- Get stored forms, without content. Use the latest version.
SELECT
  f.id,
  f.key,
  f.name,
  f.organization_oid,
  f.deleted,
  f.created_by,
  f.created_time,
  f.languages,
  f.locked,
  f.properties
FROM (SELECT DISTINCT key FROM forms) AS k
JOIN LATERAL (SELECT *
              FROM forms
              WHERE key = k.key
              ORDER BY id DESC
              LIMIT 1) AS f ON true
WHERE (f.deleted IS NULL OR NOT f.deleted)
  AND :hakukohderyhma_oid::varchar IS NULL
  OR f.used_hakukohderyhmas @> ARRAY[ :hakukohderyhma_oid::varchar ]
ORDER BY f.created_time DESC;

-- name: yesql-add-form<!
-- Add form
INSERT INTO forms (name,
                   content,
                   created_by,
                   key,
                   languages,
                   organization_oid,
                   deleted,
                   locked,
                   locked_by,
                   used_hakukohderyhmas,
                   properties)
VALUES (:name,
        :content,
        :created_by,
        :key,
        :languages,
        :organization_oid,
        :deleted,
        :locked::timestamp,
        :locked_by,
        ARRAY[ :used_hakukohderyhmas ]::varchar[],
        :properties);

-- name: yesql-get-by-id
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  languages,
  deleted,
  organization_oid,
  properties
FROM forms
WHERE id = :id;

-- name: yesql-get-forms-by-ids
SELECT
    id,
    key,
    name,
    content,
    created_by,
    created_time::text,
    languages,
    deleted,
    organization_oid,
    properties
FROM forms
WHERE id in (:ids);

-- name: yesql-get-siirtotiedosto-form-ids
SELECT
    id
FROM forms f
WHERE
    f.created_time >= :window_start::timestamptz
  AND
    f.created_time <= :window_end::timestamptz
ORDER BY f.id;

-- name: yesql-fetch-latest-version-by-id
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
  f.locked,
  f.locked_by as locked_by_oid,
  f.properties,
  (CASE WHEN f.locked_by IS NULL THEN NULL ELSE CONCAT(first_name, ' ', last_name) END) as locked_by,
  (SELECT count(*)
   FROM applications AS a
   LEFT JOIN applications AS newer_a ON a.key = newer_a.key AND newer_a.id > a.id
   WHERE newer_a.id IS NULL
     AND a.haku IS NULL
     AND a.form_id IN (SELECT id FROM forms WHERE key = f.key)) AS application_count
FROM latest_forms f
LEFT JOIN virkailija ON f.locked_by = virkailija.oid
WHERE f.key = (SELECT key FROM forms WHERE id = :id);

-- name: yesql-form-by-key-has-applications
SELECT exists(
    SELECT 1
    FROM latest_applications la
        JOIN forms fo
            on fo.id = la.form_id where fo.key = :key);

-- name: yesql-fetch-latest-version-by-key
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  languages,
  deleted,
  organization_oid,
  locked,
  properties,
  (CASE WHEN locked_by IS NULL THEN NULL ELSE CONCAT(first_name, ' ', last_name) END) as locked_by
FROM latest_forms
LEFT JOIN virkailija ON locked_by = virkailija.oid
WHERE key = :key;

-- name: yesql-fetch-latest-version-by-key-for-kk-payment-module-job
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  languages,
  deleted,
  organization_oid,
  locked,
  properties,
  locked_by
FROM latest_forms
WHERE key = :key;

-- name: yesql-fetch-latest-version-by-id-lock-for-update
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  organization_oid,
  languages,
  deleted,
  properties
FROM forms
WHERE id = (SELECT max(id)
            FROM forms
            WHERE key = (SELECT key FROM forms WHERE id = :id))
FOR UPDATE;

-- name: yesql-latest-id-by-key
SELECT max(id) AS id
FROM forms
WHERE key = :key;

-- name: yesql-get-latest-version-organization-by-key
SELECT organization_oid
FROM latest_forms
WHERE key = :key;

-- name: yesql-get-latest-version-organization-by-id
SELECT organization_oid
FROM forms f
WHERE id = :id;

-- name: yesql-get-latest-form-by-name
WITH latest_forms AS (
    SELECT
      key,
      MAX(id) AS max_id
    FROM forms
    GROUP BY key
)
SELECT
  f.key
FROM forms f
  JOIN latest_forms lf ON f.id = lf.max_id
WHERE (f.deleted IS NULL OR f.deleted = FALSE)
      AND f.name->>'fi' = :form_name
ORDER BY created_time DESC
LIMIT 1;
