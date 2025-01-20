ALTER TABLE information_requests ADD COLUMN IF NOT EXISTS send_reminder_time TIMESTAMP WITH TIME ZONE;
COMMENT ON COLUMN information_requests.send_reminder_time IS 'Milloin täydennyspyynnölle lähetetään muistutusviesti, jos muutoksia ei ole tehty';

ALTER TABLE information_requests ADD COLUMN IF NOT EXISTS reminder_processed_time TIMESTAMP WITH TIME ZONE;
COMMENT ON COLUMN information_requests.reminder_processed_time IS 'Muistutusviesti käsitelty';
