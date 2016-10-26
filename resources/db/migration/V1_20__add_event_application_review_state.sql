
-- Let's anticipate that we'll have many types of events for application, like "confirmation sent" etc. and prepare
-- for that.
-- Change old event_type to new_review_state (contains now 'received' only) and add event_type again which
-- now tells what kind of event this is (later we will have others than 'review-state-change'
alter table application_events rename column event_type to new_review_state;
alter table application_events add column event_type varchar(40);
-- Update existing events to review-state-change, it has been the only event-type so far in practice
update application_events set event_type = 'review-state-change';
