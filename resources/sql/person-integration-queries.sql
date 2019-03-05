-- name: yesql-update-person-info-as-in-person!
UPDATE applications
SET preferred_name = t.preferred_name,
    last_name = t.last_name,
    ssn = t.ssn,
    dob = t.dob
FROM (SELECT :preferred_name AS preferred_name,
             :last_name AS last_name,
             upper(:ssn) AS ssn,
             to_date(:dob, 'YYYY-MM-DD') AS dob) AS t
WHERE id IN (SELECT a.id
             FROM applications AS a
             WHERE a.person_oid = :person_oid AND
                   NOT EXISTS (SELECT 1
                               FROM applications AS a2
                               WHERE a2.key = a.key AND
                                     a2.id > a.id))
  AND (applications.preferred_name IS DISTINCT FROM t.preferred_name
       OR applications.last_name IS DISTINCT FROM t.last_name
       OR applications.ssn IS DISTINCT FROM t.ssn
       OR applications.dob IS DISTINCT FROM t.dob)

-- name: yesql-update-person-info-as-in-application!
UPDATE applications
SET preferred_name = t.preferred_name,
    last_name = t.last_name,
    ssn = t.ssn,
    dob = t.dob
FROM (SELECT a.id,
             (array_agg(answers->>'value')
              FILTER (WHERE answers->>'key' = 'preferred-name'))[1] AS preferred_name,
             (array_agg(answers->>'value')
              FILTER (WHERE answers->>'key' = 'last-name'))[1] AS last_name,
             upper((array_agg(answers->>'value')
                    FILTER (WHERE answers->>'key' = 'ssn'))[1]) AS ssn,
             to_date((array_agg(answers->>'value')
                      FILTER (WHERE answers->>'key' = 'birth-date'))[1],
                     'DD.MM.YYYY') AS dob
      FROM applications AS a
      JOIN LATERAL jsonb_array_elements(a.content->'answers') AS answers ON true
      WHERE a.person_oid = :person_oid AND
            NOT EXISTS (SELECT 1
                        FROM applications AS a2
                        WHERE a2.key = a.key AND
                              a2.id > a.id)
      GROUP BY id) AS t
WHERE applications.id = t.id
  AND (applications.preferred_name IS DISTINCT FROM t.preferred_name
       OR applications.last_name IS DISTINCT FROM t.last_name
       OR applications.ssn IS DISTINCT FROM t.ssn
       OR applications.dob IS DISTINCT FROM t.dob)
