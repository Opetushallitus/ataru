(ns ataru.applications.question-util
  (:require [ataru.translations.texts :refer [base-education-2nd-module-texts]]
            [clojure.walk :refer [keywordize-keys]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-choice-key base-education-wrapper-key base-education-2nd-language-value-to-lang]]))

(def sora-question-wrapper-label {:fi "Terveydelliset tekijät " :sv "Hälsoskäl"})


(def urheilijan-lisakysymykset-lukiokohteisiin-wrapper-key "8466feca-1993-4af3-b82c-59003ca2fd63")

(def urheilija-muu-laji-label {:fi "Muu, mikä?", :sv "Annan, vad?"})
(def urheilija-paalaji-folloup-label {:fi "Päälaji", :sv "Huvudgren"})

(def lukio-kaksoistutkinto-wrapper-label {:fi "Ammatilliset opinnot lukiokoulutuksen ohella",
                                          :sv "Yrkesinriktade studier vid sidan av gymnasieutbildningen"})

(def amm-kaksoistutkinto-wrapper-label {:fi "Lukio-opinnot ammatillisen koulutuksen ohella",
                                        :sv "Gymnasiestudier vid sidan av den yrkesinriktade utbildningen"})

(def kiinnostunut-oppisopimuskoulutuksesta-wrapper-label {:fi "Oppisopimuskoulutus ",
                                                          :sv "Läroavtalsutbildning"})

(def urheilijan-lisakysymykset-wrapper-label {:fi "Urheilijan lisäkysymykset ammatillisissa kohteissa",
                                              :sv "Tilläggsfrågor för idrottare i yrkesinriktade ansökningsmål"})

;fixme, kysymys-id:t kohdilleen.
(defn- urheilijan-lisakysymys-keys [haku-oid]
  (case haku-oid
    "1.2.246.562.29.00000000000000005368" {:keskiarvo                   "7b88594a-c308-41f8-bac3-2d3779ea4443"
                                           :peruskoulu                  "9a4de985-9a70-4de6-bfa7-0a5c2f18cb8c"
                                           :tamakausi                   "f944c9c3-c1f8-43c7-a27e-49d89d4e8eec"
                                           :viimekausi                  "e3e8b5ef-f8d9-4256-8ef6-1a52d562a370"
                                           :toissakausi                 "95b565ee-f64e-4805-b319-55b99bbce1a8"
                                           :sivulaji                    "dbfc1215-896a-47d4-bc07-b9f1494658f4"
                                           :valmentaja_nimi             "a1f1147a-d466-4d98-9a62-079a42dd4089"
                                           :valmentaja_email            "625fe96d-a5ff-4b3a-8ace-e36524215d1c"
                                           :valmentaja_puh              "f1c5986c-bea8-44f7-8324-d1cac179e6f4"
                                           :valmennusryhma_seurajoukkue "92d579fb-dafa-4edc-9e05-8f493badc4f3"
                                           :valmennusryhma_piirijoukkue "58125631-762a-499b-a402-717778bf8233"
                                           :valmennusryhma_maajoukkue   "261d7ffc-54a7-4c5c-ab80-82f7de49f648"
                                           :paalajiSeuraLiittoParent    "98951abd-fdd5-46a0-8427-78fe9706d286"}
    {:keskiarvo                   "urheilija-2nd-keskiarvo"
     :peruskoulu                  "urheilija-2nd-peruskoulu"
     :tamakausi                   "urheilija-2nd-tamakausi"
     :viimekausi                  "urheilija-2nd-viimekausi"
     :toissakausi                 "urheilija-2nd-toissakausi"
     :sivulaji                    "urheilija-2nd-sivulaji"
     :valmentaja_nimi             "urheilija-2nd-valmentaja-nimi"
     :valmentaja_email            "urheilija-2nd-valmentaja-email"
     :valmentaja_puh              "urheilija-2nd-valmentaja-puh"
     :valmennusryhma_seurajoukkue "urheilija-2nd-valmennus-seurajoukkue"
     :valmennusryhma_piirijoukkue "urheilija-2nd-valmennus-piirijoukkue"
     :valmennusryhma_maajoukkue   "urheilija-2nd-valmennus-maajoukkue"
     :valmennusryhmatParent       "84cd8829-ee39-437f-b730-9d68f0f07555"
     :paalaji-dropdown            "urheilija-2nd-lajivalinta-dropdown"
     :seura                       "urheilija-2nd-seura"
     :liitto                      "urheilija-2nd-liitto"}))

