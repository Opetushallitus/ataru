-- name: yesql-get-status-poll-applications
SELECT a.key, COALESCE(ahr.state, 'unprocessed') FROM applications a
LEFT OUTER JOIN application_hakukohde_reviews ahr ON (a.key = ahr.application_key)
WHERE a.form_id IN (SELECT id FROM forms WHERE key = :form_key)
AND (ahr.state IS NULL OR ahr.state IN ('unprocessed', 'decision-fee-outstanding'));
