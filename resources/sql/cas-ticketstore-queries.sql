-- name: yesql-add-ticket-query!
-- User logged in, add ticket
INSERT INTO cas_ticketstore (ticket) VALUES (:ticket);

-- name: yesql-remove-ticket-query!
-- User logged out, remove the ticket
DELETE FROM cas_ticketstore
WHERE ticket = :ticket;

-- name: yesql-ticket-exists-query
-- Check that the ticket exists
SELECT ticket
FROM cas_ticketstore
WHERE ticket = :ticket;
