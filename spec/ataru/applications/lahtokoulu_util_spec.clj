(ns ataru.applications.lahtokoulu-util-spec
  (:require [ataru.time.coerce :as c]
            [ataru.time.format :as f]
            [speclj.core :refer [describe it should should= tags]]
            [ataru.applications.lahtokoulu-util :as lahtokoulu-util]))

(defn str->timestamp [str] (c/to-timestamp (c/from-string str)))
(defn str->datetime [str] (f/parse (:date-time f/formatters) str))

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
                (lahtokoulu-util/get-lahtokoulu-cutoff-timestamp 2023 non-yhteishaku-tarjonta)))

          (it "should return timestamp for 1st of June on haku year for yhteishaku"
              (should=
                (str->timestamp "2016-05-30T00:00:00.000Z")
                (lahtokoulu-util/get-lahtokoulu-cutoff-timestamp 2016 yhteishaku-tarjonta)))

          (it "should return nil, eg. no cutoff, when tarjonta data is completely missing"
              (should
                (nil? (lahtokoulu-util/get-lahtokoulu-cutoff-timestamp 2016 {})))))

(describe "filter-by-jatkuva-haku-hakemus-hakukausi"
          (tags :unit)

          (it "should match if hakemus-pvm and opiskelu-loppupvm both in spring period"
              (should= true (lahtokoulu-util/filter-by-jatkuva-haku-hakemus-hakukausi (str->datetime "2023-08-02T00:00:00.000Z") "2023-06-02T21:00:00.000Z")))

          (it "should match if hakemus-pvm and opiskelu-loppupvm both in autumn period"
              (should= true (lahtokoulu-util/filter-by-jatkuva-haku-hakemus-hakukausi (str->datetime "2023-09-30T00:00:00.000Z") "2023-08-31T21:00:00.000Z")))

          (it "should match if hakemus-pvm in the january of next year"
              (should= true (lahtokoulu-util/filter-by-jatkuva-haku-hakemus-hakukausi (str->datetime "2024-01-15T00:00:00.000Z") "2023-08-31T21:00:00.000Z")))

          (it "should not match if hakemus-pvm in autumn period and opiskelu-loppupvm in spring period"
              (should= false (lahtokoulu-util/filter-by-jatkuva-haku-hakemus-hakukausi (str->datetime "2023-09-02T00:00:00.000Z") "2023-06-02T21:00:00.000Z")))

          (it "should not match if hakemus-pvm in spring period and opiskelu-loppupvm in autumn period"
              (should= false (lahtokoulu-util/filter-by-jatkuva-haku-hakemus-hakukausi (str->datetime "2023-07-02T00:00:00.000Z") "2023-09-02T21:00:00.000Z")))

          (it "should not match if hakemus-pvm and opiskelu-loppupvm both in spring period, but different year"
              (should= false (lahtokoulu-util/filter-by-jatkuva-haku-hakemus-hakukausi (str->datetime "2024-07-02T00:00:00.000Z") "2023-06-02T21:00:00.000Z")))

          (it "should not match if hakemus-pvm and opiskelu-loppupvm both in autumn period, but different year"
              (should= false (lahtokoulu-util/filter-by-jatkuva-haku-hakemus-hakukausi (str->datetime "2024-09-30T00:00:00.000Z") "2023-08-31T21:00:00.000Z")))

          (it "should not match if hakemus-pvm in next year, after january"
              (should= false (lahtokoulu-util/filter-by-jatkuva-haku-hakemus-hakukausi (str->datetime "2024-02-15T00:00:00.000Z") "2023-08-31T21:00:00.000Z"))))

(describe "resolve lahtokoulu vuodet"
          (tags :unit)

          (it "should return just current year if hakuaika not in january"
              (should= [2025] (lahtokoulu-util/resolve-lahtokoulu-vuodet-jatkuva-haku (f/parse (:date-time f/formatters) "2025-05-15T00:00:00.000Z"))))

          (it "should return also preceeding year if hakuaika in january"
              (should= [2025 2024] (lahtokoulu-util/resolve-lahtokoulu-vuodet-jatkuva-haku (f/parse (:date-time f/formatters) "2025-01-15T00:00:00.000Z")))))
