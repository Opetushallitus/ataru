(ns ataru.ohjausparametrit.ohjausparametrit-protocol)

(defprotocol OhjausparametritService
  (get-parametri [this haku-oid]))
