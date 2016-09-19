alter table forms add column key varchar(40);
update forms set key = id || '-initial-system-generated-key';
create index forms_key on forms (key);

alter table forms add column created_time timestamp with time zone default now();
update forms set created_time = modified_time;
alter table forms drop column modified_time;

alter table forms add column created_by varchar(64) not null default 'SYSTEM';
update forms set created_by = modified_by;
alter table forms drop column modified_by;
