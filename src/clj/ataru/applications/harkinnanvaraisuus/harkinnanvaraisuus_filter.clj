(ns ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-filter
  (:require [ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-util :as hu]
            [ataru.application.harkinnanvaraisuus-types :as ht]
            [clojure.walk :as walk]
            [ataru.applications.application-store :as application-store]))

(defn- harkinnanvaraisuus-reason->is-harkinnanvarainen?
  [harkinnanvaraisuus-reason]
  (and
    (some? harkinnanvaraisuus-reason)
    (not= (:none ht/harkinnanvaraisuus-reasons) harkinnanvaraisuus-reason)))

(defn- is-harkinnanvarainen-for-whole-application?
  [application]
  (let [answers                   (:answers application)
        pick-value-fn (fn [answers question]
                        (question answers))
        harkinnanvaraisuus-reason (hu/get-common-harkinnanvaraisuus-reason answers pick-value-fn)
        result (harkinnanvaraisuus-reason->is-harkinnanvarainen? harkinnanvaraisuus-reason)]
    result))

(defn- is-harkinnanvarainen-for-some-hakukohde?
  [hakukohteet-filter application]
  (let [answers                    (:answers application)
        hakukohteet                (or hakukohteet-filter (:hakukohde application))
        pick-value-fn (fn [answers question]
                        (question answers))
        harkinnanvaraisuus-reasons (map #(hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde answers % pick-value-fn) hakukohteet)]
    (some harkinnanvaraisuus-reason->is-harkinnanvarainen? harkinnanvaraisuus-reasons)))

(defn- is-harkinnanvarainen?
  [hakukohteet-filter application]
  (or
    (is-harkinnanvarainen-for-whole-application? application)
    (is-harkinnanvarainen-for-some-hakukohde? hakukohteet-filter application)))

(defn- enrich-applications-with-answers
  [fetch-applications-content-fn applications]
  (let [applications-contents (->> applications
                                   (map :id)
                                   (fetch-applications-content-fn))]
    (letfn [(assoc-content [application]
              (let [answers (->> applications-contents
                                 (filter #(= (:id application) (:id %)))
                                 first
                                 :content
                                 :answers
                                 application-store/flatten-application-answers
                                 walk/keywordize-keys)]
                (assoc application :answers answers)))]
      (map assoc-content applications))))

(defn- filter-harkinnanvaraiset-applications
  [fetch-applications-content-fn hakukohteet-filter applications]
  (let [applications-contents (enrich-applications-with-answers fetch-applications-content-fn applications)
        harkinnanvaraiset-ids (->> applications-contents
                                   (filter (partial is-harkinnanvarainen? hakukohteet-filter))
                                   (map :id)
                                   set)]
    (filter (comp harkinnanvaraiset-ids :id) applications)))

(defn filter-applications-by-harkinnanvaraisuus
  [fetch-applications-content-fn applications filters]
  (let [only-harkinnanvaraiset? (-> filters :filters :harkinnanvaraisuus :only-harkinnanvaraiset)
        hakukohteet             (-> filters :selected-hakukohteet)]
    (if (and only-harkinnanvaraiset? (seq applications))
      (filter-harkinnanvaraiset-applications fetch-applications-content-fn hakukohteet applications)
      applications)))
