(ns ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-filter-spec
  (:require [speclj.core :refer [it describe tags should=]]
            [ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-filter :as hf]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util :as hu]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-reasons]]))

(def harkinnanvaraisuus-only-filters
  {:filters {:harkinnanvaraisuus {:only-harkinnanvaraiset true}}})

(defn- fake-fetch-applications-content
  [applications-map]
  (fn [application-ids]
    (keep (fn [id] (assoc (get applications-map id) :id id)) application-ids)))

(defn- fake-get-harkinnanvaraisuus-reason-for-hakukohde
  [expected-hakukohde-oid]
  (fn [_ hakukohde-oid _]
    (if (= expected-hakukohde-oid hakukohde-oid)
      (:ataru-oppimisvaikeudet harkinnanvaraisuus-reasons)
      (:none harkinnanvaraisuus-reasons))))

(def application-data
  {"application-1-id"
   {:form    "form-1-id"
    :content {:answers []}}})

(describe "filter-applications-by-harkinnanvaraisuus"
  (tags :unit :harkinnanvaraisuus)

  (it "returns empty vector when given empty vector"
    (let [input  []
          result (hf/filter-applications-by-harkinnanvaraisuus
                   (fake-fetch-applications-content {})
                   input
                   harkinnanvaraisuus-only-filters)]
      (should= [] result)))

  (it "returns all applications if :only-harkinnanvaraiset flag is false"
    (let [input  [{:id "application-1-id"}]
          result (hf/filter-applications-by-harkinnanvaraisuus
                   (fake-fetch-applications-content application-data)
                   input
                   {:harkinnanvaraisuus {:only-harkinnanvaraiset false}})]
      (should= input result)))

  (it "does not return application if it has no application-wide reason for harkinnanvaraisuus"
    (with-redefs [hu/get-common-harkinnanvaraisuus-reason (fn [_ _] nil)
                  hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde (fn [_] (:none harkinnanvaraisuus-reasons))]
      (let [input  [{:id "application-1-id"}]
            result (hf/filter-applications-by-harkinnanvaraisuus
                     (fake-fetch-applications-content application-data)
                     input
                     harkinnanvaraisuus-only-filters)]
        (should= [] result))))

  (it "returns application if it has an application-wide reason for harkinnanvaraisuus"
    (with-redefs [hu/get-common-harkinnanvaraisuus-reason (fn [_ _] (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons))]
      (let [input  [{:id "application-1-id"}]
            result (hf/filter-applications-by-harkinnanvaraisuus
                     (fake-fetch-applications-content application-data)
                     input
                     harkinnanvaraisuus-only-filters)]
        (should= input result))))

  (it "returns application if it has a harkinnanvaraisuus reason for some hakukohde"
    (with-redefs [hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde (fake-get-harkinnanvaraisuus-reason-for-hakukohde "hakukohde-oid-1")]
      (let [input  [{:id "application-1-id" :hakukohde ["hakukohde-oid-1" "hakukohde-oid-2"]}]
            result (hf/filter-applications-by-harkinnanvaraisuus
                     (fake-fetch-applications-content application-data)
                     input
                     harkinnanvaraisuus-only-filters)]
        (should= input result))))

  (describe "when filtering by hakukohteet as well"
    (it "returns application if it has a harkinnanvaraisuus reason for given hakukohde"
      (with-redefs [hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde (fake-get-harkinnanvaraisuus-reason-for-hakukohde "hakukohde-oid-1")]
        (let [input  [{:id "application-1-id" :hakukohde ["hakukohde-oid-1" "hakukohde-oid-2"]}]
              result (hf/filter-applications-by-harkinnanvaraisuus
                       (fake-fetch-applications-content application-data)
                       input
                       (assoc harkinnanvaraisuus-only-filters :selected-hakukohteet #{"hakukohde-oid-1"}))]
          (should= input result))))

    (it "does not return application if it has a harkinnanvaraisuus reason for another hakukohde"
      (with-redefs [hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde (fake-get-harkinnanvaraisuus-reason-for-hakukohde "hakukohde-oid-1")]
        (let [input  [{:id "application-1-id" :hakukohde ["hakukohde-oid-1" "hakukohde-oid-2"]}]
              result (hf/filter-applications-by-harkinnanvaraisuus
                       (fake-fetch-applications-content application-data)
                       input
                       (assoc harkinnanvaraisuus-only-filters :selected-hakukohteet #{"hakukohde-oid-2"}))]
          (should= [] result)))))

  (describe "integration test using actual answers"
    (it "does not return application when base education is 'Perusopetuksen oppimäärä'"
      (let [input            [{:id "application-1-id"}]
            application-data {"application-1-id"
                              {:form    "form-1-id"
                               :content {:answers [{:key   "base-education-2nd"
                                                    :value "1"}]}}}
            result           (hf/filter-applications-by-harkinnanvaraisuus
                               (fake-fetch-applications-content application-data)
                               input
                               harkinnanvaraisuus-only-filters)]
        (should= [] result)))

    (it "returns application when base education is 'Ulkomailla suoritettu koulutus'"
      (let [input            [{:id "application-1-id"}]
            application-data {"application-1-id"
                              {:form    "form-1-id"
                               :content {:answers [{:key   "base-education-2nd"
                                                    :value "0"}]}}}
            result           (hf/filter-applications-by-harkinnanvaraisuus
                               (fake-fetch-applications-content application-data)
                               input
                               harkinnanvaraisuus-only-filters)]
        (should= input result)))

    (it "returns application when it has harkinnanvaraisuus reason for some hakukohde"
      (let [input            [{:id "application-1-id" :hakukohde ["hakukohde-oid-1" "hakukohde-oid-2"]}]
            application-data {"application-1-id"
                              {:form    "form-1-id"
                               :content {:answers [{:key   "base-education-2nd"
                                                    :value "1"} ; perusopetuksen oppimäärä
                                                   {:key   "harkinnanvaraisuus-reason_hakukohde-oid-1"
                                                    :value "0"}]}}}
            result           (hf/filter-applications-by-harkinnanvaraisuus
                               (fake-fetch-applications-content application-data)
                               input
                               harkinnanvaraisuus-only-filters)]
        (should= input result)))

    (it "returns application when it has harkinnanvaraisuus reason for given hakukohde"
      (let [input            [{:id "application-1-id" :hakukohde ["hakukohde-oid-1" "hakukohde-oid-2"]}]
            application-data {"application-1-id"
                              {:form    "form-1-id"
                               :content {:answers [{:key   "base-education-2nd"
                                                    :value "1"} ; perusopetuksen oppimäärä
                                                   {:key   "harkinnanvaraisuus-reason_hakukohde-oid-1"
                                                    :value "0"}]}}}
            result           (hf/filter-applications-by-harkinnanvaraisuus
                               (fake-fetch-applications-content application-data)
                               input
                               (assoc harkinnanvaraisuus-only-filters :selected-hakukohteet #{"hakukohde-oid-1"}))]
        (should= input result)))

    (it "does not return application if it has a harkinnanvaraisuus reason for another hakukohde"
      (let [input            [{:id "application-1-id" :hakukohde ["hakukohde-oid-1" "hakukohde-oid-2"]}]
            application-data {"application-1-id"
                              {:form    "form-1-id"
                               :content {:answers [{:key   "base-education-2nd"
                                                    :value "1"} ; perusopetuksen oppimäärä
                                                   {:key   "harkinnanvaraisuus-reason_hakukohde-oid-1"
                                                    :value "0"}]}}}
            result           (hf/filter-applications-by-harkinnanvaraisuus
                               (fake-fetch-applications-content application-data)
                               input
                               (assoc harkinnanvaraisuus-only-filters :selected-hakukohteet #{"hakukohde-oid-2"}))]
        (should= [] result)))))
