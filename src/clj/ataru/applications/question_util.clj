(ns ataru.applications.question-util
  (:require [ataru.translations.texts :refer [base-education-2nd-module-texts]]
            [clojure.walk :refer [keywordize-keys]]
            [ataru.util :refer [answers-by-key]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-choice-key base-education-wrapper-key base-education-2nd-language-value-to-lang]]))

(def sora-question-wrapper-label {:fi "Terveydelliset tekijät " :sv "Hälsoskäl"})

(def lukio-kaksoistutkinto-wrapper-label {:fi "Ammatilliset opinnot lukiokoulutuksen ohella",
                                          :sv "Yrkesinriktade studier vid sidan av gymnasieutbildningen"})

(def amm-kaksoistutkinto-wrapper-label {:fi "Lukio-opinnot ammatillisen koulutuksen ohella",
                                        :sv "Gymnasiestudier vid sidan av den yrkesinriktade utbildningen"})

(def kiinnostunut-oppisopimuskoulutuksesta-wrapper-label {:fi "Oppisopimuskoulutus ",
                                                          :sv "Läroavtalsutbildning"})

(def urheilijan-lisakysymykset-wrapper-label {:fi "Urheilijan lisäkysymykset ammatillisissa kohteissa",
                                              :sv "Tilläggsfrågor för idrottare i yrkesinriktade ansökningsmål"})

;This should at some point be replaced by hardcoded id's for the fields.
(defn assoc-deduced-vakio-answers-for-toinen-aste-application [questions application]
                                               (let [answers (:keyValues application)
                                                     pohjakoulutus-vuosi-key (some->> (:tutkintovuosi-keys questions)
                                                                                 (filter #(not (nil? (get answers (name %)))))
                                                                                 first
                                                                                 name)
                                                     pohjakoulutus-vuosi (when pohjakoulutus-vuosi-key
                                                                      (get answers pohjakoulutus-vuosi-key))
                                                     pohjakoulutus-kieli-key (some->> (:tutkintokieli-keys questions)
                                                                                 (filter #(not (nil? (get answers (name %)))))
                                                                                 first
                                                                                 name)
                                                     pohjakoulutus-kieli-answer (when pohjakoulutus-kieli-key
                                                                                  (get answers (name pohjakoulutus-kieli-key)))
                                                     pohjakoulutus-kieli (base-education-2nd-language-value-to-lang pohjakoulutus-kieli-answer)]
                                                 (update application :keyValues (fn [kv] (merge kv {"pohjakoulutus_vuosi" pohjakoulutus-vuosi
                                                                                                    "pohjakoulutus_kieli" pohjakoulutus-kieli})))))

(defn get-hakurekisteri-toinenaste-specific-questions
  [form]
  (let [content (:content (keywordize-keys form))
        base-education-options-followups (->> content
                                    (filter #(= base-education-wrapper-key (:id %)))
                                    first
                                    :children
                                    (filter #(= base-education-choice-key (:id %)))
                                    first
                                    :options
                                    (mapcat :followups))
        tutkintovuosi-keys (->> base-education-options-followups
                                (filter #(= (:year-of-graduation-question base-education-2nd-module-texts) (:label %)))
                                (map #(keyword (:id %))))
        tutkintokieli-keys (->> base-education-options-followups
                                (filter #(= (:study-language base-education-2nd-module-texts) (:label %)))
                                (map #(keyword (:id %))))
        sora-wrapper-children (->> content
                                   (filter #(= sora-question-wrapper-label (:label %)))
                                   first
                                   :children)
        sora-terveys-question (get-in sora-wrapper-children [0 :id])
        sora-aiempi-question (get-in sora-wrapper-children [1 :id])
        kaksoistutkinto-questions (->> content
                                       (filter #(or (= lukio-kaksoistutkinto-wrapper-label (:label %))
                                                    (= amm-kaksoistutkinto-wrapper-label (:label %))))
                                       (map #(get-in % [:children 0 :id])))
        oppisopimuskoulutus-key (->> content
                                     (filter #(= kiinnostunut-oppisopimuskoulutuksesta-wrapper-label (:label %)))
                                     first
                                     :children
                                     first
                                     :id)
        urhelijian-ammatilliset-lisakysymykset-question (->> content
                                                             (filter #(= urheilijan-lisakysymykset-wrapper-label (:label %)))
                                                             first
                                                             :children
                                                             first)]
    {:tutkintovuosi-keys tutkintovuosi-keys
     :tutkintokieli-keys tutkintokieli-keys
     :sora-terveys-key sora-terveys-question
     :sora-aiempi-key sora-aiempi-question
     :kaksoistutkinto-keys kaksoistutkinto-questions
     :oppisopimuskoulutus-key (keyword oppisopimuskoulutus-key)
     :urheilijan-amm-lisakysymys-key (keyword (:id urhelijian-ammatilliset-lisakysymykset-question))
     :urheilijan-amm-groups (set (:belongs-to-hakukohderyhma urhelijian-ammatilliset-lisakysymykset-question))}))
