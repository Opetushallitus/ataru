-- name: yesql-get-koodisto
SELECT *
FROM koodisto_cache
WHERE koodisto_uri = :koodisto_uri AND version = :version
ORDER BY created_at DESC
LIMIT 1;

-- name: yesql-create-koodisto<!
INSERT INTO koodisto_cache (koodisto_uri, version, checksum, content)
VALUES (:koodisto_uri, :version, :checksum, :content);
