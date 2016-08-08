create table application_confirmation_emails (
  id                bigserial primary key,
  application_id    bigint references applications(id),
  recipient         varchar(128) not null,
  created_at        timestamp with time zone default now(),
  delivery_attempts bigint default 0,
  delivered_at      timestamp with time zone
);