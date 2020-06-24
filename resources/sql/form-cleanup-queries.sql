-- name: yesql-clean-up-old-forms!
DELETE FROM forms WHERE id IN (SELECT id FROM forms f
                               WHERE created_time < (NOW() - interval '1 month')
                                 AND f.id NOT IN (SELECT form_id FROM applications)
                                 AND f.id NOT IN (SELECT id FROM forms f2 WHERE f2.key = f.key ORDER BY f2.id DESC LIMIT 5)
                               LIMIT :limit);