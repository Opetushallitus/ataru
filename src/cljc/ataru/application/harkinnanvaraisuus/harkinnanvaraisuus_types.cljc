(ns ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types)

(def sure-yks-mat-ai "SURE_YKS_MAT_AI")
(def ataru-yks-mat-ai "ATARU_YKS_MAT_AI")
(def sure-ei-paattotodistusta "SURE_EI_PAATTOTODISTUSTA")
(def ataru-ulkomailla-opiskelu "ATARU_ULKOMAILLA_OPISKELTU")
(def ataru-ei-paattotodistusta "ATARU_EI_PAATTOTODISTUSTA")
(def ei-harkinnanvarainen "EI_HARKINNANVARAINEN")

(def harkinnanvaraisuus-reasons
  {:sure-yks-mat-ai                          sure-yks-mat-ai
   :sure-ei-paattotodistusta                 sure-ei-paattotodistusta
   :ataru-yks-mat-ai                         ataru-yks-mat-ai
   :ataru-ulkomailla-opiskelu                ataru-ulkomailla-opiskelu
   :ataru-ei-paattotodistusta                ataru-ei-paattotodistusta
   :ataru-sosiaaliset-syyt                   "ATARU_SOSIAALISET_SYYT"
   :ataru-oppimisvaikeudet                   "ATARU_OPPIMISVAIKEUDET"
   :ataru-koulutodistusten-vertailuvaikeudet "ATARU_KOULUTODISTUSTEN_VERTAILUVAIKEUDET"
   :ataru-riittamaton-tutkintokielen-taito   "ATARU_RIITTAMATON_TUTKINTOKIELEN_TAITO"
   :ei-harkinnanvarainen-hakukohde           "EI_HARKINNANVARAINEN_HAKUKOHDE"
   :none                                     ei-harkinnanvarainen})

(def harkinnanvaraisuus-types
  (vals harkinnanvaraisuus-reasons))

(def harkinnanvaraisuus-yksilollistetty-matikka-aikka-types
  [sure-yks-mat-ai ataru-yks-mat-ai])

(def pohjakoulutus-harkinnanvarainen-types
  (concat harkinnanvaraisuus-yksilollistetty-matikka-aikka-types
          [sure-ei-paattotodistusta ataru-ulkomailla-opiskelu ataru-ei-paattotodistusta]))

(defn harkinnanvarainen?
  [reason]
  (and
    (some? reason)
    (not (#{(:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons)
            (:none harkinnanvaraisuus-reasons)}
          reason))))