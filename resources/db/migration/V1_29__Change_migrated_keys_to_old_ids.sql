-- Fix <version>-initial-system-generated-key keys (from earlier migration) to be just the version
-- which was the old way of referencing the forms in hakija. This way the production form references
-- with old style references should just work.
update forms set key = regexp_replace(key, '(\d+).*', '\1') where key like '%initial-system-generated-key';