(defn- urheilijan-ammatilliset-lisakysymys-keys [haku-oid]
  (case haku-oid
    "1.2.246.562.29.00000000000000005368" {:peruskoulu                  "22e8cc0a-ef4b-4f47-b0e3-a34bb1c3c07d"
                                           :tamakausi                   "a9a32f30-86b4-4e41-a6d4-4a6863a086ab"
                                           :viimekausi                  "6822dcb8-86b7-400b-a92f-4d02be6b7063"
                                           :toissakausi                 "0278ea3f-e6c1-41c3-a9cc-b3be8acd493d"
                                           :sivulaji                    "25c3adca-8a4f-41f2-91d2-7c787a47d166"
                                           :valmentaja_nimi             "548e2d07-ac5d-49c5-a744-9bc49550c742"
                                           :valmentaja_email            "83a487b1-3485-4763-a996-82a9640d1812"
                                           :valmentaja_puh              "346ac362-4f75-4225-a7ba-3ddcb9b0498d"
                                           :valmennusryhma_seurajoukkue "fc52158d-0d80-42d5-b16b-852e0e50e4d6"
                                           :valmennusryhma_piirijoukkue "6437bc3e-e554-4b9e-982d-4622fc77be50"
                                           :valmennusryhma_maajoukkue   "01a4a834-0128-4147-a890-6e2932c915d6"
                                           :paalajiSeuraLiittoParent    "09257557-0bbf-4e94-a19b-44b561817eda"}
    "1.2.246.562.29.00000000000000021303" {:peruskoulu                  "22e8cc0a-ef4b-4f47-b0e3-a34bb1c3c07d"
                                           :tamakausi                   "a9a32f30-86b4-4e41-a6d4-4a6863a086ab"
                                           :viimekausi                  "6822dcb8-86b7-400b-a92f-4d02be6b7063"
                                           :toissakausi                 "0278ea3f-e6c1-41c3-a9cc-b3be8acd493d"
                                           :sivulaji                    "25c3adca-8a4f-41f2-91d2-7c787a47d166"
                                           :valmentaja_nimi             "548e2d07-ac5d-49c5-a744-9bc49550c742"
                                           :valmentaja_email            "83a487b1-3485-4763-a996-82a9640d1812"
                                           :valmentaja_puh              "346ac362-4f75-4225-a7ba-3ddcb9b0498d"
                                           :valmennusryhma_seurajoukkue "fc52158d-0d80-42d5-b16b-852e0e50e4d6"
                                           :valmennusryhma_piirijoukkue "6437bc3e-e554-4b9e-982d-4622fc77be50"
                                           :valmennusryhma_maajoukkue   "01a4a834-0128-4147-a890-6e2932c915d6"
                                           :paalaji-dropdown            "09257557-0bbf-4e94-a19b-44b561817eda"
                                           :seura                       "06900eee-7949-445d-ac4f-e8738a904185"
                                           :liitto                      "2b2ede36-e520-4727-8151-93115d26ef7f"}
    {:peruskoulu                  "urheilija-2nd-amm-peruskoulu"
     :tamakausi                   "urheilija-2nd-amm-tamakausi"
     :viimekausi                  "urheilija-2nd-amm-viimekausi"
     :toissakausi                 "urheilija-2nd-amm-toissakausi"
     :sivulaji                    "urheilija-2nd-amm-sivulaji"
     :valmentaja_nimi             "urheilija-2nd-amm-valmentaja-nimi"
     :valmentaja_email            "urheilija-2nd-amm-valmentaja-email"
     :valmentaja_puh              "urheilija-2nd-amm-valmentaja-puh"
     :valmennusryhma_seurajoukkue "urheilija-2nd-amm-valmennus-seurajoukkue"
     :valmennusryhma_piirijoukkue "urheilija-2nd-amm-valmennus-piirijoukkue"
     :valmennusryhma_maajoukkue   "urheilija-2nd-amm-valmennus-maajoukkue"
     :paalaji-dropdown            "urheilija-2nd-amm-lajivalinta-dropdown"
     :seura                       "urheilija-2nd-amm-seura"
     :liitto                      "urheilija-2nd-amm-liitto"}))

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
  ([form] (get-hakurekisteri-toinenaste-specific-questions form "unknown haku"))
  ([form haku-oid]
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
                                                              first)
         urheilija-keys (urheilijan-lisakysymys-keys haku-oid)
         urheilija-amm-keys (urheilijan-ammatilliset-lisakysymys-keys haku-oid)
         laji-options (->> content
                           (filter #(= urheilijan-lisakysymykset-lukiokohteisiin-wrapper-key (:id %)))
                           first
                           :children
                           (filter #(= (:paalaji-dropdown urheilija-keys) (:id %)))
                           first
                           :options)
         laji-value-to-label (into {} (map (fn [laji] {(:value laji) (:label laji)}) laji-options))
         muu-laji-key (->> laji-options
                           (filter #(= urheilija-muu-laji-label (:label %)))
                           first
                           :followups
                           (filter #(= urheilija-paalaji-folloup-label (:label %)))
                           first
                           :id)]
     {:tutkintovuosi-keys                          tutkintovuosi-keys
      :tutkintokieli-keys                          tutkintokieli-keys
      :sora-terveys-key                            sora-terveys-question
      :sora-aiempi-key                             sora-aiempi-question
      :kaksoistutkinto-keys                        kaksoistutkinto-questions
      :oppisopimuskoulutus-key                     (keyword oppisopimuskoulutus-key)
      :urheilijan-amm-lisakysymys-key              (keyword (:id urhelijian-ammatilliset-lisakysymykset-question))
      :urheilijan-amm-groups                       (set (:belongs-to-hakukohderyhma urhelijian-ammatilliset-lisakysymykset-question))
      :urheilijan-lisakysymys-keys                 urheilija-keys
      :urheilijan-lisakysymys-laji-key-and-mapping {:laji-dropdown-key (keyword (:paalaji-dropdown urheilija-keys))
                                                    :muu-laji-key      (keyword muu-laji-key)
                                                    :value-to-label    laji-value-to-label}
      :urheilijan-amm-lisakysymys-keys             urheilija-amm-keys})))
