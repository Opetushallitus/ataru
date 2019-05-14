-- name: sql-get-all-applications
SELECT id FROM applications;

-- name: sql-get-application
SELECT id, person_oid, content
FROM applications
WHERE id = :id;

-- name: sql-update-application!
UPDATE applications
SET preferred_name = :preferred_name,
    last_name = :last_name,
    ssn = :ssn,
    email = :email,
    dob = :dob::DATE,
    content = :content
WHERE id = :id;

-- name: sql-application-secret-ids
SELECT id FROM application_secrets;
