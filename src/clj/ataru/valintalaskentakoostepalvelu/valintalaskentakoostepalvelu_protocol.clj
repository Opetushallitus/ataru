(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol)

(defprotocol ValintalaskentakoostepalveluService
  (hakukohde-uses-valintalaskenta? [this hakukohde-oid])
  (opiskelijan-suoritukset [this haku-oid hakemus-oid])
  (hakemusten-harkinnanvaraisuus-valintalaskennasta [this hakemukset-with-harkinnanvaraisuus]))
