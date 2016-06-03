-- name: add-application-query<!
-- Add application
insert into applications (form_id, key, content, lang) values (:form_id, :key, :content, :lang);
