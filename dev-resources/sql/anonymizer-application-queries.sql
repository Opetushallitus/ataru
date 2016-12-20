-- name: sql-get-all-applications
SELECT id, key, preferred_name, last_name, content FROM applications;

-- name: sql-update-application!
UPDATE applications
SET preferred_name = :preferred_name, last_name = :last_name, content = :content
WHERE id = :id;
