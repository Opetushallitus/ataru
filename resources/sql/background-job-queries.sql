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
WITH all_jobs AS (SELECT DISTINCT ON (job_id) *
                  FROM job_iterations
                  WHERE job_id IN (SELECT job_id
                                   FROM job_iterations
                                   WHERE execution_time > now() - (:period || ' HOUR')::INTERVAL)
                  ORDER BY job_id DESC, id DESC)
SELECT jobs_executed.job_type,
       jobs_executed.n     AS total,
       coalesce(fail.n, 0) AS fail,
       coalesce(0, 0)      AS error,
       coalesce(0, 0)      AS waiting
FROM (SELECT job_type, count(*) AS n
      FROM all_jobs AS total_jobs
               JOIN jobs AS j ON j.id = total_jobs.job_id
      GROUP BY job_type
      ORDER BY job_type) AS jobs_executed
         LEFT JOIN (SELECT job_type, count(*) AS n
                    FROM all_jobs AS aj
                             JOIN jobs AS j ON j.id = aj.job_id
                    WHERE transition = 'fail'
                    GROUP BY job_type) AS fail
                   ON fail.job_type = jobs_executed.job_type;



-- WITH queue AS (
--   SELECT j.job_type, ji.transition, count(*) AS n
--   FROM job_iterations AS ji
--   JOIN jobs AS j ON j.id = ji.job_id
--   WHERE NOT ji.executed AND
--         NOT ji.final AND
--         ji.next_activation < now()
--   GROUP BY j.job_type, ji.transition)
-- SELECT jt.job_type,
--        jt.n AS total,
--        coalesce(fail.n, 0) AS fail,
--        coalesce(error.n, 0) AS error,
--        coalesce(waiting.n, 0) AS waiting
-- FROM (SELECT job_type, count(*) AS n
--       FROM jobs
--       GROUP BY job_type) AS jt
-- LEFT JOIN (SELECT j.job_type, count(*) AS n
--            FROM job_iterations AS ji
--            JOIN jobs AS j ON j.id = ji.job_id
--            WHERE transition = 'fail'
--            GROUP BY j.job_type) AS fail
--   ON fail.job_type = jt.job_type
-- LEFT JOIN (SELECT job_type, n
--            FROM queue
--            WHERE transition = 'error-retry') AS error
--   ON error.job_type = jt.job_type
-- LEFT JOIN (SELECT job_type, n
--            FROM queue
--            WHERE transition = 'start') AS waiting
--   ON error.job_type = jt.job_type;
