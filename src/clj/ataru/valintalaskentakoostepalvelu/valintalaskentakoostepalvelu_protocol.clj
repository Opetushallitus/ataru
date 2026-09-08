(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol)

(defprotocol ValintalaskentakoostepalveluService
  (hakukohde-uses-valintalaskenta? [this hakukohde-oid])
  (opiskelijan-suoritukset [this haku-oid hakemus-oid])
  (hakemusten-harkinnanvaraisuus-valintalaskennasta [this hakemus-oids])
  (hakemusten-harkinnanvaraisuus-valintalaskennasta-no-cache [this hakemus-oids]))
