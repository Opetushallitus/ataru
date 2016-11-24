-- Drop the obsolete field so that we don't accidentally
-- rely on the version in queries. Key should always be used
-- since review is not specific to a certain application version
alter table application_reviews drop column application_id;
