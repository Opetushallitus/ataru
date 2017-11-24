(ns ataru.ohjausparametrit.ohjausparametrit-client
  (:require [ataru.util.http-util :as hu]
            [ataru.config.url-helper :as url-helper]
            [clj-time.coerce :as t]))

(defn get-ohjausparametrit [haku-oid]
  (let [resp (-> :ohjausparametrit-service.parametri
                  (url-helper/resolve-url haku-oid)
                  (hu/do-request))]
    (clojure.walk/prewalk (fn [x]
                            (cond-> x
                              (and (vector? x)
                                   (some #{(first x)} [:dateStart :dateEnd :date :__modified__]))
                              (update 1 t/from-long)))
                          resp)))
