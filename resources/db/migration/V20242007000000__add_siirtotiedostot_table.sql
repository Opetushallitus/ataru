create table if not exists siirtotiedosto (
    id varchar not null,
    uuid varchar,
    window_start varchar,
    window_end varchar not null,
    run_start timestamp with time zone not null default now(),
    run_end timestamp with time zone,
    info jsonb,
    success boolean,
    error_message varchar,
    PRIMARY KEY (id)
);


CREATE SEQUENCE IF NOT EXISTS siirtotiedosto_id_seq START 1;

COMMENT ON column siirtotiedosto.run_start IS 'Siirtotiedosto-operaation suorituksen alkuaika';
COMMENT ON column siirtotiedosto.run_end IS 'Siirtotiedosto-operaation suorituksen loppuaika';
COMMENT ON column siirtotiedosto.info IS 'Tietoja tallennetuista entiteeteistä, mm. lukumäärät';
COMMENT ON column siirtotiedosto.error_message IS 'null, jos mikään ei mennyt vikaan';

--These initial values expect that data before the hardcoded first window_end will be handled manually through swagger or similar.
INSERT INTO siirtotiedosto(id, uuid, window_start, window_end, run_start, run_end, info, success, error_message)
VALUES (nextval('siirtotiedosto_id_seq'), '57be2612-ba79-429e-a93e-c38346f1d62d', '1970-01-01 00:00:00.000000 +00:00'::timestamptz, '2024-08-01 00:00:00.000000 +00:00'::timestamptz, now(), now(), '{"entityTotals": {}}'::jsonb, true, null) ON CONFLICT DO NOTHING;

