(ns ataru.ohjausparametrit.utils
  (:require [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit]))

(defn synthetic-application-form-key
  [ohjausparametrit-service haku-oid]
  (get (ohjausparametrit/get-parametri ohjausparametrit-service haku-oid) :synteettisetLomakeavain))
