(ns ataru.util.app-utils
  (:require [clojure.core.match :as m]
            [environ.core :refer [env]]))

(defn get-app-id
  ([]
   (get-app-id nil))
  ([[app-id & _]]
   (let [app-id (keyword (or app-id (:app env)))]
     (m/match app-id
       :ataru-editori :virkailija
       :ataru-hakija  :hakija
       app-id         app-id))))
