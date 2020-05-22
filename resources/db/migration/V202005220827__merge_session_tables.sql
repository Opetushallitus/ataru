create index if not exists sessions_ticket on sessions ((data -> 'identity' ->> 'ticket'));

alter table sessions add column if not exists created_at timestamp with time zone default now();

update sessions
set data = jsonb_set(data, '{logged-in}', 'true'),
    created_at = c_ts.login_time
from cas_ticketstore c_ts
where c_ts.ticket = sessions.data -> 'identity' ->> 'ticket';

comment on table cas_ticketstore is 'Ei enää käytössä; voi poistaa, jos sessiotaulujen yhdistystä ei tarvitse rollbackata. Oli: Kirjautumisessa käytetyt CAS service ticketit';
