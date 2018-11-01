-- name: yesql-rajaavat-hakukohderyhmat
WITH ryhmat AS (
     SELECT haku_oid,
            hakukohderyhma_oid,
            raja,
            created_time
     FROM rajaavat_hakukohderyhmat
     WHERE haku_oid = :haku_oid
)
SELECT haku_oid AS "haku-oid",
       hakukohderyhma_oid AS "hakukohderyhma-oid",
       raja AS raja,
       (SELECT max(created_time) FROM ryhmat) AS "last-modified"
FROM ryhmat;

-- name: yesql-insert-rajaava-hakukohderyhma
INSERT INTO rajaavat_hakukohderyhmat (haku_oid, hakukohderyhma_oid, raja)
VALUES (:haku_oid, :hakukohderyhma_oid, :raja)
RETURNING haku_oid AS "haku-oid",
          hakukohderyhma_oid AS "hakukohderyhma-oid",
          raja AS raja,
          created_time AS "last-modified";

-- name: yesql-update-rajaava-hakukohderyhma
UPDATE rajaavat_hakukohderyhmat
SET raja = :raja,
    created_time = DEFAULT
WHERE haku_oid = :haku_oid AND
      hakukohderyhma_oid = :hakukohderyhma_oid AND
      NOT created_time > :if_unmodified_since::timestamptz
RETURNING haku_oid AS "haku-oid",
          hakukohderyhma_oid AS "hakukohderyhma-oid",
          raja AS raja,
          created_time AS "last-modified";

-- name: yesql-delete-rajaava-hakukohderyhma!
DELETE FROM rajaavat_hakukohderyhmat
WHERE haku_oid = :haku_oid AND
      hakukohderyhma_oid = :hakukohderyhma_oid;

-- name: yesql-priorisoivat-hakukohderyhmat
WITH ryhmat AS (
     SELECT haku_oid,
            hakukohderyhma_oid,
            prioriteetit,
            created_time
     FROM priorisoivat_hakukohderyhmat
     WHERE haku_oid = :haku_oid
)
SELECT haku_oid AS "haku-oid",
       hakukohderyhma_oid AS "hakukohderyhma-oid",
       prioriteetit AS prioriteetit,
       (SELECT max(created_time) FROM ryhmat) AS "last-modified"
FROM ryhmat;

-- name: yesql-insert-priorisoiva-hakukohderyhma
INSERT INTO priorisoivat_hakukohderyhmat (haku_oid, hakukohderyhma_oid, prioriteetit)
VALUES (:haku_oid, :hakukohderyhma_oid, :prioriteetit::jsonb)
RETURNING haku_oid AS "haku-oid",
          hakukohderyhma_oid AS "hakukohderyhma-oid",
          prioriteetit AS prioriteetit,
          created_time AS "last-modified";

-- name: yesql-update-priorisoiva-hakukohderyhma
UPDATE priorisoivat_hakukohderyhmat
SET prioriteetit = :prioriteetit::jsonb,
    created_time = DEFAULT
WHERE haku_oid = :haku_oid AND
      hakukohderyhma_oid = :hakukohderyhma_oid AND
      NOT created_time > :if_unmodified_since::timestamptz
RETURNING haku_oid AS "haku-oid",
          hakukohderyhma_oid AS "hakukohderyhma-oid",
          prioriteetit AS prioriteetit,
          created_time AS "last-modified";

-- name: yesql-delete-priorisoiva-hakukohderyhma!
DELETE FROM priorisoivat_hakukohderyhmat
WHERE haku_oid = :haku_oid AND
      hakukohderyhma_oid = :hakukohderyhma_oid;
