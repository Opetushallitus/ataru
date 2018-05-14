-- name: yesql-set-1_96-content-ending!
update email_templates set content_ending = :content_ending where lang = :lang;

-- name: yesql-set-1_96-subject!
update email_templates set subject = :subject where lang = :lang;
