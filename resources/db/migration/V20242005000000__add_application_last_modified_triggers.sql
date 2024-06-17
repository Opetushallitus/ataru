
create or replace function update_application_modified_time() returns trigger as
$$
begin
    if (tg_op = 'DELETE') then
        update applications
        set modified_time = now()::timestamptz
        where key = old.application_key
          and modified_time <> now()::timestamptz;
        return null;
    else
        update applications
        set modified_time = now()::timestamptz
        where key = new.application_key
          and modified_time <> now()::timestamptz;
        return null;
    end if;
end;
$$ language plpgsql;

create trigger set_application_modified_time_on_hakukohde_review_update
    after insert or update or delete
    on application_hakukohde_reviews
    for each row
execute procedure update_application_modified_time();

create trigger set_application_modified_time_on_application_event_update
    after insert or update or delete
    on application_events
    for each row
execute procedure update_application_modified_time();

create trigger set_application_modified_time_on_review_notes_update
    after insert or update or delete
    on application_review_notes
    for each row
execute procedure update_application_modified_time();

create trigger set_application_modified_time_on_attachment_review_update
    after insert or update or delete
    on application_hakukohde_attachment_reviews
    for each row
execute procedure update_application_modified_time();
