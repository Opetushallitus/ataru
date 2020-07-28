--This is meant to be run only once manually, at installation. This is not needed once the relevant values have
--been added to jobs and job_iterations.

BEGIN;

DO $$
    DECLARE activation timestamptz;
    BEGIN

        IF (SELECT count(*) > 0
            FROM jobs
            WHERE job_type = 'clean-old-forms-job') THEN
            RAISE EXCEPTION 'clean-old-forms-job already created';
        END IF;

        SELECT (CURRENT_DATE + TIME '20:30' AT TIME ZONE 'UTC') INTO activation;
        IF (activation < now()) THEN
            SELECT (activation + INTERVAL '1 day') INTO activation;
        END IF;
        RAISE NOTICE 'next activation %', activation;

        INSERT INTO jobs (job_type)
        VALUES ('clean-old-forms-job');
        INSERT INTO job_iterations (job_id,
                                    step,
                                    transition,
                                    state,
                                    next_activation)
        VALUES ((SELECT id FROM jobs WHERE job_type = 'clean-old-forms-job'),
                'initial',
                'start',
                jsonb_build_object('last-run-long', extract(epoch from activation - interval '1 day') * 1000),
                activation);

    END $$;

END;
