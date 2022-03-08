(ns ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-filter-spec
  (:require [speclj.core :refer [it describe tags should=]]
            [ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-filter :as hf]))

(def harkinnanvaraisuus-only-filters
  {:filters {:harkinnanvaraisuus {:only-harkinnanvaraiset true}}})

(def harkinnanvaraisuus-lomakeosio-key "harkinnanvaraisuus-wrapper")
(def harkinnanvaraisuus-no-answer-value "0")
(def harkinnanvaraisuus-yes-answer-value "1")

(defn- fake-fetch-applications-content
  [applications-map]
  (fn [application-ids]
    (keep (fn [id] (assoc (get applications-map id) :id id)) application-ids)))

(defn- fake-fetch-form
  [form-map]
  (fn [form-id]
    (get form-map form-id)))

(describe "filter-applications-by-harkinnanvaraisuus"
  (tags :unit :harkinnanvaraisuus)

  (it "returns empty vector when given empty vector"
    (let [input  []
          result (hf/filter-applications-by-harkinnanvaraisuus
                   (fake-fetch-applications-content {})
                   (fake-fetch-form {})
                   input
                   harkinnanvaraisuus-only-filters)]
      (should= [] result)))

  (it "returns all applications if :only-harkinnanvaraiset flag is false"
    (let [input            [{:id "application-1-id"}]
          application-data {"application-1-id"
                            {:form    "form-1-id"
                             :content {:answers []}}}
          form-data        {"form-1-id" {:content []}}
          result           (hf/filter-applications-by-harkinnanvaraisuus
                             (fake-fetch-applications-content application-data)
                             (fake-fetch-form form-data)
                             input
                             {:harkinnanvaraisuus {:only-harkinnanvaraiset false}})]
      (should= input result)))

  (it "returns application if it contains 'yes' answer to question with id 'harkinnanvaraisuus'"
    (let [input            [{:id "application-1-id"}]
          application-data {"application-1-id"
                            {:form    "form-1-id"
                             :content {:answers [{:original-question "harkinnanvaraisuus" :value harkinnanvaraisuus-yes-answer-value}]}}}
          form-data        {"form-1-id" {:content [{:id "harkinnanvaraisuus"}]}}
          result           (hf/filter-applications-by-harkinnanvaraisuus
                             (fake-fetch-applications-content application-data)
                             (fake-fetch-form form-data)
                             input
                             harkinnanvaraisuus-only-filters)]
      (should= input result)))

  (it "returns empty vector if input contains 'no' answer to question with id 'harkinnanvaraisuus'"
    (let [input            [{:id "application-1-id"}]
          application-data {"application-1-id"
                            {:form    "form-1-id"
                             :content {:answers [{:original-question "harkinnanvaraisuus" :value harkinnanvaraisuus-no-answer-value}]}}}
          form-data        {"form-1-id" {:content [{:id "harkinnanvaraisuus"}]}}
          result           (hf/filter-applications-by-harkinnanvaraisuus
                             (fake-fetch-applications-content application-data)
                             (fake-fetch-form form-data)
                             input
                             harkinnanvaraisuus-only-filters)]
      (should= [] result)))

  (describe "when filtering by hakukohteet as well"
    (it "returns application if it contains 'yes' answer to harkinnanvaraisuus question for given hakukohde"
      (let [input            [{:id "application-1-id"}]
            application-data {"application-1-id"
                              {:form    "form-1-id"
                               :content {:answers [{:original-question "harkinnanvaraisuus"
                                                    :duplikoitu-kysymys-hakukohde-oid "hakukohde-oid-1"
                                                    :value harkinnanvaraisuus-yes-answer-value}]}}}
            form-data        {"form-1-id" {:content [{:id "harkinnanvaraisuus"}]}}
            result           (hf/filter-applications-by-harkinnanvaraisuus
                               (fake-fetch-applications-content application-data)
                               (fake-fetch-form form-data)
                               input
                               (assoc harkinnanvaraisuus-only-filters :selected-hakukohteet #{"hakukohde-oid-1"}))]
        (should= input result)))

    (it "does not return application if it contains 'no' answer to harkinnanvaraisuus question for given hakukohde even if it contains 'yes' answer for another"
      (let [input            [{:id "application-1-id"}]
            application-data {"application-1-id"
                              {:form    "form-1-id"
                               :content {:answers [{:original-question "harkinnanvaraisuus"
                                                    :duplikoitu-kysymys-hakukohde-oid "hakukohde-oid-1"
                                                    :value harkinnanvaraisuus-no-answer-value}
                                                   {:original-question "harkinnanvaraisuus"
                                                    :duplikoitu-kysymys-hakukohde-oid "hakukohde-oid-2"
                                                    :value harkinnanvaraisuus-yes-answer-value}]}}}
            form-data        {"form-1-id" {:content [{:id "harkinnanvaraisuus"}]}}
            result           (hf/filter-applications-by-harkinnanvaraisuus
                               (fake-fetch-applications-content application-data)
                               (fake-fetch-form form-data)
                               input
                               (assoc harkinnanvaraisuus-only-filters :selected-hakukohteet #{"hakukohde-oid-1"}))]
        (should= [] result))))

  (describe "form has condition which can hide 'harkinnanvaraisuus' section"
    (let [form-data {"form-1-id" {:content [{:id harkinnanvaraisuus-lomakeosio-key}
                                            {:id                            "hiding-question"
                                             :section-visibility-conditions [{:section-name harkinnanvaraisuus-lomakeosio-key
                                                                              :condition    {:data-type           "str"
                                                                                             :comparison-operator "="
                                                                                             :answer-compared-to  "please-hide-the-section"}}]}]}}]
      (it "returns application if it contains the answer which hides 'harkinnanvaraisuus' section"
        (let [input            [{:id "application-1-id"}]
              application-data {"application-1-id"
                                {:form    "form-1-id"
                                 :content {:answers [{:key "hiding-question" :value "please-hide-the-section"}]}}}
              result           (hf/filter-applications-by-harkinnanvaraisuus
                                 (fake-fetch-applications-content application-data)
                                 (fake-fetch-form form-data)
                                 input
                                 harkinnanvaraisuus-only-filters)]
          (should= input result)))

      (it "does not return application if it contains answer which doesn't hide 'harkinnanvaraisuus' section"
        (let [input            [{:id "application-1-id"}]
              application-data {"application-1-id"
                                {:form    "form-1-id"
                                 :content {:answers [{:key "hiding-question" :value "something-else"}]}}}
              result           (hf/filter-applications-by-harkinnanvaraisuus
                                 (fake-fetch-applications-content application-data)
                                 (fake-fetch-form form-data)
                                 input
                                 harkinnanvaraisuus-only-filters)]
          (should= [] result)))))

  (describe "form has multiple conditions which can hide 'harkinnanvaraisuus' section"
    (let [form-data {"form-1-id" {:content [{:id harkinnanvaraisuus-lomakeosio-key}
                                            {:id                            "hiding-question-1"
                                             :section-visibility-conditions [{:section-name harkinnanvaraisuus-lomakeosio-key
                                                                              :condition    {:data-type           "str"
                                                                                             :comparison-operator "="
                                                                                             :answer-compared-to  "please-hide-the-section-1"}}
                                                                             {:section-name harkinnanvaraisuus-lomakeosio-key
                                                                              :condition    {:data-type           "str"
                                                                                             :comparison-operator "="
                                                                                             :answer-compared-to  "please-hide-the-section-2"}}]}
                                            {:id                            "hiding-question-2"
                                             :section-visibility-conditions [{:section-name harkinnanvaraisuus-lomakeosio-key
                                                                              :condition    {:data-type           "str"
                                                                                             :comparison-operator "="
                                                                                             :answer-compared-to  "please-hide-the-section-3"}}]}]}}]
      (it "returns application if it contains any answer which hides 'harkinnanvaraisuus' section"
        (let [input            [{:id "application-1-id"}]
              application-data {"application-1-id"
                                {:form    "form-1-id"
                                 :content {:answers [{:key "hiding-question-1" :value "please-hide-the-section-1"}]}}
                                "application-2-id"
                                {:form    "form-1-id"
                                 :content {:answers [{:key "hiding-question-1" :value "please-hide-the-section-2"}]}}
                                "application-3-id"
                                {:form    "form-1-id"
                                 :content {:answers [{:key "hiding-question-2" :value "please-hide-the-section-3"}]}}}
              result           (hf/filter-applications-by-harkinnanvaraisuus
                                 (fake-fetch-applications-content application-data)
                                 (fake-fetch-form form-data)
                                 input
                                 harkinnanvaraisuus-only-filters)]
          (should= input result))))))
