CREATE TABLE kk_application_payment_states
(
    id            bigserial PRIMARY KEY,
    person_oid    text NOT NULL,
    start_term    text NOT NULL,
    start_year    text NOT NULL,
    state         text NOT NULL,
    created_time  timestamp with time zone DEFAULT now(),
    modified_time timestamp with time zone DEFAULT now(),
    UNIQUE(person_oid, start_term, start_year)
);

COMMENT ON TABLE kk_application_payment_states IS 'Korkeakoulujen hakemusmaksujen tila aloituslukukausittain';

CREATE TABLE kk_application_payment_events
(
    id                               bigserial PRIMARY KEY,
    kk_application_payment_state_id  bigint REFERENCES kk_application_payment_states (id),
    new_state                        text NOT NULL,
    event_type                       text NOT NULL,
    virkailija_oid                   text REFERENCES virkailija(oid),
    message                          text,
    created_time                     timestamp with time zone DEFAULT now()
);

COMMENT ON TABLE kk_application_payment_events IS 'Korkeakoulujen hakemusmaksujen tilamuutoshistoria';
