begin;
select setval('application_events_id_seq', (select max(id) from application_events));
select setval('application_feedback_id_seq', (select max(id) from application_feedback));
select setval('application_hakukohde_attachment_reviews_id_seq', (select max(id) from application_hakukohde_attachment_reviews));
select setval('application_hakukohde_reviews_id_seq', (select max(id) from application_hakukohde_reviews));
select setval('application_review_notes_id_seq', (select max(id) from application_review_notes));
select setval('application_reviews_id_seq', (select max(id) from application_reviews));
select setval('application_secrets_id_seq', (select max(id) from application_secrets));
select setval('applications_id_seq', (select max(id) from applications));
select setval('email_templates_id_seq', (select max(id) from email_templates));
select setval('forms_id_seq', (select max(id) from forms));
select setval('information_requests_id_seq', (select max(id) from information_requests));
select setval('job_iterations_id_seq', (select max(id) from job_iterations));
select setval('jobs_id_seq', (select max(id) from jobs));
select setval('koodisto_cache_id_seq', (select max(id) from koodisto_cache));
select setval('application_oid', (select max(trim(leading '0' from substring(key, char_length('1.2.246.562.11.') + 1))::bigint)
                                  from applications));
commit;
