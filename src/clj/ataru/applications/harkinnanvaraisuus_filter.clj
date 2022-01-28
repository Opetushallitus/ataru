(ns ataru.applications.harkinnanvaraisuus-filter
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :as string]))

(def harkinnanvaraisuus-key "harkinnanvaraisuus")
(def harkinnanvaraisuus-yes-answer-value "0")

(defn- is-application-harkinnanvarainen
  [application]
  (pprint "------------------__----------!!!Application!!!!---------------------------------")
  (pprint application)
  application)

(defn- answered-yes-to-harkinnanvaraisuus
  [application]
  (->> (-> application :content :answers)
       (filter #(= harkinnanvaraisuus-key (:original-question %)))
       (some #(= harkinnanvaraisuus-yes-answer-value (:value %)))
       (boolean)))

(defn- filter-harkinnanvaraiset-applications
  [fetch-application-content-fn applications]
  (let [applications-contents-and-forms (fetch-application-content-fn (map :id applications))
        harkinnanvaraiset-ids (->> applications-contents-and-forms
                                   (filter answered-yes-to-harkinnanvaraisuus)
                                   (map :id)
                                   set)]
    (pprint "---------------------------------------APPLICATIONS CONtENTS AND FORMS --------------------------------")
    (pprint applications-contents-and-forms)
    (println "harkinnanvaraiset-ids" harkinnanvaraiset-ids)
    (filter (comp harkinnanvaraiset-ids :id) applications)))

(defn filter-applications-by-harkinnanvaraisuus
  [fetch-applications-content-fn applications filters]
  (let [only-harkinnanvaraiset? (-> filters :harkinnanvaraisuus :only-harkinnanvaraiset)]
    (if only-harkinnanvaraiset?
      (filter-harkinnanvaraiset-applications fetch-applications-content-fn applications)
      applications)))



; harkinnanvarainen jos tutkinto ulkomailla suoritettu
; jos oppivelvollisuuden suorittaminen keskeytynyt
; kyllä kysymykseen harkinnanvaraisuuteen
; pohjakoulutuksessa valittu yksilöllistetty matematiikka ja äidinkieli
