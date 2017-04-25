-- Dig ssn out of answer (if it exists) and update it to a dedicated, indexed column
DO $$DECLARE r RECORD;
BEGIN
  FOR r IN SELECT id, answers ->> 'value' as ssn
    FROM
     (SELECT id, key, jsonb_array_elements(content -> 'answers') AS answers FROM applications) AS app_answers
    WHERE answers @> '{"key":"ssn"}'
  LOOP
    RAISE NOTICE 'Updating id: % with ssn %', r.id, r.ssn;
    UPDATE applications SET ssn = r.ssn WHERE id = r.id;
  END LOOP;
END$$;
