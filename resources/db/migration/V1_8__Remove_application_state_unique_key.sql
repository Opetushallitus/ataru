alter table applications drop constraint applications_state_key;

update applications set state = 'received' where state is null;

alter table applications alter column state set not null, alter column state set default 'received';

create index applications_state_key on applications(state);

