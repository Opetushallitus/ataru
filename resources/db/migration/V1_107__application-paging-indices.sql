CREATE INDEX applications_order_created_time ON applications (date_trunc('second', timezone('Europe/Helsinki', created_time)), key);
CREATE INDEX applications_haku_order_created_time ON applications (haku, date_trunc('second', timezone('Europe/Helsinki', created_time)), key);

CREATE INDEX applications_order_submitted ON applications (date_trunc('second', timezone('Europe/Helsinki', submitted)), key);
CREATE INDEX applications_haku_order_submitted ON applications (haku, date_trunc('second', timezone('Europe/Helsinki', submitted)), key);

CREATE INDEX applications_order_name ON applications (last_name COLLATE "fi_FI", preferred_name COLLATE "fi_FI", key);
CREATE INDEX applications_haku_order_name ON applications (haku, last_name COLLATE "fi_FI", preferred_name COLLATE "fi_FI", key);
