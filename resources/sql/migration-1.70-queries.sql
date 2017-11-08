-- name: yesql-update-application-content!
UPDATE applications SET content = :content WHERE id = :id;

-- name: yesql-get-final-person-integration-job-iteration-count
SELECT COUNT(ji.*) AS count FROM jobs j
  INNER JOIN job_iterations ji ON j.id = ji.job_id
  WHERE j.job_type = 'ataru.person-service.person-integration'
        AND (ji.state ->> 'application-id' IN (:application_ids) OR ji.state ->> 'application_id' IN (:application_ids))
        AND ji.final IS TRUE;
