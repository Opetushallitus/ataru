CREATE TABLE IF NOT EXISTS application_koski_tutkinnot (
    application_key  varchar(40),
    tutkinnot        jsonb,
    PRIMARY KEY (application_key)
);

COMMENT ON TABLE application_koski_tutkinnot IS 'Koskesta haetut tutkinnot hakemuksittain, tallennetaan tarvittaessa';
