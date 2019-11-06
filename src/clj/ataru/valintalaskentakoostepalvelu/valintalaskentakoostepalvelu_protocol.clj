(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol)

(defprotocol ValintalaskentakoostepalveluService
  (hakukohde-uses-valintalaskenta? [this hakukohde-oid]))
