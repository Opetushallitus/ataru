-- name: yesql-get-kk-application-payments-for-application-keys
SELECT
  id,
  application_key,
  state,
  reason,
  due_date,
  total_sum,
  maksut_secret,
  required_at,
  reminder_sent_at,
  approved_at,
  created_at,
  modified_at
FROM kk_application_payments
WHERE application_key IN (:application_keys);

-- name: yesql-get-awaiting-kk-application-payments
SELECT
  id,
  application_key,
  state,
  reason,
  due_date,
  total_sum,
  maksut_secret,
  required_at,
  reminder_sent_at,
  approved_at,
  created_at,
  modified_at
FROM kk_application_payments
WHERE state = 'awaiting';

-- name: yesql-upsert-kk-application-payment<!
INSERT INTO kk_application_payments (
  application_key,
  state,
  reason,
  due_date,
  total_sum,
  maksut_secret,
  required_at,
  reminder_sent_at,
  approved_at
)
VALUES (
  :application_key,
  :state,
  :reason,
  :due_date::date,
  :total_sum,
  :maksut_secret,
  :required_at::timestamptz,
  :reminder_sent_at::timestamptz,
  :approved_at::timestamptz
)
ON CONFLICT (application_key)
  DO UPDATE SET
  state = :state,
  reason = :reason,
  due_date = :due_date::date,
  total_sum = :total_sum,
  maksut_secret = :maksut_secret,
  required_at = :required_at::timestamptz,
  reminder_sent_at = :reminder_sent_at::timestamptz,
  approved_at = :approved_at::timestamptz;

-- name: yesql-get-kk-application-payments-history-for-application-keys
SELECT
  id,
  application_key,
  state,
  reason,
  due_date,
  total_sum,
  maksut_secret,
  required_at,
  reminder_sent_at,
  approved_at,
  created_at,
  modified_at
FROM kk_application_payments_history
WHERE application_key IN (:application_keys);

-- name: yesql-update-maksut-secret!
UPDATE kk_application_payments
SET maksut_secret = :maksut_secret
WHERE application_key = :application_key;
