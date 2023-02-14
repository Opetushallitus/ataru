-- name: yesql-upsert-harkinnanvaraisuus-process!
INSERT INTO harkinnanvaraisuus_process (application_id, application_key, haku_oid)
VALUES (:application_id, :application_key, :haku_oid)
ON CONFLICT (application_key)
    DO UPDATE SET harkinnanvarainen_only = NULL, last_checked = NULL, skip_check = false, application_id = :application_id;

-- name: yesql-fetch-harkinnanvaraisuus-unprocessed
SELECT
    application_id,
    application_key,
    haku_oid
FROM harkinnanvaraisuus_process
WHERE last_checked is NULL AND skip_check = false
ORDER BY application_id ASC LIMIT 250;

-- name: yesql-skip-checking-harkinnanvaraisuus-processes!
UPDATE harkinnanvaraisuus_process
SET skip_check = true WHERE application_id in (:ids);

-- name: yesql-update-harkinnanvaraisuus-process!
UPDATE harkinnanvaraisuus_process
SET last_checked = :last_checked, harkinnanvarainen_only = :harkinnanvarainen_only
WHERE application_id = :application_id;

-- name: yesql-fetch-checked-harkinnanvaraisuus-processes
SELECT
    application_id,
    application_key,
    haku_oid,
    harkinnanvarainen_only
FROM harkinnanvaraisuus_process
WHERE last_checked IS NOT NULL AND last_checked < :before_this AND skip_check = false AND harkinnanvarainen_only IS NOT NULL
ORDER BY application_id ASC LIMIT 1000;