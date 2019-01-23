CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TEXT SEARCH CONFIGURATION unaccent_simple (COPY = simple);

ALTER TEXT SEARCH CONFIGURATION unaccent_simple
  ALTER MAPPING FOR hword, hword_part, word
    WITH unaccent, simple;

DROP INDEX applications_name_idx;
CREATE INDEX applications_name_idx ON applications
USING GIN (to_tsvector('unaccent_simple', preferred_name || ' ' || last_name));
