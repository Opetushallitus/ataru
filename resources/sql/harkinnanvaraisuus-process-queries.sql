-- name: yesql-upsert-harkinnanvaraisuus-process!
INSERT INTO harkinnanvaraisuus_process (application_id, application_key, haku_oid)
VALUES (:application_id, :application_key, :haku_oid)
ON CONFLICT (application_key)
    DO UPDATE SET harkinnanvaraisuus_status = NULL, last_checked = NULL, skip_check = false, application_id = :application_id;