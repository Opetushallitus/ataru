-- name: yesql-get-applications-with-hakukohde-and-without-haku
select id, hakukohde from applications where hakukohde is not null and haku is null;

-- name: yesql-add-haku-to-application!
update applications set haku = :haku, haku_name = :haku_name where id = :application_id;
