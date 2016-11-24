-- Drop the obsolete field so that we don't accidentally
-- rely on the version in queries. Key should always be used
-- since event is not specific to a certain application version
alter table application_events drop column application_id;
