ALTER TABLE initial_selections DROP CONSTRAINT initial_selections_pkey;
ALTER TABLE initial_selections ADD CONSTRAINT initial_selections_pkey PRIMARY KEY (selection_group_id, selection_id, question_id);

ALTER TABLE selections DROP CONSTRAINT selections_pkey;
ALTER TABLE selections ADD CONSTRAINT selections_pkey PRIMARY KEY (application_key, selection_group_id, question_id);