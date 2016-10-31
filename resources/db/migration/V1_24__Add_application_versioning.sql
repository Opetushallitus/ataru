alter table applications drop constraint "applications_key_key";
alter table application_events add column application_key varchar(40);
alter table application_confirmation_emails add column application_key varchar(40);
alter table application_reviews add column application_key varchar(40);

alter table applications add column created_time timestamp with time zone default now();
update applications set created_time = modified_time;
alter table applications drop column modified_time;
