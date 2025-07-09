CREATE TABLE IF NOT EXISTS payment_reminders (
    id                 BIGSERIAL PRIMARY KEY,
    application_key    VARCHAR(256) NOT NULL,
    message            TEXT,
    lang               TEXT,
    send_reminder_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status             TEXT,
    handled_at         TIMESTAMP WITH TIME ZONE
);