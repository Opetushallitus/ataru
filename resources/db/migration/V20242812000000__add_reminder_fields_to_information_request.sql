ALTER TABLE information_requests ADD COLUMN IF NOT EXISTS send_reminder_time TIMESTAMP WITH TIME ZONE
COMMENT ON COLUMN information_request.send_reminder_at IS 'Milloin täydennyspyynnölle lähetetään muistutusviesti, jos muutoksia ei ole tehty';

ALTER TABLE information_requests ADD COLUMN IF NOT EXISTS reminder_processed_time TIMESTAMP WITH TIME ZONE
COMMENT ON COLUMN information_request.reminder_processed IS 'Muistutusviesti käsitelty';
