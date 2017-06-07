-- Dig dob out of answer (if it exists) and update it to a dedicated, indexed column
DO $$DECLARE r RECORD;
BEGIN
  FOR r IN SELECT id, answers ->> 'value' as email
    FROM
     (SELECT id, key, jsonb_array_elements(content -> 'answers') AS answers FROM applications) AS app_answers
    WHERE answers @> '{"key":"email"}'
  LOOP
    RAISE NOTICE 'Updating id: % with email %', r.id, r.email;
    UPDATE applications SET email = r.email WHERE id = r.id;
  END LOOP;
END$$;
