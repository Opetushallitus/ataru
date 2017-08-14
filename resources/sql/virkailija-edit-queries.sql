-- name: yesql-upsert-virkailija-credentials!
INSERT INTO virkailija_credentials
VALUES (:secret, :username, :oid, :application_key, :first_name, :last_name)
ON CONFLICT ON CONSTRAINT virkailija_credentials_pkey
  DO UPDATE SET secret = :secret, valid = true;

-- name: yesql-invalidate-virkailija-credentials!
UPDATE virkailija_credentials
SET valid = false
WHERE secret = :virkailija_secret;

-- name: yesql-get-virkailija-secret-valid
SELECT valid
FROM virkailija_credentials
WHERE secret = :virkailija_secret;