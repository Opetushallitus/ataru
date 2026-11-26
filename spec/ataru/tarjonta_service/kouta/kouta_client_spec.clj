(ns ataru.tarjonta-service.kouta.kouta-client-spec
  (:require [ataru.cas.client :as cas]
            [ataru.ohjausparametrit.ohjausparametrit-client :as ohjausparametrit-client]
            [ataru.tarjonta-service.kouta.kouta-client :as kouta-client]
            [ataru.time :as t]
            [cheshire.core :as json]
            [clojure.string :as string]
            [speclj.core :refer [describe it should-contain should-not should= tags]]))

(def ^:private day-millis (* 24 60 60 1000))

(def ^:private haut
  [{:oid "1.2.246.562.29.00000000000000000001" :kohdejoukkoKoodiUri "haunkohdejoukko_12#1"}
   {:oid "1.2.246.562.29.00000000000000000002" :kohdejoukkoKoodiUri "haunkohdejoukko_12#1"}
   {:oid "1.2.246.562.29.00000000000000000003" :kohdejoukkoKoodiUri "haunkohdejoukko_11#1"}])

(def ^:private hkp-dates
  {"1.2.246.562.29.00000000000000000001" (+ (System/currentTimeMillis) day-millis)
   "1.2.246.562.29.00000000000000000002" (- (System/currentTimeMillis) day-millis)
   "1.2.246.562.29.00000000000000000003" (+ (System/currentTimeMillis) day-millis)})

(describe "kouta client get-active-kk-haku-oids"
  (tags :unit :tarjonta)

  (it "searches haut by current hakuvuosi and returns kk-haut whose PH_HKP is in the future"
    (let [requested-urls (atom [])]
      (with-redefs [cas/cas-authenticated-get (fn [_ url]
                                                (swap! requested-urls conj url)
                                                {:status 200 :body (json/generate-string haut)})
                    ohjausparametrit-client/get-ohjausparametrit (fn [haku-oid]
                                                                   {:PH_HKP {:date (get hkp-dates haku-oid)}})]
        (should= ["1.2.246.562.29.00000000000000000001"]
                 (kouta-client/get-active-kk-haku-oids nil))
        (should= 1 (count @requested-urls))
        (let [url (first @requested-urls)]
          (should-contain "kouta-internal/haku/search" url)
          (should-contain (str "hakuvuosi=" (t/year (t/now))) url)
          (should-not (string/includes? url "?vuosi="))
          (should-not (string/includes? url "&vuosi=")))))))
