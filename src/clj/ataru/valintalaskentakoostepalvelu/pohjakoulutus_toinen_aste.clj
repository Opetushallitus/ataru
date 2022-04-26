(ns ataru.valintalaskentakoostepalvelu.pohjakoulutus-toinen-aste
  (:require [clojure.string :as string]))

(def lisapistekoulutus-mapping
  {:LISAKOULUTUS_KYMPPI             :lisapistekoulutus-perusopetuksenlisaopetus
   :LISAKOULUTUS_VALMA              :lisapistekoulutus-valma
   :LISAKOULUTUS_MAAHANMUUTTO_LUKIO :lisapistekoulutus-luva
   :LISAKOULUTUS_KANSANOPISTO       :lisapistekoulutus-kansanopisto
   :LISAKOULUTUS_OPISTOVUOSI        :lisapistekoulutus-opistovuosi})

(def oppiaine-lang-postix "_OPPIAINE")

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

(defn- get-arvosanat
  [get-koodi-label suoritus]
  (letfn [(get-oppiaine-lang [aine]
            (let [lang-key (keyword (str (name (:key aine)) oppiaine-lang-postix))]
              (get-koodi-label "kieli" 1 (get suoritus lang-key))))]
    (->> (keys suoritus)
         (filter #(string/includes? (str %) "PK_"))
         (map (fn [aine]
                {:key aine
                 :label (get-koodi-label "oppiaineetyleissivistava" 1 (last (str/split (str aine) #"PK_")))}))
         (filter #(not (nil? (:label %))))
         (map (fn [aine]
                (merge aine
                       {:value (get suoritus (:key aine))
                        :lang (get-oppiaine-lang aine)}))))))

(defn pohjakoulutus-for-application
  [get-koodi-label suoritus]
  (let [pohjakoulutus        (:POHJAKOULUTUS suoritus)
        opetuskieli          (string/upper-case (:perusopetuksen_kieli suoritus))
        suoritusvuosi        (:PK_SUORITUSVUOSI suoritus)
        lisapistekoulutukset (get-lisapistekoulutukset suoritus)
        arvosanat            (get-arvosanat get-koodi-label suoritus)]
    (prn arvosanat)
    (cond-> {}
      pohjakoulutus (assoc :pohjakoulutus {:value pohjakoulutus
                                           :label (get-koodi-label "2asteenpohjakoulutus2021" 1 pohjakoulutus)})
      opetuskieli (assoc :opetuskieli {:value opetuskieli
                                       :label (get-koodi-label "kieli" 1 opetuskieli)})
      suoritusvuosi (assoc :suoritusvuosi suoritusvuosi)
      lisapistekoulutukset (assoc :lisapistekoulutukset lisapistekoulutukset)
      arvosanat (assoc :arvosanat arvosanat))))
