-- name: add-application-query<!
-- Add application
insert into applications (form_id, key, content, lang, state) values (:form_id, :key, :content, :lang, :state);

-- name: application-query-by-modified
select key, lang, form_id, modified_time, content from applications where form_id = :form_id and lang = :lang order by modified_time desc;
