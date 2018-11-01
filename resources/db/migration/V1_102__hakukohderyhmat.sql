CREATE TABLE rajaavat_hakukohderyhmat (
       haku_oid text NOT NULL,
       hakukohderyhma_oid text NOT NULL,
       raja int NOT NULL,
       created_time timestamp with time zone DEFAULT now(),
       PRIMARY KEY (haku_oid, hakukohderyhma_oid)
);

CREATE TABLE priorisoivat_hakukohderyhmat (
       haku_oid text NOT NULL,
       hakukohderyhma_oid text NOT NULL,
       prioriteetit jsonb NOT NULL,
       created_time timestamp with time zone DEFAULT now(),
       PRIMARY KEY (haku_oid, hakukohderyhma_oid)
);
