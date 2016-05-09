-- name: get-session-query
-- Get session data
select data from sessions where key = :key;

-- name: add-session-query!
-- Add session
insert into sessions (key, data) values (:key, :data);

-- name: update-session-query!
-- Update session
update sessions set data = :data where key = :key;

-- name: delete-session-query!
-- Deletes session
delete from session where key = :key;
