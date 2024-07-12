-- name: latest-siirtotiedosto-data
-- Returns latest successful siirtotiedosto-operation data
select id, window_start, window_end, run_start, run_end, info, success, error_message from siirtotiedostot
where success order by run_start desc limit 1;

-- name: upsert-siirtotiedosto-data!
-- Upserts siirtotiedosto-operation data
insert into siirtotiedostot (id, window_start, window_end, run_start, run_end, info, success, error_message)
values (:id::uuid, :window_start, :window_end, :run_start::timestamptz, :run_end::timestamptz,
        :info::jsonb, :success, :error_message)
on conflict on constraint siirtotiedostot_pkey do update
    set window_start = :window_start,
        window_end = :window_end,
        run_start = :run_start::timestamptz,
        run_end = :run_end::timestamptz,
        info = :info,
        success = :success,
        error_message = :error_message;