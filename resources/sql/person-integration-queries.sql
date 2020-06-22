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
             (SELECT value
              FROM answers
              WHERE key = 'preferred-name' AND
                    application_id = a.id) AS preferred_name,
             (SELECT value
              FROM answers
              WHERE key = 'last-name' AND
                    application_id = a.id) AS last_name,
             (SELECT upper(value)
              FROM answers
              WHERE key = 'ssn' AND
                    application_id = a.id) AS ssn,
             (SELECT to_date(value, 'DD.MM.YYYY')
              FROM answers
              WHERE key = 'birth-date' AND
                    application_id = a.id) AS dob
      FROM applications AS a
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
