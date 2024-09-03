CREATE TABLE IF NOT EXISTS siirtotiedosto (
    id varchar not null,
    execution_uuid varchar,
    window_start timestamp with time zone not null,
    window_end timestamp with time zone not null not null,
    run_start timestamp with time zone not null default now(),
    run_end timestamp with time zone,
    info jsonb,
    success boolean,
    error_message varchar,
    PRIMARY KEY (id)
);


CREATE SEQUENCE IF NOT EXISTS siirtotiedosto_id_seq START 1;

COMMENT ON COLUMN siirtotiedosto.execution_uuid IS 'Operaation tunniste (uuid)';
COMMENT ON COLUMN siirtotiedosto.window_start IS 'Siirtotiedosto-operaation aikaikkunan alkuaika (siirtotiedostoon tulevat tällä aikavälillä muuttuneet hakemukset ja lomakkeet)';
COMMENT ON COLUMN siirtotiedosto.window_end IS 'Siirtotiedosto-operaation aikaikkunan loppuaika (siirtotiedostoon tulevat tällä aikavälillä muuttuneet hakemukset ja lomakkeet)';
COMMENT ON COLUMN siirtotiedosto.run_start IS 'Siirtotiedosto-operaation suorituksen alkuaika';
COMMENT ON COLUMN siirtotiedosto.run_end IS 'Siirtotiedosto-operaation suorituksen loppuaika';
COMMENT ON COLUMN siirtotiedosto.info IS 'Tietoja tallennetuista entiteeteistä, mm. lukumäärät';
COMMENT ON COLUMN siirtotiedosto.error_message IS 'null, jos mikään ei mennyt vikaan';

--These initial values expect that data before the hardcoded first window_end will be handled manually through swagger or similar.
INSERT INTO siirtotiedosto(id, execution_uuid, window_start, window_end, run_start, run_end, info, success, error_message)
VALUES (nextval('siirtotiedosto_id_seq'), '57be2612-ba79-429e-a93e-c38346f1d62d', '1970-01-01 00:00:00.000000 +00:00', '2024-08-01 00:00:00.000000 +00:00', now(), null, '{}'::jsonb, true, null) ON CONFLICT DO NOTHING;

