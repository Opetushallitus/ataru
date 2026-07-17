(ns ataru.maksut.maksut-protocol)

(defprotocol MaksutServiceProtocol
  (create-kk-application-payment-lasku [this lasku])

  (create-kasittely-lasku [this lasku])

  (create-paatos-lasku [this lasku])

  (list-lasku-statuses [this keys])

  (list-laskut-by-application-key [this application-key])

  (download-receipt [this order-id])

  (invalidate-laskut [this application-keys])

  (force-invalidate-laskut [this application-keys])

  (delete-laskut [this application-keys])

  (update-laskut-due-date [this application-keys due-date]))
