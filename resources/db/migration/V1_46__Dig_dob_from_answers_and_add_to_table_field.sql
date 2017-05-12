-- Dig dob out of answer (if it exists) and update it to a dedicated, indexed column
DO $$DECLARE r RECORD;
BEGIN
  FOR r IN SELECT id, answers ->> 'value' as dob
    FROM
     (SELECT id, key, jsonb_array_elements(content -> 'answers') AS answers FROM applications) AS app_answers
    WHERE answers @> '{"key":"birth-date"}'
  LOOP
    RAISE NOTICE 'Updating id: % with dob %', r.id, r.dob;
    UPDATE applications SET dob = to_date(r.dob, 'DD.MM.YYYY') WHERE id = r.id;
  END LOOP;
END$$;
