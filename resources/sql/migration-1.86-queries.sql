-- name: yesql-get-1_86-forms
SELECT *
FROM latest_forms
WHERE deleted IS NULL OR NOT deleted
ORDER BY created_time DESC;

-- name: yesql-insert-1_86-form<!
INSERT INTO forms (name, content, created_by, key, languages, organization_oid, deleted)
VALUES (:name, :content, :created_by, :key, :languages, :organization_oid, :deleted);

-- name: yesql-get-1_86-applications
SELECT a.*
FROM latest_applications AS a
JOIN forms AS f ON f.id = a.form_id
WHERE f.key = :form_key;

-- name: yesql-insert-1_86-application!
INSERT INTO applications
(key, lang, form_id, content, preferred_name, last_name, person_oid, hakukohde, haku, ssn, dob, email)
VALUES (:key, :lang, :form_id, :content, :preferred_name, :last_name, :person_oid, ARRAY[:hakukohde]::character varying(127)[], :haku, :ssn, :dob, :email);

-- name: yesql-insert-1_86-application-event<!
INSERT INTO application_events (application_key, event_type, new_review_state, virkailija_oid, hakukohde, review_key)
VALUES (:application_key, :event_type, :new_review_state, :virkailija_oid, :hakukohde, :review_key);
