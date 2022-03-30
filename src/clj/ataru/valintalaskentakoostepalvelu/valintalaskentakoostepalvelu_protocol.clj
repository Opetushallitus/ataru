(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol)

(defprotocol ValintalaskentakoostepalveluService
  (hakukohde-uses-valintalaskenta? [this hakukohde-oid])
  (opiskelijan-suoritukset [this haku-oid hakemus-oid]))
