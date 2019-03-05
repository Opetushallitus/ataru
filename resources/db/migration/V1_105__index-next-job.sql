CREATE INDEX job_iterations_next_activation_idx ON job_iterations (next_activation) WHERE NOT executed AND NOT final;
