CREATE TABLE field_deadlines (
       application_key text,
       field_id        text,
       deadline        timestamptz NOT NULL,
       modified        timestamptz NOT NULL DEFAULT now(),
       CONSTRAINT field_deadline PRIMARY KEY(application_key, field_id)
);

COMMENT ON TABLE field_deadlines IS 'Hakemuskohtainen yksittäisen kysymyksen muokkauksen takaraja';
COMMENT ON COLUMN field_deadlines.application_key IS 'Hakemuksen OID';
COMMENT ON COLUMN field_deadlines.field_id IS 'Kysymyksen tunniste';
COMMENT ON COLUMN field_deadlines.deadline IS 'Muokkauksen takaraja';
COMMENT ON COLUMN field_deadlines.modified IS 'Rivin luonti- tai muokkaushetki';
