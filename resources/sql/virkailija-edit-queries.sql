-- name: yesql-upsert-virkailija-credentials!
INSERT INTO virkailija_credentials
VALUES (:secret, :username, :oid, :application_key, :first_name, :last_name);

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
