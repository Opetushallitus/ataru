-- name: yesql-add-application-query<!
-- Add application
insert into applications (form_id, key, content, lang) values (:form_id, :key, :content, :lang);

-- name: yesql-application-query-by-modified
select key, lang, form_id as form, modified_time, content from applications where form_id = :form_id and lang = :lang order by modified_time desc limit :limit;

-- name: yesql-fetch-application-counts
select count(key) from applications where form_id = :form_id;

-- name: yesql-add-application-event!
-- Add application event
insert into application_events (application_id, event_type) values (:application_id, :event_type)
