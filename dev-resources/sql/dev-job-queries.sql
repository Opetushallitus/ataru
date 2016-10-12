-- name: yesql-get-final-iteration-for-job
select step, state, next_activation, transition, retry_count, final, error from job_iterations
where job_id = :job_id
and final = true;

