CREATE INDEX IF NOT EXISTS job_iterations_execution_time_idx ON job_iterations (execution_time);
CREATE INDEX IF NOT EXISTS job_iterations_job_id_id_idx ON job_iterations (job_id, id DESC);
CREATE INDEX IF NOT EXISTS jobs_job_type_idx ON jobs (job_type);
