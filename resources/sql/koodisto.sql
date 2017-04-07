-- name: yesql-get-koodisto
select * from koodisto_cache where koodisto_uri = :koodisto_uri and version = :version order by created_at desc limit 1;

-- name: yesql-create-koodisto<!
insert into koodisto_cache (koodisto_uri, version, checksum, content) values (:koodisto_uri, :version, :checksum, :content);
