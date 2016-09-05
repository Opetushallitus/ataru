create table koodisto_cache (
  id             serial primary key,
  created_at     timestamp with time zone default now(),
  koodisto_uri   varchar(256) not null,
  version        integer not null,
  checksum       varchar(64) not null,
  content        jsonb not null,

  unique (koodisto_uri, version, checksum)
);
