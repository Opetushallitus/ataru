(ns ataru.maksut.maksut-protocol)

(defprotocol MaksutServiceProtocol
  (create-kasittely-lasku [this lasku])

  (create-paatos-lasku [this lasku])

  (list-laskut-by-application-key [this application-key]))
