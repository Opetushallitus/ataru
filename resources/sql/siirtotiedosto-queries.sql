-- name: latest-siirtotiedosto-data
-- Returns latest successful siirtotiedosto-operation data
select id, uuid, window_start, window_end, run_start::text, run_end::text, info, success, error_message from siirtotiedosto
where success order by run_end desc limit 1;

-- name: upsert-siirtotiedosto-data!
-- Upserts siirtotiedosto-operation data
insert into siirtotiedosto (id, uuid, window_start, window_end, run_start, run_end, info, success, error_message)
values (nextval('siirtotiedosto_id_seq'), :id::uuid, :window_start, :window_end, :run_start::timestamptz, :run_end::timestamptz,
        :info::jsonb, :success, :error_message)
on conflict on constraint siirtotiedosto_pkey do update
    set window_start = :window_start,
        window_end = :window_end,
        run_start = :run_start::timestamptz,
        run_end = :run_end::timestamptz,
        info = :info,
        success = :success,
        error_message = :error_message;