-- State is now irrelevant/redundant, since applicatiin_reviews contains relevant review states, and application_events
-- contains history of state changes

alter table applications drop column state;

create table application_reviews (
  id              bigserial primary key,
  application_id  bigint references applications(id),
  modified_time   timestamp  with time zone default now(),
  state           varchar(40),
  notes           text -- Let's not limit the size of this at db level
);

comment on column application_reviews.state is 'For example accepted, rejected, ...';
-- application_reviews has a one-to-one relation with applications, let's enforce it:
alter table application_reviews add constraint application_reviews_uniq_application_id unique (application_id);

create table application_events (
  id              bigserial primary key,
  application_id  bigint references applications(id),
  event_type      varchar(40),
  time            timestamp  with time zone default now()
);

comment on column application_events.event_type is 'For example arrived, accepted, ...';
