-- name: yesql-upsert-email-template!<
INSERT INTO email_templates (form_key, virkailija_oid, lang, content, haku_oid)
VALUES (:form_key, :virkailija_oid, :lang, :content, :haku_oid)
ON CONFLICT ON CONSTRAINT email_templates_form_key_haku_oid_lang_key
  DO UPDATE SET content = :content, virkailija_oid = :virkailija_oid
RETURNING *;

-- name: yesql-get-email-templates
SELECT *
FROM email_templates
WHERE form_key = :form_key AND haku_oid = :haku_oid;
