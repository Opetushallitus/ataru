(ns ataru.util.app-utils
  (:require [environ.core :refer [env]]))

(defn get-app-id
  ([]
   (get-app-id nil))
  ([[app-id & _]]
   (keyword
     (or app-id
         (:app env)))))
