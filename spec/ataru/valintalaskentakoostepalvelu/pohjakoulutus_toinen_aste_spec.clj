(ns ataru.valintalaskentakoostepalvelu.pohjakoulutus-toinen-aste-spec
  (:require [speclj.core :refer [it describe tags should=]]
            [ataru.valintalaskentakoostepalvelu.pohjakoulutus-toinen-aste :refer [pohjakoulutus-for-application
                                                                                  oppiaine-lang-postfix
                                                                                  oppiaine-valinnainen-postfix]]
            [clojure.string :as string]))

(defn- dummy-get-koodi
  [_ _ koodi]
  (when (not (or
               (nil? koodi)
               (string/includes? koodi oppiaine-lang-postfix)
               (string/includes? koodi "SUORITUSVUOSI")
               (string/includes? koodi oppiaine-valinnainen-postfix)))
    koodi))

(def suoritus {:POHJAKOULUTUS "Perusopetus"
               :perusopetuksen_kieli "fi"
               :PK_SUORITUSVUOSI "2022"})

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
                          (should= 7 (last valinnaiset))))))
