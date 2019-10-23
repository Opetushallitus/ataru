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
  j.stop,
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

--name: yesql-status
WITH all_jobs AS (SELECT DISTINCT ON (job_id) job_id, transition, executed, final, next_activation
                  FROM job_iterations
                  WHERE job_id IN (SELECT job_id
                                   FROM job_iterations
                                   WHERE execution_time > now() - (:period || ' HOUR')::INTERVAL)
                  ORDER BY job_id DESC, id DESC)
SELECT total_jobs.job_type,
       total_jobs.n           AS total,
       coalesce(fail.n, 0)    AS fail,
       coalesce(error.n, 0)   AS error,
       coalesce(waiting.n, 0) AS waiting
FROM (SELECT job_type, count(*) AS n
      FROM all_jobs
               JOIN jobs AS j ON j.id = all_jobs.job_id
      GROUP BY job_type
      ORDER BY job_type) AS total_jobs
         LEFT JOIN (SELECT job_type, count(*) AS n
                    FROM all_jobs AS aj
                             JOIN jobs AS j ON j.id = aj.job_id
                    WHERE transition = 'fail'
                    GROUP BY job_type) AS fail
                   ON fail.job_type = total_jobs.job_type
         LEFT JOIN (SELECT job_type, count(*) AS n
                    FROM all_jobs AS aj
                             JOIN jobs AS j ON j.id = aj.job_id
                    WHERE transition = 'error-retry'
                      AND NOT executed
                      AND NOT final
                      AND next_activation > now()
                    GROUP BY job_type) AS error
                   ON error.job_type = total_jobs.job_type
         LEFT JOIN (SELECT job_type, count(*) AS n
                    FROM all_jobs AS aj
                             JOIN jobs AS j ON j.id = aj.job_id
                    WHERE transition = 'start'
                      AND NOT executed
                      AND NOT final
                      AND next_activation > now()
                    GROUP BY job_type) AS waiting
                   ON error.job_type = total_jobs.job_type;