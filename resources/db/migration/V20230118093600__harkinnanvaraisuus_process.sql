CREATE TABLE harkinnanvaraisuus_process (
    application_id bigint NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    application_key text NOT NULL UNIQUE,
    haku_oid text NOT NULL,
    harkinnanvaraisuus_status text,
    skip_check boolean DEFAULT false,
    last_checked timestamp with time zone,
    PRIMARY KEY (application_id, application_key)
);

COMMENT ON TABLE harkinnanvaraisuus_process IS 'Hakemuksien harkinnanvaraisuuden tarkistuksen tilataulu';
COMMENT ON COLUMN harkinnanvaraisuus_process.application_key IS 'Hakemuksen oid';
COMMENT ON COLUMN harkinnanvaraisuus_process.haku_oid IS 'Haun oid johon hakemus liittyy';
COMMENT ON COLUMN harkinnanvaraisuus_process.harkinnanvaraisuus_status IS 'Hakemuksen harkinnanvaraisuuden viimeisin tila';
COMMENT ON COLUMN harkinnanvaraisuus_process.skip_check IS 'Voiko harkinnanvaraisuuden tarkistuksen jättää tekemättä';
COMMENT ON COLUMN harkinnanvaraisuus_process.last_checked IS 'Milloin viimeksi hakemuksen harkinnanvaraisuus on tarkastettu';