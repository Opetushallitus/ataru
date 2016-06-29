-- name: add-application-query<!
-- Add application
insert into applications (form_id, key, content, lang, state) values (:form_id, :key, :content, :lang, :state);
