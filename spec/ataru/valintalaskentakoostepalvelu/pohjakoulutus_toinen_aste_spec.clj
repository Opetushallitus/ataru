(ns ataru.valintalaskentakoostepalvelu.pohjakoulutus-toinen-aste-spec
  (:require [speclj.core :refer [it describe tags should=]]
            [ataru.valintalaskentakoostepalvelu.pohjakoulutus-toinen-aste :refer [pohjakoulutus-for-application
                                                                                  oppiaine-lang-postfix
                                                                                  oppiaine-valinnainen-postfix]]
            [clojure.string :as string]))

;; Nämä oppiainekoodit ovat vain koskioppiaineetyleissivistava-koodistossa, eivät oppiaineetyleissivistava-koodistossa.
(def ^:private koski-only-oppiaineet #{"AOM" "OP" "OPA" "YL" "ET"})

(defn- dummy-get-koodi
  [uri _ koodi]
  (when (not (or
               (nil? koodi)
               (string/includes? koodi oppiaine-lang-postfix)
               (string/includes? koodi "SUORITUSVUOSI")
               (string/includes? koodi oppiaine-valinnainen-postfix)
               (and (= uri "oppiaineetyleissivistava")
                    (contains? koski-only-oppiaineet koodi))))
    koodi))

(def suoritus {:POHJAKOULUTUS "Perusopetus"
               :perusopetuksen_kieli "fi"
               :PK_SUORITUSVUOSI "2022"})

(def suoritus-without-kieli (dissoc suoritus :perusopetuksen_kieli))

(describe "pohjakoulutus-toinen-aste"
          (tags :unit)
          (describe "pohjakoulutus-for-application"
                    (it "returns pohjakoulutus, opetuskieli, suoritusvuosi"
                        (let [result (pohjakoulutus-for-application dummy-get-koodi suoritus)]
                          (should= "Perusopetus" (get-in result [:pohjakoulutus :value]))
                          (should= "Perusopetus" (get-in result [:pohjakoulutus :label]))
                          (should= "FI" (get-in result [:opetuskieli :value]))
                          (should= "FI" (get-in result [:opetuskieli :label]))
                          (should= "2022" (get-in result [:suoritusvuosi]))))

                    (it "returns arvosanat"
                        (let [grades {:PK_AI 6 :PK_AI_OPPIAINE "en" :PK_MA 8}
                              result (pohjakoulutus-for-application dummy-get-koodi (merge suoritus grades))
                              arvosanat (vec (:arvosanat result))]
                          (should= 2 (count arvosanat))
                          (should= 6 (get-in arvosanat [0 :value]))
                          (should= "AI" (get-in arvosanat [0 :label]))
                          (should= :PK_AI (get-in arvosanat [0 :key]))
                          (should= 8 (get-in arvosanat [1 :value]))
                          (should= "MA" (get-in arvosanat [1 :label]))
                          (should= :PK_MA (get-in arvosanat [1 :key]))))

                    (it "returns äidinkielenomainen kieli (AOM) from koskioppiaineetyleissivistava when it is missing from oppiaineetyleissivistava"
                        (let [grades {:PK_AOM 8 :PK_AOM_OPPIAINE "pl" :PK_MA 9}
                              result (pohjakoulutus-for-application dummy-get-koodi (merge suoritus grades))
                              arvosanat (vec (:arvosanat result))
                              aom (first (filter #(= :PK_AOM (:key %)) arvosanat))]
                          (should= 2 (count arvosanat))
                          (should= 8 (:value aom))
                          (should= "AOM" (:label aom))
                          (should= "pl" (:lang aom))))

                    (it "returns arvosanat with valinnaiset"
                        (let [grades {:PK_FY 9 :PK_FY_VAL1 8 :PK_FY_VAL2 10 :PK_FY_VAL3 7}
                              result (pohjakoulutus-for-application dummy-get-koodi (merge suoritus grades))
                              arvosanat (vec (:arvosanat result))
                              valinnaiset (get-in arvosanat [0 :valinnaiset])]
                          (should= 1 (count arvosanat))
                          (should= 9 (get-in arvosanat [0 :value]))
                          (should= 3 (count valinnaiset))
                          (should= 8 (first valinnaiset))
                          (should= 10 (second valinnaiset))
                          (should= 7 (last valinnaiset))))

                    (it "returns arvosanat even without kieli"
                        (let [grades {:PK_FY 9 :PK_FY_VAL1 8 :PK_FY_VAL2 10 :PK_FY_VAL3 7}
                              result (pohjakoulutus-for-application dummy-get-koodi (merge suoritus-without-kieli grades))
                              arvosanat (vec (:arvosanat result))
                              valinnaiset (get-in arvosanat [0 :valinnaiset])
                              kieli (get-in result [:opetuskieli :value])]
                          (should= nil kieli)
                          (should= 1 (count arvosanat))
                          (should= 9 (get-in arvosanat [0 :value]))
                          (should= 3 (count valinnaiset))
                          (should= 8 (first valinnaiset))
                          (should= 10 (second valinnaiset))
                          (should= 7 (last valinnaiset))))))
