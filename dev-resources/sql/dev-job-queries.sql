-- name: yesql-get-final-iteration-for-job
SELECT
  step,
  state,
  next_activation,
  transition,
  retry_count,
  final,
  caused_by_error
FROM job_iterations
WHERE job_id = :job_id
      AND final = TRUE;
