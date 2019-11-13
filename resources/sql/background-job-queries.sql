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

-- name: yesql-status
-- Uses a recursive CTE to emulate a loose index scan to quickly compute distinct job types
WITH RECURSIVE job_types AS ((SELECT job_type FROM jobs ORDER BY job_type LIMIT 1)
                             UNION ALL
                             (SELECT t.job_type
                              FROM job_types
                              JOIN LATERAL (SELECT job_type
                                            FROM jobs
                                            WHERE job_type > job_types.job_type
                                            ORDER BY job_type
                                            LIMIT 1) AS t ON true)),
     thresholds AS (SELECT ord, label, ('1 ' || label)::INTERVAL AS "interval"
                    FROM unnest(array['hour', 'day', 'week']) WITH ORDINALITY AS t(label, ord))
SELECT jsonb_object_agg(t.job_type, t.stats) AS status
FROM (SELECT t.job_type AS job_type,
             jsonb_build_object(
               'queued', max(t.queued),
               'late', max(t.late),
               'total', jsonb_object_agg(t.threshold, t.total),
               'failed', jsonb_object_agg(t.threshold, t.failed),
               'errored', jsonb_object_agg(t.threshold, t.errored)) AS stats
      FROM (SELECT job_types.job_type AS job_type,
                   thresholds.label AS threshold,
                   coalesce(not_executed.queued, 0) AS queued,
                   coalesce(not_executed.late, 0) AS late,
                   sum(coalesce(executed.total, 0)) OVER w AS total,
                   sum(coalesce(executed.failed, 0)) OVER w AS failed,
                   sum(coalesce(executed.errored, 0)) OVER w AS errored
            FROM job_types
            CROSS JOIN thresholds
            LEFT JOIN (SELECT (SELECT job_type FROM jobs WHERE jobs.id = t.job_id) AS job_type,
                              width_bucket(age(now(), t.execution_time), (SELECT array_agg("interval") FROM thresholds)) AS bucket,
                              count(*) AS total,
                              count(*) FILTER (WHERE transition = 'fail') AS failed,
                              count(*) FILTER (WHERE transition = 'error-retry') AS errored
                       FROM (SELECT job_id, max(execution_time) AS execution_time
                             FROM job_iterations
                             WHERE execution_time > now() - (SELECT max("interval") FROM thresholds)
                             GROUP BY job_id) AS t
                       JOIN LATERAL (SELECT transition
                                     FROM job_iterations
                                     WHERE job_iterations.job_id = t.job_id
                                     ORDER BY job_iterations.id DESC
                                     LIMIT 1) AS transition ON true
                       GROUP BY job_type, bucket) AS executed
              ON executed.job_type = job_types.job_type AND
                 executed.bucket = thresholds.ord
            LEFT JOIN (SELECT (SELECT job_type FROM jobs WHERE jobs.id = job_id) AS job_type,
                              count(*) FILTER (WHERE next_activation >= now()) AS queued,
                              count(*) FILTER (WHERE now() > next_activation) AS late
                       FROM job_iterations
                       WHERE NOT executed AND
                             NOT final
                       GROUP BY job_type) AS not_executed
              ON not_executed.job_type = job_types.job_type
            WINDOW w AS (PARTITION BY job_types.job_type ORDER BY thresholds.ord ASC)) AS t
      GROUP BY t.job_type) AS t;
