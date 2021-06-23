ALTER TABLE forms ADD COLUMN used_hakukohderyhmas varchar[];

COMMENT ON COLUMN forms.used_hakukohderyhmas IS 'Lomakkeeseen liittyvät hakukohderyhmät';
