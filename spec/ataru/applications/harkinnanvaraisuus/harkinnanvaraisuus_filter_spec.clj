(ns ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-filter-spec
  (:require [speclj.core :refer [it describe tags should=]]
            [ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-filter :as hf]))

(def harkinnanvaraisuus-only-filters
  {:filters {:harkinnanvaraisuus {:only-harkinnanvaraiset true}}})

(def application-data
  {"application-1-id"
   {:form    "form-1-id"
    :content {:answers []}}})

(defn- fake-hakemusten-harkinnanvaraisuus-valintalaskennasta
  [response]
  (fn [_]
    response))

(describe "filter-applications-by-harkinnanvaraisuus"
  (tags :unit :harkinnanvaraisuus)

  (it "returns empty vector when given empty vector"
    (let [input  []
          result (hf/filter-applications-by-harkinnanvaraisuus
                   (fake-hakemusten-harkinnanvaraisuus-valintalaskennasta [])
                   input
                   harkinnanvaraisuus-only-filters)]
      (should= [] result)))

  (it "returns all applications if :only-harkinnanvaraiset flag is false"
    (let [input  [{:id "application-1-id"}]
          result (hf/filter-applications-by-harkinnanvaraisuus
                   (fake-hakemusten-harkinnanvaraisuus-valintalaskennasta [])
                   input
                   {:harkinnanvaraisuus {:only-harkinnanvaraiset false}})]
      (should= input result)))

  (it "does not return application if it has no application-wide reason for harkinnanvaraisuus"
    (let [input  [{:id "application-1-id"}]
          result (hf/filter-applications-by-harkinnanvaraisuus
                   (fake-hakemusten-harkinnanvaraisuus-valintalaskennasta
                     [{:hakemusOid  "application-1-id"
                       :hakutoiveet [{:hakukohdeOid            "hakukohde-oid-1"
                                      :harkinnanvaraisuudenSyy "EI_HARKINNANVARAINEN"}]}])
                   input
                   harkinnanvaraisuus-only-filters)]
      (should= [] result)))

  (it "returns application if it has an application-wide reason for harkinnanvaraisuus"
    (let [input  [{:id "application-1-id"}]
          result (hf/filter-applications-by-harkinnanvaraisuus
                   (fake-hakemusten-harkinnanvaraisuus-valintalaskennasta
                     [{:hakemusOid  "application-1-id"
                       :hakutoiveet [{:hakukohdeOid            "hakukohde-oid-1"
                                      :harkinnanvaraisuudenSyy "ATARU_ULKOMAILLA_OPISKELTU"}]}])
                   input
                   harkinnanvaraisuus-only-filters)]
      (should= input result)))

  (it "returns application if it has a harkinnanvaraisuus reason for some hakukohde"
    (let [input  [{:id "application-1-id" :hakukohde ["hakukohde-oid-1" "hakukohde-oid-2"]}]
          result (hf/filter-applications-by-harkinnanvaraisuus
                   (fake-hakemusten-harkinnanvaraisuus-valintalaskennasta
                     [{:hakemusOid  "application-1-id"
                       :hakutoiveet [{:hakukohdeOid            "hakukohde-oid-1"
                                      :harkinnanvaraisuudenSyy "ATARU_SOSIAALISET_SYYT"}
                                     {:hakukohdeOid            "hakukohde-oid-2"
                                      :harkinnanvaraisuudenSyy "EI_HARKINNANVARAINEN"}]}])
                   input
                   harkinnanvaraisuus-only-filters)]
      (should= input result)))

  (describe "when filtering by hakukohteet as well"
    (it "returns application if it has a harkinnanvaraisuus reason for given hakukohde"
      (let [input  [{:id "application-1-id" :hakukohde ["hakukohde-oid-1" "hakukohde-oid-2"]}]
            result (hf/filter-applications-by-harkinnanvaraisuus
                     (fake-hakemusten-harkinnanvaraisuus-valintalaskennasta
                       [{:hakemusOid  "application-1-id"
                         :hakutoiveet [{:hakukohdeOid            "hakukohde-oid-1"
                                        :harkinnanvaraisuudenSyy "ATARU_SOSIAALISET_SYYT"}
                                       {:hakukohdeOid            "hakukohde-oid-2"
                                        :harkinnanvaraisuudenSyy "EI_HARKINNANVARAINEN"}]}])
                     input
                     (assoc harkinnanvaraisuus-only-filters :selected-hakukohteet #{"hakukohde-oid-1"}))]
        (should= input result)))

    (it "does not return application if it has a harkinnanvaraisuus reason for another hakukohde"
      (let [input  [{:id "application-1-id" :hakukohde ["hakukohde-oid-1" "hakukohde-oid-2"]}]
            result (hf/filter-applications-by-harkinnanvaraisuus
                     (fake-hakemusten-harkinnanvaraisuus-valintalaskennasta
                       [{:hakemusOid  "application-1-id"
                         :hakutoiveet [{:hakukohdeOid            "hakukohde-oid-1"
                                        :harkinnanvaraisuudenSyy "ATARU_SOSIAALISET_SYYT"}
                                       {:hakukohdeOid            "hakukohde-oid-2"
                                        :harkinnanvaraisuudenSyy "EI_HARKINNANVARAINEN"}]}])
                     input
                     (assoc harkinnanvaraisuus-only-filters :selected-hakukohteet #{"hakukohde-oid-2"}))]
        (should= [] result)))))
