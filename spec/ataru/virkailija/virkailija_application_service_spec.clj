(ns ataru.virkailija.virkailija-application-service-spec
  (:require [ataru.virkailija.virkailija-application-service :as application-service]
            [clj-time.coerce :as coerce]
            [speclj.core :refer [describe it should= should-be-nil tags]]))

(defn str->timestamp [str] (coerce/to-timestamp (coerce/from-string str)))

(def yhteishaku-tarjonta
  {:tarjonta
   {:hakukohteet                      []
    :haku-oid                         "haku-oid"
    :hakuaika                         {:start (str->timestamp "2023-03-02T00:00:00.000Z")
                                       :end   (str->timestamp "2023-05-05T00:00:00.000Z")}
    :kohdejoukko-uri                  "haunkohdejoukko_11#1"
    :yhteishaku                       true}})

(def non-yhteishaku-tarjonta
  {:tarjonta
   {:hakukohteet                      []
    :haku-oid                         "haku-oid"
    :hakuaika                         {:start (str->timestamp "2023-03-02T00:00:00.000Z")
                                       :end   (str->timestamp "2023-05-05T00:00:00.000Z")}
    :kohdejoukko-uri                  "haunkohdejoukko_13#1"
    :yhteishaku                       false}})

(describe "get-lahtokoulu-cutoff-timestamp"
          (tags :unit)

          (it "should return haku end timestamp for non-yhteishaku"
              (should=
                (str->timestamp "2023-05-05T00:00:00.000Z")
                (application-service/get-lahtokoulu-cutoff-timestamp 2023 non-yhteishaku-tarjonta)))

          (it "should return timestamp for 1st of June on haku year for yhteishaku"
              (should=
                (str->timestamp "2016-05-30T00:00:00.000Z")
                (application-service/get-lahtokoulu-cutoff-timestamp 2016 yhteishaku-tarjonta)))

          (it "should return nil, eg. no cutoff, when tarjonta data is completely missing"
              (should-be-nil
                (application-service/get-lahtokoulu-cutoff-timestamp 2016 {}))))
