-- name: yesql-get-status-poll-applications
SELECT a.key, COALESCE(ahr.state, 'unprocessed') state FROM applications a
LEFT OUTER JOIN application_hakukohde_reviews ahr ON (a.key = ahr.application_key)
WHERE a.form_id IN
      (SELECT id FROM forms f
                 WHERE f.key in (:form_keys)
                 OR f.properties->'payment'->>'type' IN ('payment-type-tutu', 'payment-type-astu'))
AND (ahr.state IS NULL OR ahr.state IN ('unprocessed', 'decision-fee-outstanding'));

-- name: yesql-get-payment-reminders
SELECT * FROM payment_reminders
    WHERE status IS NULL
    AND send_reminder_time::date <= current_date;

-- name: yesql-add-payment-reminder<!
INSERT INTO payment_reminders (
    application_key,
    message,
    lang,
    send_reminder_time,
    order_id
) VALUES (
    :application_key,
    :message,
    :lang,
    :send_reminder_time,
    :order_id
);

-- name: yesql-set-reminder-handled!
UPDATE payment_reminders
    SET handled_at = now(),
        status = :status::payment_reminder_status
    WHERE id = :id;