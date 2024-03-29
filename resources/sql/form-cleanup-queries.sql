-- name: yesql-clean-up-old-forms!
DELETE FROM forms WHERE id IN (SELECT f.id FROM forms f
                                LEFT JOIN applications a ON a.form_id=f.id
                               WHERE a.form_id is null
                                 AND f.created_time < (NOW() - interval '1 month')
                                 AND f.id NOT IN (SELECT form_id FROM application_feedback)
                                 AND f.id NOT IN (SELECT id FROM forms f2 WHERE f2.key = f.key ORDER BY f2.id DESC LIMIT 5)
                               LIMIT :limit);