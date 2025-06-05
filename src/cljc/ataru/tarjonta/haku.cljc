(ns ataru.tarjonta.haku
  (:require [clojure.string :as string]
            [ataru.constants :refer [hakutapa-jatkuva-haku]]))

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
  (some-> (:hakutapa-uri haku)
          (string/starts-with? hakutapa-jatkuva-haku)))

