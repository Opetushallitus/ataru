create trigger set_application_modified_time_on_kk_application_payment_update
    after insert or update or delete
    on kk_application_payments
    for each row
execute procedure update_application_modified_time();
