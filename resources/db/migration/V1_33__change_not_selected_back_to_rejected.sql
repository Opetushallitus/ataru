-- Changing rejected to not-selected was a mistake:
-- pilot users actually needed both rejected and not-selected
-- states. We'll support both, but have to revert the previous
-- migration's mapping first:
update application_reviews set state = 'rejected' where state = 'not-selected';
update application_events set new_review_state = 'rejected' where new_review_state = 'not-selected';
