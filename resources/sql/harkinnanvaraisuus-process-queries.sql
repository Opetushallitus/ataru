-- name: yesql-upsert-harkinnanvaraisuus-process!
INSERT INTO harkinnanvaraisuus_process (application_id, application_key, haku_oid)
VALUES (:application_id, :application_key, :haku_oid)
ON CONFLICT (application_key)
    DO UPDATE SET harkinnanvaraisuus_status = NULL, last_checked = NULL, skip_check = false, application_id = :application_id;

-- name: yesql-fetch-harkinnanvaraisuus-unprocessed
SELECT
    application_id,
    application_key,
    haku_oid
FROM harkinnanvaraisuus_process
WHERE last_checked is NULL ORDER BY application_id ASC LIMIT 1000;

-- name: yesql-skip-checking-harkinnanvaraisuus-processes!
UPDATE harkinnanvaraisuus_process
SET skip_check = true WHERE application_id in (:ids);