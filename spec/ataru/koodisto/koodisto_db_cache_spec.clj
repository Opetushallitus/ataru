(ns ataru.koodisto.koodisto-db-cache-spec
  (:require [ataru.util.http-util :as http-util]
            [ataru.koodisto.koodisto-codes :as koodisto-codes]
            [ataru.koodisto.koodisto-db-cache :as koodisto-db]
            [clojure.java.io :as io]
            [speclj.core :refer [describe tags it should= run-specs]])
  (:import java.net.URI
           java.time.ZonedDateTime))

(defn- fake-http-response [opts status body-path]
  {:opts    opts
   :status  status
   :headers {}
   :body    (some-> body-path (io/resource) (slurp))
   :error   nil})

(defn- fake-http-requester
  [mocks]
  (fn [{:keys [url _] :as opts}]
    (let [uri  (URI. url)
          path (.getPath uri)]
      (apply fake-http-response (into [opts] (get mocks path [500 (str "Unknown path " path)]))))))

(def school-from-organization-service {:uri     "oppilaitosnumero_02439"
                                       :version 1
                                       :value   "02439"
                                       :label   {:fi "Suonenjoen maatalous- ja palvelualojen oppilaitos"
                                                 :sv "Suonenjoen maatalous- ja palvelualojen oppilaitos"
                                                 :en "Suonenjoen maatalous- ja palvelualojen oppilaitos"}
                                       :valid   {:start (ZonedDateTime/parse "1994-08-01T00:00+03:00[Europe/Helsinki]")
                                                 :end   (ZonedDateTime/parse "1996-07-31T00:00+03:00[Europe/Helsinki]")}})

(def school-from-koodisto-only (merge school-from-organization-service {:label {:fi "Suonenjoen maatalousoppilaitos"}}))

(describe "loading 'oppilaitostyyppi' koodisto"
  (tags :unit :koodisto)

  (it "should load details from organization service"
    (with-redefs [koodisto-codes/institution-type-codes ["21"]
                  http-util/do-request (fake-http-requester {"/koodisto-service/rest/codeelement/oppilaitostyyppi_21/"
                                                             [200 "koodisto_service/codeelement/oppilaitostyyppi_21.json"]

                                                             "/koodisto-service/rest/codeelement/oppilaitosnumero_02439/1"
                                                             [200 "koodisto_service/json/oppilaitosnumero/koodi.json"]

                                                             "/organisaatio-service/rest/organisaatio/02439"
                                                             [200 "organisaatio_service/organisaatio/02439.json"]})]

      (should= [school-from-organization-service] (koodisto-db/get-koodi-options "oppilaitostyyppi"))))

  (it "should use koodisto provided labels if organization service doesn't contain school details"
    (with-redefs [koodisto-codes/institution-type-codes ["21"]
                  http-util/do-request (fake-http-requester {"/koodisto-service/rest/codeelement/oppilaitostyyppi_21/"
                                                             [200 "koodisto_service/codeelement/oppilaitostyyppi_21.json"]

                                                             "/koodisto-service/rest/codeelement/oppilaitosnumero_02439/1"
                                                             [200 "koodisto_service/json/oppilaitosnumero/koodi.json"]

                                                             "/organisaatio-service/rest/organisaatio/02439"
                                                             [404 nil]})]

      (should= [school-from-koodisto-only] (koodisto-db/get-koodi-options "oppilaitostyyppi"))))
  )

(run-specs)