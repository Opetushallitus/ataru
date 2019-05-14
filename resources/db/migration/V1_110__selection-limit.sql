CREATE TABLE selections (
  application_key TEXT NULL,
  question_id TEXT NOT NULL,
  answer_id TEXT NOT NULL,
  selection_group_id TEXT NOT NULL,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)'),
  PRIMARY KEY (application_key, selection_group_id)
);

CREATE TABLE initial_selections (
  selection_id TEXT NOT NULL,
  question_id TEXT NOT NULL,
  answer_id TEXT NOT NULL,
  selection_group_id TEXT NOT NULL,
  valid tstzrange NOT NULL DEFAULT tstzrange(now(), now() + INTERVAL '4 hour', '[)'),
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)'),
  PRIMARY KEY (selection_group_id, selection_id)
);

CREATE TABLE selections_history (like selections);
CREATE TABLE initial_selections_history (like initial_selections);

create or replace function selections_history_trigger() returns trigger as
$$
begin
    insert into selections_history (
        application_key,
        question_id,
        answer_id,
        selection_group_id,
        system_time,
        transaction_id
    ) values (
        old.application_key,
        old.question_id,
        old.answer_id,
        old.selection_group_id,
        tstzrange(lower(old.system_time), now(), '[)'),
        old.transaction_id
    );
    return null;
end;
$$ language plpgsql;

create trigger update_selections
after update on selections
for each row
when (old.transaction_id <> txid_current())
execute procedure selections_history_trigger();

create trigger delete_selections
after delete on selections
for each row
execute procedure selections_history_trigger();

create or replace function initial_selections_history_trigger() returns trigger as
$$
begin
    insert into initial_selections_history (
        selection_id,
        question_id,
        answer_id,
        selection_group_id,
        valid,
        system_time,
        transaction_id
    ) values (
        old.selection_id,
        old.question_id,
        old.answer_id,
        old.selection_group_id,
        old.valid,
        tstzrange(lower(old.system_time), now(), '[)'),
        old.transaction_id
    );
    return null;
end;
$$ language plpgsql;

create trigger update_initial_selections
after update on initial_selections
for each row
when (old.transaction_id <> txid_current())
execute procedure initial_selections_history_trigger();

create trigger delete_initial_selections
after delete on initial_selections
for each row
execute procedure initial_selections_history_trigger();
