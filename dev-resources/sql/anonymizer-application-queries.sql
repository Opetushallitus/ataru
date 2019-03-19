-- name: sql-get-all-applications
SELECT id, person_oid, content FROM applications;

-- name: sql-update-application!
UPDATE applications
SET preferred_name = :preferred_name,
    last_name = :last_name,
    ssn = :ssn,
    email = :email,
    dob = :dob,
    content = :content
WHERE id = :id;
