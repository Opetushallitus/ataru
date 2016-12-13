update application_reviews set state = 'unprocessed' where state = 'received';
update application_events set new_review_state = 'unprocessed' where new_review_state = 'received';

update application_reviews set state = 'not-selected' where state = 'rejected';
update application_events set new_review_state = 'not-selected' where new_review_state = 'rejected';

update application_reviews set state = 'selected' where state = 'approved';
update application_events set new_review_state = 'selected' where new_review_state = 'approved';

