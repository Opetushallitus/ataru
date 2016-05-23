(ns ataru.hakija.form-view
  (:require [ataru.hakija.banner :refer [banner]]
            [clojure.string :as str]))

(defn- application-id []
  (last (str/split (-> js/window .-location .-pathname) #"/")))

(defn form-view []
  [banner])
