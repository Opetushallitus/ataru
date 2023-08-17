CREATE TABLE oppija_sessions (
    key varchar not null,
    ticket varchar not null,
    data jsonb not null default '{}'::jsonb,
    created_at timestamp with time zone default now(),
    PRIMARY KEY (key)
)