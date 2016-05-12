create table cas_ticketstore (
  ticket              varchar(100) primary key,
  login_time       timestamp with time zone default now()
);
