create table if not exists siirtotiedostot (
    id varchar not null,
    window_start varchar,
    window_end varchar not null,
    run_start timestamp with time zone not null default now(),
    run_end timestamp with time zone,
    info jsonb,
    success boolean,
    error_message varchar,
    PRIMARY KEY (id)
);

COMMENT ON column siirtotiedostot.run_start IS 'Siirtotiedosto-operaation suorituksen alkuaika';
COMMENT ON column siirtotiedostot.run_end IS 'Siirtotiedosto-operaation suorituksen loppuaika';
COMMENT ON column siirtotiedostot.info IS 'Tietoja tallennetuista entiteeteistä, mm. lukumäärät';
COMMENT ON column siirtotiedostot.error_message IS 'null, jos mikään ei mennyt vikaan';