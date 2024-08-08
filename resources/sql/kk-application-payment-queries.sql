-- name: yesql-get-kk-application-payment-states-for-person-oids
SELECT
  id,
  person_oid,
  start_term,
  start_year,
  state,
  created_time,
  modified_time
FROM kk_application_payment_states
WHERE person_oid IN (:person_oids) AND start_term = :start_term AND start_year = :start_year;

-- name: yesql-upsert-kk-application-payment-state<!
INSERT INTO kk_application_payment_states (person_oid, start_term, start_year, state, created_time, modified_time)
VALUES (:person_oid, :start_term, :start_year, :state, now(), now())
ON CONFLICT (person_oid, start_term, start_year)
  DO UPDATE SET state = :state, modified_time = now();

-- name: yesql-add-kk-application-payment-event<!
INSERT INTO kk_application_payment_events (kk_application_payment_state_id, new_state, event_type, virkailija_oid, message)
VALUES (:kk_application_payment_state_id, :new_state, :event_type, :virkailija_oid, :message);

-- name: yesql-get-kk-application-payment-events
SELECT
  id,
  kk_application_payment_state_id,
  new_state,
  event_type,
  virkailija_oid,
  message,
  created_time
FROM kk_application_payment_events
WHERE kk_application_payment_state_id IN (:kk_application_payment_state_ids);
