(ns ataru.suoritus.suoritus-service-spec
  (:require [speclj.core :refer :all]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.suoritus.suoritus-client :as suoritus-client]
            [ataru.applications.suoritus-filter :as suoritus-filter]
            [clj-time.coerce :as coerce]))

(def service (suoritus-service/map->HttpSuoritusService {}))

(def test-henkilo-oid "1.123.123.123")
(def test-luokkatasot (suoritus-filter/luokkatasot-for-suoritus-filter))

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

(def jatkuva-haku-tarjonta
  {:tarjonta
   {:hakukohteet                      []
    :haku-oid                         "haku-oid"
    :haku-aika                        {:start (str->timestamp "2023-01-01T00:00:00.000Z")
                                       :end   (str->timestamp "2023-12-31T23:59:59.999Z")}
    :hakutapa-uri                     "hakutapa_03#1"
    :yhteishaku                       false}})

(def test-client-response
  [{:id "6d5f828d-355c-4c37-b719-ef5e4c6f6dde",
    :oppilaitosOid "1.1.111.111.111.111",
    :luokkataso "TELMA",
    :luokka "TELMA",
    :henkiloOid "1.123.123.123",
    :alkuPaiva "2022-02-02T21:00:00.000Z",
    :loppuPaiva "2024-06-02T21:00:00.000Z",
    :source "koski",
    :core {:oppilaitosOid "1.1.111.111.111.111",
           :luokkataso "TELMA",
           :henkiloOid "1.123.123.123"}}
   {:id "cd9edf5c-c411-4610-8dfe-7cc1a753896d",
    :oppilaitosOid "1.2.222.222.222.222",
    :luokkataso "9",
    :luokka "9E",
    :henkiloOid "1.123.123.123",
    :alkuPaiva "2020-08-11T21:00:00.000Z",
    :loppuPaiva "2023-06-02T21:00:00.000Z",
    :source "koski",
    :core {:oppilaitosOid "1.2.222.222.222.222",
           :luokkataso "9",
           :henkiloOid "1.123.123.123"}}
   {:id "abcddf6c-b311-4110-1234-8cc1a753896d",
    :oppilaitosOid "1.3.333.333.333.333",
    :luokkataso "FOO",
    :luokka "FOO",
    :henkiloOid "1.123.123.123",
    :alkuPaiva "2020-01-11T21:00:00.000Z",
    :loppuPaiva "2025-09-02T21:00:00.000Z",
    :source "koski",
    :core {:oppilaitosOid "1.3.333.333.333.333",
           :luokkataso "FOO",
           :henkiloOid "1.123.123.123"}}
   {:id "cccddf6c-b311-4110-1234-8cc1a753895a",
    :oppilaitosOid "1.5.555.555.555.555",
    :luokkataso "TELMA",
    :luokka "TELMA",
    :henkiloOid "1.123.123.123",
    :alkuPaiva "2024-01-05T00:00:00.000Z",
    :loppuPaiva "2024-12-05T21:00:00.000Z",
    :source "koski",
    :core {:oppilaitosOid "1.5.555.555.555.555",
           :luokkataso "TELMA",
           :henkiloOid "1.123.123.123"}}])

(def test-client-response-overlapping
  (conj test-client-response
        {:id "aaaaaaaa-355c-cccc-b719-ef5e4c6f6dde",
         :oppilaitosOid "1.4.444.444.444.444",
         :luokkataso "TELMA",
         :luokka "TELMA",
         :henkiloOid "1.123.123.123",
         :alkuPaiva "2022-08-03T21:00:00.000Z",
         :loppuPaiva "2024-06-02T21:00:00.000Z",
         :source "koski",
         :core {:oppilaitosOid "1.4.444.444.444.444",
                :luokkataso "TELMA",
                :henkiloOid "1.123.123.123"}}))

