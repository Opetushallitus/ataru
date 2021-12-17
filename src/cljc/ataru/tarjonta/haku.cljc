(ns ataru.tarjonta.haku
  (:require [clojure.string :as string]))

(defn toisen-asteen-yhteishaku?
  [haku]
  (let [kohdejoukko-uri (get haku :kohdejoukko-uri)
        kohdejoukko (when (some? kohdejoukko-uri)
                      (-> kohdejoukko-uri
                        (string/split #"#")
                        first))
        yhteishaku? (get haku :yhteishaku)]
    (and yhteishaku?
      (= kohdejoukko "haunkohdejoukko_11"))))
