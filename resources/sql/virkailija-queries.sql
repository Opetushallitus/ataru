-- name: yesql-upsert-virkailija<!
INSERT INTO virkailija
VALUES (:oid, :first_name, :last_name, '{"review": {}}')
ON CONFLICT ON CONSTRAINT virkailija_pkey
DO UPDATE SET first_name = :first_name, last_name = :last_name;

-- name: yesql-update-virkailija-settings!
UPDATE virkailija SET settings = :settings WHERE oid = :oid;
