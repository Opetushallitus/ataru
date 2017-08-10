-- name: yesql-get-virkailija-credentials
SELECT *
FROM virkailija_credentials
WHERE application_key = :application_key;

-- name: yesql-upsert-virkailija-credentials!
INSERT INTO virkailija_credentials
VALUES (:secret, :username, :oid, :application_key)
ON CONFLICT ON CONSTRAINT virkailija_credentials_pkey
  DO UPDATE SET secret = :secret;

