(ns ataru.application.harkinnanvaraisuus-types)

(def harkinnanvaraisuus-reasons
  {:sure-yks-mat-ai "SURE_YKS_MAT_AI"
   :sure-ei-paattotodistusta "SURE_EI_PAATTOTODISTUSTA"
   :ataru-yks-mat-ai "ATARU_YKS_MAT_AI"
   :ataru-ulkomailla-opiskelu "ATARU_ULKOMAILLA_OPISKELTU"
   :ataru-ei-paattotodistusta "ATARU_EI_PAATTOTODISTUSTA"
   :ataru-social-reasons "ATARU_SOCIAL_REASONS"
   :ataru-study-challenges "ATARU_STUDY_CHALLENGES"
   :ataru-certificate-comparison-difficulties "ATARU_CERTIFICATE_COMPARISON_DIFFICULTIES"
   :ataru-insufficient-language-skill "ATARU_INSUFFICIENT_LANGUAGE_SKILL"
   :none "NONE"})

(def harkinnanvaraisuus-types
  (vals harkinnanvaraisuus-reasons))