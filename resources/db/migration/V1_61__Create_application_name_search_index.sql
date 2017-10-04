create index applications_name_idx on applications
using GIN (to_tsvector('simple', preferred_name || ' ' || last_name));
