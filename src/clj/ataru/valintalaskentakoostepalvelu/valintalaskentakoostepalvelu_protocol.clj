(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol)

(defprotocol ValintalaskentakoostepalveluService
  (hakukohde-uses-valintalaskenta? [this hakukohde-oid])
  (opiskelijan-suoritukset [this haku-oid hakemus-oid])
  (opiskelijoiden-suoritukset [this haku-oid hakemus-oids])
  (hakemusten-harkinnanvaraisuus-valintalaskennasta [this hakemus-oids]))
