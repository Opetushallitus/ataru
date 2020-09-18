-- name: yesql-selection-state-used-in-hakukohde
select exists(select true
              from application_hakukohde_reviews ahr
              where ahr.hakukohde = :hakukohde_oid
                and ahr.requirement = 'selection-state');
