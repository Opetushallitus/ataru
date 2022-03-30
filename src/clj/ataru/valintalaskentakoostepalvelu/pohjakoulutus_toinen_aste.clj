(ns ataru.valintalaskentakoostepalvelu.pohjakoulutus-toinen-aste)

(def lisapistekoulutus-mapping
  {:LISAKOULUTUS_KYMPPI             :lisapistekoulutus-perusopetuksenlisaopetus
   :LISAKOULUTUS_VALMA              :lisapistekoulutus-valma
   :LISAKOULUTUS_MAAHANMUUTTO_LUKIO :lisapistekoulutus-luva
   :LISAKOULUTUS_KANSANOPISTO       :lisapistekoulutus-kansanopisto
   :LISAKOULUTUS_OPISTOVUOSI        :lisapistekoulutus-opistovuosi})

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
    []
    lisapistekoulutus-mapping))

(defn pohjakoulutus-for-application
  [get-suoritus get-koodi-label haku-oid application-key]
  (let [suoritus             (get-suoritus haku-oid application-key)
        pohjakoulutus        (:POHJAKOULUTUS suoritus)
        opetuskieli          (:perusopetuksen_kieli suoritus)
        suoritusvuosi        (:PK_SUORITUSVUOSI suoritus)
        lisapistekoulutukset (get-lisapistekoulutukset suoritus)]
    (cond-> {:suoritus suoritus}
      pohjakoulutus (assoc :pohjakoulutus {:value pohjakoulutus
                                           :label (get-koodi-label "2asteenpohjakoulutus2021" 1 pohjakoulutus)})
      opetuskieli (assoc :opetuskieli {:value opetuskieli
                                       :label (get-koodi-label "kieli" 1 opetuskieli)})
      suoritusvuosi (assoc :suoritusvuosi suoritusvuosi)
      lisapistekoulutukset (assoc :lisapistekoulutukset lisapistekoulutukset))))
