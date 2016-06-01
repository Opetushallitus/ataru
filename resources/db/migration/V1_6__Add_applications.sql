
create table applications (
  id            bigserial primary key,
  key           varchar(40) unique,
  form_id       bigint references forms(id), -- We'll need to add version as part of foreign key here when forms get versions
  modified_time timestamp with time zone default now(),
  content       JSONB null
);

comment on column applications.key is 'Key visible to user, part of url sent via email. E.g. UUID';
