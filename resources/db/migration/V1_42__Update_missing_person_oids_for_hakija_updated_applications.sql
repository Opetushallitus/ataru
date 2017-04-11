-- New application versions updated by applicants got null person_oids even when
-- the old versions already had a correct person_oid

DO $$DECLARE r record;
BEGIN
  FOR r IN SELECT distinct key, person_oid FROM applications where person_oid is not null
  LOOP
    raise notice 'Updating key: % person oid %', r.key, r.person_oid;
    update applications set person_oid = r.person_oid where person_oid is null and key = r.key;
  END LOOP;
END$$;
