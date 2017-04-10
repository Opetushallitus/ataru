-- name: yesql-add-background-job<!
INSERT INTO jobs (job_type) VALUES (:job_type);

-- name: yesql-add-job-iteration<!
INSERT INTO job_iterations
(job_id, step, state, next_activation, transition, retry_count, final, caused_by_error)
VALUES
  (:job_id, :step, :state, :next_activation, :transition, :retry_count, :final, :caused_by_error);

-- name: yesql-select-job-for-execution
-- Selects job and locks it for execution. The locking doesn't force other nodes to
-- block since we use "skip locked", other nodes just won't see the current job transaction
-- while it's running
SELECT
  j.id  AS job_id,
  j.job_type,
  ji.id AS iteration_id,
  ji.step,
  ji.state,
  ji.retry_count
FROM jobs j
  JOIN job_iterations ji ON j.id = ji.job_id
WHERE ji.executed = FALSE
      AND j.job_type IN (:job_types)
      AND ji.next_activation < now()
      AND ji.final = FALSE
ORDER BY ji.next_activation ASC
LIMIT 1 -- Limits us to only one job at a time
FOR UPDATE SKIP LOCKED;

-- name: yesql-update-previous-iteration!
UPDATE job_iterations
SET executed = TRUE, execution_time = now()
WHERE id = :id;
