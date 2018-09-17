BEGIN;

DO $$
DECLARE activation timestamptz;
BEGIN

IF (SELECT count(*) > 0
    FROM jobs
    WHERE job_type = 'start-automatic-eligibility-if-ylioppilas-job-job') THEN
  RAISE EXCEPTION 'start-automatic-eligibility-if-ylioppilas-job-job already running';
END IF;

SELECT (CURRENT_DATE + TIME '20:30' AT TIME ZONE 'UTC') INTO activation;
IF (activation < now()) THEN
  SELECT (activation + INTERVAL '1 day') INTO activation;
END IF;
RAISE NOTICE 'next activation %', activation;

INSERT INTO jobs (job_type)
VALUES ('start-automatic-eligibility-if-ylioppilas-job-job');
INSERT INTO job_iterations (job_id,
                            step,
                            transition,
                            state,
                            next_activation)
VALUES ((SELECT id FROM jobs WHERE job_type = 'start-automatic-eligibility-if-ylioppilas-job-job'),
        'initial',
        'start',
        jsonb_build_object('last-run-long', extract(epoch from activation - interval '1 day') * 1000),
        activation);

END $$;

END;
