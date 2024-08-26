-- name: latest-siirtotiedosto-data
-- Returns latest successful siirtotiedosto-operation data
-- There should always be at least one, as per siirtotiedosto table migration.
select id, execution_uuid, window_start, window_end, run_start::text, run_end::text, info, success, error_message from siirtotiedosto
where success order by id desc limit 1;

-- name: insert-new-siirtotiedosto-operation!<
-- Inserts new siirtotiedosto-operation data
insert into siirtotiedosto (id, execution_uuid, window_start, window_end, run_start, run_end, info, success, error_message)
values (nextval('siirtotiedosto_id_seq'), :execution_uuid::uuid, :window_start, now(), now(), null,
        '{}'::jsonb, null, null) returning id, execution_uuid, window_start, window_end, info, success, error_message;

-- name: upsert-siirtotiedosto-data!
-- Upserts siirtotiedosto-operation data
update siirtotiedosto
    set run_end = now(),
        info = :info::jsonb,
        success = :success::boolean,
        error_message = :error_message
where id = :id;
