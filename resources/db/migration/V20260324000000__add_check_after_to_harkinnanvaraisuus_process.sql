ALTER TABLE harkinnanvaraisuus_process
    ADD COLUMN IF NOT EXISTS check_after TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

ALTER TABLE harkinnanvaraisuus_process
    ALTER COLUMN check_after DROP DEFAULT;

COMMENT ON COLUMN harkinnanvaraisuus_process.check_after IS 'Milloin harkinnanvaraisuus voidaan tarkistaa. Tähän voi lisätä viivettä, että muut järjestelmät ovat päivittyneet.';
