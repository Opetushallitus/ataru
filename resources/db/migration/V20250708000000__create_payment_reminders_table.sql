CREATE TYPE payment_reminder_status AS ENUM ('sent', 'paid', 'overdue', 'invalidated');

CREATE TABLE IF NOT EXISTS payment_reminders (
    id                 BIGSERIAL PRIMARY KEY,
    application_key    VARCHAR(256) NOT NULL,
    application_id     BIGINT NOT NULL,
    order_id           VARCHAR(256) NOT NULL UNIQUE,
    message            TEXT DEFAULT '',
    lang               VARCHAR(2) NOT NULL,
    send_reminder_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status             payment_reminder_status,
    handled_at         TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE default now(),
);