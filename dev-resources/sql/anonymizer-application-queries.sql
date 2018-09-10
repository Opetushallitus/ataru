-- name: sql-get-all-applications
SELECT id, key, preferred_name, last_name, ssn, person_oid, content FROM applications;

-- name: sql-update-application!
UPDATE applications
SET preferred_name = :preferred_name, last_name = :last_name, ssn = :ssn, content = :content
WHERE id = :id;
