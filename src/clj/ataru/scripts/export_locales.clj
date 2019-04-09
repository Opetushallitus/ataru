(ns ataru.scripts.export-locales
  (:require
   [cheshire.core :as json]
   [environ.core :refer [env]]
   [ataru.translations.texts :refer [virkailija-texts]]
   [ataru.config.core :refer [config]]))

(defn locale->entry [key [lang value]]
  {:key key :category "ataru-virkailija" :locale lang :value value})

; lein export-locales > l.json
; pbcopy < l.json
(defn -main
  []
  (let [entries (mapcat (fn [[key value]] (map (partial locale->entry key) value)) virkailija-texts)]
    (print (json/generate-string entries))))


