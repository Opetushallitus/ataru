-- name: yesql-upsert-virkailija-credentials!
INSERT INTO virkailija_credentials
VALUES (:secret, :username, :oid, :application_key, :first_name, :last_name)
ON CONFLICT ON CONSTRAINT virkailija_credentials_pkey
  DO UPDATE SET secret = :secret;