(describe "suoritus-service"
          (tags :unit :suoritus)
          (with-stubs)

          (around [spec]
                  (with-redefs [suoritus-client/opiskelijat (stub :opiskelijat
                                                                  {:return test-client-response})]
                    (spec)))
          (it "returns most recently started student class data filtered by luokkatasot when haku enddate used as cutoff date"
              (let [data (suoritus-service/opiskelijan-luokkatieto-for-hakemus service test-henkilo-oid test-luokkatasot "2023-03-02T00:00:00.000Z" non-yhteishaku-tarjonta)]
                (should= {:oppilaitos-oid "1.1.111.111.111.111"
                          :luokka "TELMA"
                          :luokkataso "TELMA"
                          :alkupaiva "2022-02-02T21:00:00.000Z"
                          :loppupaiva "2024-06-02T21:00:00.000Z"}
                         data)))

          (it "returns student class data that is ongoing on yhteishaku cutoff date"
              (let [data (suoritus-service/opiskelijan-luokkatieto-for-hakemus service test-henkilo-oid test-luokkatasot "2021-03-02T00:00:00.000Z" yhteishaku-tarjonta)]
                (should= {:oppilaitos-oid "1.2.222.222.222.222"
                          :luokka "9E"
                          :luokkataso "9"
                          :alkupaiva "2020-08-11T21:00:00.000Z"
                          :loppupaiva "2023-06-02T21:00:00.000Z"}
                         data)))

          (it "returns latest ongoing student class data by start date on yhteishaku cutoff date"
              (with-redefs [suoritus-client/opiskelijat (stub :opiskelijat
                                                              {:return test-client-response-overlapping})]
                (let [data (suoritus-service/opiskelijan-luokkatieto-for-hakemus service test-henkilo-oid test-luokkatasot "2023-03-02T00:00:00.000Z" yhteishaku-tarjonta)]
                  (should= {:oppilaitos-oid "1.4.444.444.444.444"
                            :luokka "TELMA"
                            :luokkataso "TELMA"
                            :alkupaiva "2022-08-03T21:00:00.000Z"
                            :loppupaiva "2024-06-02T21:00:00.000Z"}
                           data))))

          (it "doesn't return student class data if nothing was ongoing on cutoff date"
              (let [data (suoritus-service/opiskelijan-luokkatieto-for-hakemus service test-henkilo-oid test-luokkatasot "2019-03-02T00:00:00.000Z" yhteishaku-tarjonta)]
                (should= nil
                         data)))
          (it "returns latest ongoing student class data for jatkuva haku in spring period"
                (let [data (suoritus-service/opiskelijan-luokkatieto-for-hakemus service test-henkilo-oid test-luokkatasot "2023-08-02T00:00:00.000Z" jatkuva-haku-tarjonta)]
                  (should= {:oppilaitos-oid "1.2.222.222.222.222"
                            :luokka "9E"
                            :luokkataso "9"
                            :alkupaiva "2020-08-11T21:00:00.000Z"
                            :loppupaiva "2023-06-02T21:00:00.000Z"}
                           data)))
          (it "returns latest ongoing student class data for jatkuva haku in autumn period"
                (let [data (suoritus-service/opiskelijan-luokkatieto-for-hakemus service test-henkilo-oid test-luokkatasot "2025-01-15T00:00:00.000Z" jatkuva-haku-tarjonta)]
                  (should= {:oppilaitos-oid "1.5.555.555.555.555"
                            :luokka "TELMA"
                            :luokkataso "TELMA"
                            :alkupaiva "2024-01-05T00:00:00.000Z"
                            :loppupaiva "2024-12-05T21:00:00.000Z"}
                           data)))
          (it "doesn't return student class data if nothing matches to jatkuva haku in spring period"
              (let [data (suoritus-service/opiskelijan-luokkatieto-for-hakemus service test-henkilo-oid test-luokkatasot "2025-03-02T00:00:00.000Z" jatkuva-haku-tarjonta)]
                (should= nil
                         data)))
          (it "doesn't return student class data if nothing matches to jatkuva haku in autumn period"
              (let [data (suoritus-service/opiskelijan-luokkatieto-for-hakemus service test-henkilo-oid test-luokkatasot "2023-09-02T00:00:00.000Z" jatkuva-haku-tarjonta)]
                (should= nil
                         data))))
