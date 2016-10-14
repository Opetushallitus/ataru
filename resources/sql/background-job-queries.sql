-- name: yesql-add-background-job<!
insert into jobs (job_type) values (:job_type);

-- name: yesql-add-job-iteration<!
insert into job_iterations
(job_id, step, state, next_activation, transition, retry_count, final, caused_by_error)
values
(:job_id, :step, :state, :next_activation, :transition, :retry_count, :final, :caused_by_error);

-- name: yesql-select-job-for-execution
-- Selects job and locks it for execution. The locking doesn't force other nodes to
-- block since we use "skip locked", other nodes just won't see the current job transaction
-- while it's running
select j.id as job_id, j.job_type,
ji.id as iteration_id, ji.step, ji.state, ji.retry_count
from jobs j
join job_iterations ji on j.id = ji.job_id
where ji.executed = FALSE
and job_type in (:job_types)
and next_activation < now()
and final = FALSE
order by next_activation asc
limit 1 -- Limits us to only one job at a time
for update skip locked;

-- name: yesql-update-previous-iteration!
update job_iterations set executed = TRUE, execution_time = now() where id = :id;
