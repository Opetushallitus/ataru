-- NOTE: the migration datestamp is reverse (YYYYDDMM instead of YYYYMMDD) and slightly misdated on purpose:
-- some of the previous migrations from 2024 were accidentally added that way, and we need to run this after those
-- to use the last modified procedure.

-- The original migration was removed before going forward to QA and production, but drop the tables just in case
-- if they still exist and aren't cleaned up manually.

DROP TABLE IF EXISTS kk_application_payment_events;
DROP TABLE IF EXISTS kk_application_payment_states;

-- Store payment info related to individual applications.
-- Use history table with automatic update triggers.

CREATE TABLE IF NOT EXISTS kk_application_payments
(
    id                   serial PRIMARY KEY,
    application_key      varchar(40) UNIQUE,
    state                text NOT NULL,
    reason               text,
    due_date             date,
    total_sum            text,
    maksut_secret        text,
    required_at          timestamp with time zone,
    reminder_sent_at     timestamp with time zone,
    approved_at          timestamp with time zone,
    created_at           timestamp with time zone DEFAULT now(),
    modified_at          timestamp with time zone DEFAULT now()
);

COMMENT ON TABLE kk_application_payments IS 'Korkeakouluhakujen hakemusmaksujen tila hakemuksittain';

-- Automatic modification timestamps for the main table
CREATE OR REPLACE FUNCTION update_payment_modified_at() RETURNS trigger AS $$
BEGIN
  NEW.modified_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER sync_lastmod
BEFORE UPDATE ON kk_application_payments
FOR EACH ROW EXECUTE PROCEDURE update_payment_modified_at();

-- Also update application modified at in sync whenever payment data changes
CREATE OR REPLACE TRIGGER set_application_modified_time_on_kk_application_payment_update
    AFTER INSERT OR UPDATE OR DELETE
    ON kk_application_payments
    FOR EACH ROW
EXECUTE PROCEDURE update_application_modified_time();

-- Automatic audit history for payment changes.
CREATE TABLE IF NOT EXISTS kk_application_payments_history
(
    id                   serial PRIMARY KEY,
    application_key      varchar(40),
    state                text NOT NULL,
    reason               text,
    due_date             date,
    total_sum            text,
    maksut_secret        text,
    required_at          timestamp with time zone,
    reminder_sent_at     timestamp with time zone,
    approved_at          timestamp with time zone,
    created_at           timestamp with time zone DEFAULT now(),
    modified_at          timestamp with time zone DEFAULT now()
);

COMMENT ON TABLE kk_application_payments IS 'Korkeakouluhakujen hakemusmaksujen tilahistoria hakemuksittain';

CREATE OR REPLACE FUNCTION kk_application_payments_history_trigger() RETURNS TRIGGER AS
$$
begin
    insert into kk_application_payments_history (
        application_key,
        state,
        reason,
        due_date,
        total_sum,
        maksut_secret,
        required_at,
        reminder_sent_at,
        approved_at,
        created_at,
        modified_at
    ) values (
        old.application_key,
        old.state,
        old.reason,
        old.due_date,
        old.total_sum,
        old.maksut_secret,
        old.required_at,
        old.reminder_sent_at,
        old.approved_at,
        old.created_at,
        old.modified_at
    );
    return null;
end;
$$ language plpgsql;

CREATE OR REPLACE TRIGGER update_kk_application_payments
AFTER UPDATE ON kk_application_payments
FOR EACH ROW
EXECUTE PROCEDURE kk_application_payments_history_trigger();

CREATE OR REPLACE TRIGGER delete_kk_application_payments
AFTER DELETE ON kk_application_payments
FOR EACH ROW
EXECUTE PROCEDURE kk_application_payments_history_trigger();

