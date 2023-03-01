(ns ataru.maksut.maksut-protocol)

(defprotocol MaksutServiceProtocol
  (create-kasittely-lasku [this lasku])

  (create-paatos-lasku [this lasku])

  (list-lasku-statuses [this keys])

  (list-laskut-by-application-key [this application-key])

  (download-receipt [this order-id]))
