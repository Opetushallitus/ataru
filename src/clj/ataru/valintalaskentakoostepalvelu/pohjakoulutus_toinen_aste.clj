(ns ataru.valintalaskentakoostepalvelu.pohjakoulutus-toinen-aste
  (:require [clojure.string :as string]))

(def lisapistekoulutus-mapping
  {:LISAKOULUTUS_KYMPPI             :lisapistekoulutus-perusopetuksenlisaopetus
   :LISAKOULUTUS_VALMA              :lisapistekoulutus-valma
   :LISAKOULUTUS_MAAHANMUUTTO_LUKIO :lisapistekoulutus-luva
   :LISAKOULUTUS_KANSANOPISTO       :lisapistekoulutus-kansanopisto
   :LISAKOULUTUS_OPISTOVUOSI        :lisapistekoulutus-opistovuosi})

(def oppiaine-lang-postfix "_OPPIAINE")
(def oppiaine-valinnainen-postfix "_VAL")
(def oppiaine-aidinkieli-prefix "PK_AI")

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
                {:key aine
                 :label (get-koodi-label "oppiaineetyleissivistava" 1 (last (string/split (str aine) #"PK_")))}))
         (filter #(not (nil? (:label %))))
         (map (fn [aine]
                (merge aine
                       {:value (get suoritus (:key aine))
                        :lang (get-oppiaine-lang aine)
                        :valinnaiset (get-valinnaiset-arvosanat suoritus (:key aine))}))))))

(defn pohjakoulutus-for-application
  [get-koodi-label suoritus]
  (let [pohjakoulutus        (:POHJAKOULUTUS suoritus)
        opetuskieli          (string/upper-case (:perusopetuksen_kieli suoritus))
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
