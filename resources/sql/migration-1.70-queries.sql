-- name: yesql-update-application-content!
UPDATE applications SET content = :content WHERE id = :id;

-- name: yesql-get-final-person-integration-job-iteration-count
WITH application_ids AS (
    SELECT a.id::CHAR FROM applications a WHERE a.key = :application_key
)
SELECT COUNT(ji.*) AS count FROM jobs j
  INNER JOIN job_iterations ji ON j.id = ji.job_id
  WHERE j.job_type = 'ataru.person-service.person-integration'
    AND (ji.state ->> 'application-id' IN (SELECT id FROM application_ids)
      OR ji.state ->> 'application_id' IN (SELECT id FROM application_ids))
    AND ji.final IS TRUE;
