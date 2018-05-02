(ns ataru.virkailija.db
  (:require
   [ataru.application.review-states :as review-states]
   [ataru.virkailija.application-sorting :as application-sorting]))

(def default-db
  {:editor               {:forms               nil
                          :autosave            nil                  ; autosave stop function, see autosave.cljs
                          :selected-form-key   nil
                          :used-by-haut        {:fetching? false
                                                :error?    false}
                          :email-template-lang "fi"}
   ; Initial active panel on page load.
   :active-panel         :editor
   :application          {:review                     {}
                          :attachment-state-filter    (mapv first review-states/attachment-hakukohde-review-types) ; TODO
                          :processing-state-filter    (mapv first review-states/application-hakukohde-processing-states) ; TODO
                          :selection-state-filter     (mapv first review-states/application-hakukohde-selection-states) ; TODO
                          :sort                       application-sorting/initial-sort
                          :application-list-expanded? true
                          :filters                    {:language-requirement {:unreviewed  true
                                                                              :fulfilled   true
                                                                              :unfulfilled true}
                                                       :degree-requirement   {:unreviewed  true
                                                                              :fulfilled   true
                                                                              :unfulfilled true}
                                                       :eligibility-state    {:unreviewed true
                                                                              :eligible   true
                                                                              :uneligible true}
                                                       :payment-obligation   {:unreviewed    true
                                                                              :obligated     true
                                                                              :not-obligated true}
                                                       :only-identified      {:identified   true
                                                                              :unidentified true}}}
   :haut                 {}
   :hakukohteet          {}
   :fetching-haut        0
   :fetching-hakukohteet 0
   :banner               {:type :in-flow}})
