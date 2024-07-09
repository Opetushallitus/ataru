(ns ataru.kk-application-payment.utils
  (:require [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [clojure.string :as str]))

(defn requires-higher-education-application-fee?
  "Returns true if application fee should be charged for given haku"
  [tarjonta-service haku hakukohde-oids]
  (let [hakukohteet (tarjonta/get-hakukohteet
                      tarjonta-service
                      (remove nil? hakukohde-oids))]
    (and
      (boolean haku)
      (boolean hakukohteet)
      ; Kohdejoukko must be korkeakoulutus
      (and (string? (:kohdejoukko-uri haku))
           (str/starts-with? (:kohdejoukko-uri haku) "haunkohdejoukko_12#"))
      ; "Kohdejoukon tarkenne must be empty or siirtohaku
      (or (nil? (:kohdejoukon-tarkenne-uri haku))
          (str/starts-with? (:kohdejoukon-tarkenne-uri haku) "haunkohdejoukontarkenne_1#"))
      ; Must be tutkintoon johtava
      (boolean (some true? (map #(:tutkintoon-johtava? %) hakukohteet))))))
