-- name: yesql-add-scheduled-job-instance!
INSERT INTO scheduled_job_instances (job_type, scheduled_at) VALUES (:job_type, :scheduled_at::timestamptz);

-- name: yesql-add-job-type!
INSERT INTO job_types (job_type, enabled) VALUES (:job_type, :enabled) ON CONFLICT DO NOTHING;

-- name: yesql-get-job-types
SELECT job_type, enabled FROM job_types;

-- name: yesql-update-job-type!
UPDATE job_types SET enabled=:enabled WHERE job_type=:job_type;