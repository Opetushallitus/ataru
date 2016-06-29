(ns ataru.virkailija.test-util
  (:require [jayq.core :as jq]
            [cljs.test :refer-macros [async]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn app-frame
  []
  (-> (jq/$ :#test)
      (first)
      (.-contentWindow)
      (.-document)
      (jq/$)))

(defn setup
  []
  (async done
         (doto (jq/$ :#test)
           (jq/attr :src "/lomake-editori/")
           (jq/attr :width "1024")
           (jq/attr :height "768"))
         (done)))

(defn get-element
  [xpath]
  (let [elements (-> (app-frame)
                     (.xpath xpath))]
    (when (= 1 (count elements))
      (first elements))))