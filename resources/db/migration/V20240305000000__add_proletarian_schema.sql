CREATE TABLE IF NOT EXISTS proletarian_jobs (
    job_id      UUID PRIMARY KEY,   -- job id, generated and returned by proletarian.job/enqueue!
    queue       TEXT      NOT NULL, -- queue name
    job_type    TEXT      NOT NULL, -- job type
    payload     TEXT      NOT NULL, -- Transit-encoded job data
    attempts    INTEGER   NOT NULL, -- Number of attempts. Starts at 0. Increments when the job is processed.
    enqueued_at TIMESTAMP NOT NULL, -- When the job was enqueued (never changes)
    process_at  TIMESTAMP NOT NULL  -- When the job should be run (updates every retry)
);

CREATE TABLE IF NOT EXISTS proletarian_archived_jobs (
    job_id      UUID PRIMARY KEY,   -- Copied from job record.
    queue       TEXT      NOT NULL, -- Copied from job record.
    job_type    TEXT      NOT NULL, -- Copied from job record.
    payload     TEXT      NOT NULL, -- Copied from job record.
    attempts    INTEGER   NOT NULL, -- Copied from job record.
    enqueued_at TIMESTAMP NOT NULL, -- Copied from job record.
    process_at  TIMESTAMP NOT NULL, -- Copied from job record (data for the last run only)
    status      TEXT      NOT NULL, -- success / failure
    finished_at TIMESTAMP NOT NULL  -- When the job was finished (success or failure)
);

DROP INDEX IF EXISTS proletarian_job_queue_process_at;

CREATE INDEX proletarian_job_queue_process_at ON proletarian_jobs (queue, process_at);

--- Taulu jonka avulla varmistetaan että toistuvat jobit skeduloidaan klusterissa vain kerran
CREATE TABLE IF NOT EXISTS scheduled_job_instances (
    job_type        TEXT        NOT NULL,
    scheduled_at    TIMESTAMP   NOT NULL,
    PRIMARY KEY (scheduled_at, job_type)
);

--- taulu jonka avulla jobien ajo voidaan disabloida
CREATE TABLE IF NOT EXISTS job_types (
    job_type        TEXT        PRIMARY KEY,
    enabled         BOOLEAN     NOT NULL
);