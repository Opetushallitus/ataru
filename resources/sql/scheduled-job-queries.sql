-- name: yesql-add-scheduled-job-instance!
INSERT INTO scheduled_job_instances (job_type, scheduled_at) VALUES (:job_type, :scheduled_at::timestamptz);

-- name: yesql-add-job-type!
INSERT INTO job_types (job_type, enabled) VALUES (:job_type, :enabled) ON CONFLICT DO NOTHING;

-- name: yesql-get-job-types
SELECT job_type, enabled FROM job_types;

-- name: yesql-update-job-type!
UPDATE job_types SET enabled=:enabled WHERE job_type=:job_type;

-- name: yesql-get-queue-lengths
SELECT job_type, (SELECT count(1) FROM proletarian_jobs WHERE queue=job_types.job_type) AS length FROM job_types;
