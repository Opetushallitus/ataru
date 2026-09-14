(ns ataru.valintalaskentakoostepalvelu.pohjakoulutus-toinen-aste
  (:require [clojure.string :as string]))

(def lisapistekoulutus-mapping
  {:LISAKOULUTUS_KYMPPI             :lisapistekoulutus-perusopetuksenlisaopetus
   :LISAKOULUTUS_VALMA              :lisapistekoulutus-valma
   :LISAKOULUTUS_MAAHANMUUTTO_LUKIO :lisapistekoulutus-luva
   :LISAKOULUTUS_KANSANOPISTO       :lisapistekoulutus-kansanopisto
   :LISAKOULUTUS_OPISTOVUOSI        :lisapistekoulutus-opistovuosi
   :LISAKOULUTUS_TUVA               :lisapistekoulutus-tuva})

(def oppiaine-lang-postfix "_OPPIAINE")
(def oppiaine-valinnainen-postfix "_VAL")
(def oppiaine-aidinkieli-prefix "PK_AI")

;; Oppiaineen label haetaan kahdesta koodistosta, koska kumpikin sisältää koodeja joita toisessa ei ole:
;; - "oppiaineetyleissivistava" on ensisijainen ja sitä on laajennettu tätä näkymää varten numeroiduilla
;;   kielivarianteilla (A12, A22, B22, B23, B32, B33), joita koski-koodistossa ei ole.
;; - "koskioppiaineetyleissivistava" on toissijainen. Suorituspalvelu tunnistaa aineet sen perusteella,
;;   ja siellä on mm. AOM (äidinkielenomainen kieli), OP, OPA, YL ja ET, jotka puuttuvat ensisijaisesta.
;; Ilman toissijaista näiden aineiden rivit katoaisivat Arvosanat-välilehdeltä, koska get-arvosanat
;; pudottaa rivin jolle kumpikaan koodisto ei anna labelia.
(def oppiaine-label-koodistot ["oppiaineetyleissivistava" "koskioppiaineetyleissivistava"])

(defn- suoritus-value-true?
  [suoritus key]
  (= "true" (key suoritus)))

(defn- get-lisapistekoulutukset
  [suoritus]
  (reduce
    (fn [acc [valintalaskenta-key ataru-key]]
      (if (suoritus-value-true? suoritus valintalaskenta-key)
        (conj acc ataru-key)
        acc))
    nil
    lisapistekoulutus-mapping))

(defn- get-valinnaiset-arvosanat
  [suoritus aine-key]
  (->> (keys suoritus)
       (map name)
       (filter #(string/includes? % (str (name aine-key) oppiaine-valinnainen-postfix)))
       (sort)
       (map #(get suoritus (keyword %))))
  )

(defn- get-arvosanat
  [get-koodi-label suoritus]
  (letfn [(get-oppiaine-lang [aine]
            (let [lang-key (keyword (str (name (:key aine)) oppiaine-lang-postfix))]
              (if (string/includes? (name (:key aine)) oppiaine-aidinkieli-prefix)
                (get-koodi-label "aidinkielijakirjallisuus" 1 (get suoritus lang-key))
                (get-koodi-label "kielivalikoima" 1 (get suoritus lang-key)))))]
    (->> (keys suoritus)
         (filter #(string/includes? (str %) "PK_"))
         (map (fn [aine]
                (let [koodi (last (string/split (str aine) #"PK_"))]
                  {:key   aine
                   :label (some #(get-koodi-label % 1 koodi) oppiaine-label-koodistot)})))
         (filter #(not (nil? (:label %))))
         (map (fn [aine]
                (merge aine
                       {:value (get suoritus (:key aine))
                        :lang (get-oppiaine-lang aine)
                        :valinnaiset (get-valinnaiset-arvosanat suoritus (:key aine))}))))))

(defn pohjakoulutus-for-application
  [get-koodi-label suoritus]
  (let [pohjakoulutus        (:POHJAKOULUTUS suoritus)
        opetuskieli          (some-> (:perusopetuksen_kieli suoritus)
                                     (string/upper-case))
        suoritusvuosi        (:PK_SUORITUSVUOSI suoritus)
        lisapistekoulutukset (get-lisapistekoulutukset suoritus)
        arvosanat            (get-arvosanat get-koodi-label suoritus)]
    (cond-> {}
      pohjakoulutus (assoc :pohjakoulutus {:value pohjakoulutus
                                           :label (get-koodi-label "2asteenpohjakoulutus2021" 1 pohjakoulutus)})
      opetuskieli (assoc :opetuskieli {:value opetuskieli
                                       :label (get-koodi-label "kieli" 1 opetuskieli)})
      suoritusvuosi (assoc :suoritusvuosi suoritusvuosi)
      lisapistekoulutukset (assoc :lisapistekoulutukset lisapistekoulutukset)
      arvosanat (assoc :arvosanat arvosanat))))
