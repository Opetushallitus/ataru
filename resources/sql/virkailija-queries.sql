-- name: yesql-upsert-virkailija<!
INSERT INTO virkailija
VALUES (:oid, :first_name, :last_name, '{"review": {}}')
ON CONFLICT ON CONSTRAINT virkailija_pkey
DO UPDATE SET first_name = :first_name, last_name = :last_name;

-- name: yesql-update-virkailija-settings!
UPDATE virkailija SET settings = :settings WHERE oid = :oid;

-- name: yesql-get-virkailija-for-update
SELECT oid, first_name, last_name, settings FROM virkailija WHERE oid = :oid FOR UPDATE;

-- name: yesql-get-virkailija
SELECT oid, first_name, last_name, settings FROM virkailija WHERE oid = :oid;
