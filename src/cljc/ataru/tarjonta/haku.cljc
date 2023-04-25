(ns ataru.tarjonta.haku
  (:require [clojure.string :as string]))

(defn toisen-asteen-yhteishaku?
  [haku]
  (let [kohdejoukko-uri (get haku :kohdejoukko-uri)
        kohdejoukko     (when (some? kohdejoukko-uri)
                          (-> kohdejoukko-uri
                            (string/split #"#")
                            first))
        yhteishaku?     (get haku :yhteishaku)]
    (boolean
      (and yhteishaku?
        (= kohdejoukko "haunkohdejoukko_11")))))

(defn jatkuva-haku?
  [haku]
  (if-let [hakutapa (:hakutapa-uri haku)]
    (boolean (string/starts-with? hakutapa "hakutapa_03"))))