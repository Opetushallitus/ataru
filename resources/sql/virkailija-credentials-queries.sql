-- name: yesql-upsert-virkailija-credentials<!
INSERT INTO virkailija_credentials (oid, application_key, secret) VALUES (:oid, :application_key, :secret);

-- name: yesql-invalidate-virkailija-credentials!
UPDATE virkailija_credentials
SET valid = false
WHERE secret = :virkailija_secret;

-- name: yesql-get-virkailija-secret-valid
SELECT valid
FROM virkailija_credentials
WHERE secret = :virkailija_secret
AND created_time > now() - INTERVAL '1 hour';

-- name: yesql-get-virkailija-oid
SELECT oid
FROM virkailija_credentials
WHERE secret = :virkailija_secret AND application_key = :application_key;
