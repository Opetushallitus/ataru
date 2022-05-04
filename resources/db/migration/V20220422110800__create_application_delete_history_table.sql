CREATE TABLE application_delete_history
(
    id                BIGSERIAL PRIMARY KEY,
    application_key   VARCHAR(256) NOT NULL,
    deleted_at        TIMESTAMP WITH TIME ZONE DEFAULT now(),
    deleted_by        varchar(256) NOT NULL,
    delete_ordered_by varchar(256) NOT NULL,
    reason_of_delete  varchar(1024) NOT NULL
);

COMMENT ON TABLE application_delete_history IS 'Logi poistetuista hakemuksista';
COMMENT ON COLUMN application_delete_history.application_key IS 'Hakemuksen oid';
COMMENT ON COLUMN application_delete_history.deleted_at IS 'Poistohetki';
COMMENT ON COLUMN application_delete_history.deleted_by IS 'Poistanut';
COMMENT ON COLUMN application_delete_history.delete_ordered_by IS 'Poistoa pyytänyt henkilö';
COMMENT ON COLUMN application_delete_history.reason_of_delete IS 'Poiston syy';

