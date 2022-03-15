(ns ataru.application.harkinnanvaraisuus-types)

(def harkinnanvaraisuus-reasons
  {:sure-yks-mat-ai                          "SURE_YKS_MAT_AI"
   :sure-ei-paattotodistusta                 "SURE_EI_PAATTOTODISTUSTA"
   :ataru-yks-mat-ai                         "ATARU_YKS_MAT_AI"
   :ataru-ulkomailla-opiskelu                "ATARU_ULKOMAILLA_OPISKELTU"
   :ataru-ei-paattotodistusta                "ATARU_EI_PAATTOTODISTUSTA"
   :ataru-sosiaaliset-syyt                   "ATARU_SOSIAALISET_SYYT"
   :ataru-oppimisvaikeudet                   "ATARU_OPPIMISVAIKEUDET"
   :ataru-koulutodistusten-vertailuvaikeudet "ATARU_KOULUTODISTUSTEN_VERTAILUVAIKEUDET"
   :ataru-riittamaton-tutkintokielen-taito   "ATARU_RIITTAMATON_TUTKINTOKIELEN_TAITO"
   :none                                     "EI_HARKINNANVARAINEN"})

(def harkinnanvaraisuus-types
  (vals harkinnanvaraisuus-reasons))