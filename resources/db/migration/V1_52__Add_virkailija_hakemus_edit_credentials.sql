create table virkailija_credentials (
  secret char(36),
  username varchar(50),
  oid varchar(50),
  application_key varchar(50) PRIMARY KEY
);