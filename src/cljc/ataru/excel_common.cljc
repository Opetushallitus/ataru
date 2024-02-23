(ns ataru.excel-common
  (:require [ataru.translations.texts :refer [excel-texts virkailija-texts]]))

(def hakemuksen-yleiset-tiedot-fields
  [{:id        "application-number"
    :label     (:application-number excel-texts)}
   {:id        "application-created-time"
    :label     (:sent-at excel-texts)}
   {:id        "application-state"
    :label     (:application-state excel-texts)}
   {:id        "student-number"
    :label     (:student-number excel-texts)}
   {:id        "applicant-oid"
    :label     (:applicant-oid excel-texts)}
   {:id        "turvakielto"
    :label     (:turvakielto excel-texts)}])

(def kasittelymerkinnat-fields
  [{:id        "hakukohde-handling-state"
    :label     (:hakukohde-handling-state excel-texts)}
   {:id        "kielitaitovaatimus"
    :label     (:kielitaitovaatimus excel-texts)}
   {:id        "tutkinnon-kelpoisuus"
    :label     (:tutkinnon-kelpoisuus excel-texts)}
   {:id        "hakukelpoisuus"
    :label     (:hakukelpoisuus excel-texts)}
   {:id        "eligibility-set-automatically"
    :label     (:eligibility-set-automatically virkailija-texts)}
   {:id        "ineligibility-reason"
    :label     (:ineligibility-reason virkailija-texts)}
   {:id        "maksuvelvollisuus"
    :label     (:maksuvelvollisuus excel-texts)}
   {:id        "valinnan-tila"
    :label     (:valinnan-tila excel-texts)}
   {:id        "ehdollinen"
    :label     (:ehdollinen excel-texts)}
   {:id        "pisteet"
    :label     (:pisteet excel-texts)}
   {:id        "application-review-notes"
    :label     (:notes excel-texts)}])