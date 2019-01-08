(ns ataru.virkailija.db
  (:require
   [ataru.application.review-states :as review-states]))

(def default-db
  {:editor               {:forms               nil
                          :autosave            nil                  ; autosave stop function, see autosave.cljs
                          :selected-form-key   nil
                          :used-by-haut        {:fetching? false
                                                :error?    false}
                          :email-template-lang "fi"}
   ; Initial active panel on page load.
   :active-panel         :editor
   :application          {:applications               []
                          :review                     {}
                          :attachment-state-filter    (set (mapv first review-states/attachment-hakukohde-review-types-with-no-requirements))
                          :processing-state-filter    (set (mapv first review-states/application-hakukohde-processing-states))
                          :selection-state-filter     (set (mapv first review-states/application-hakukohde-selection-states))
                          :sort                       {:column :created-time :order :descending}
                          :selected-time-column       :created-time
                          :application-list-expanded? true
                          :mass-information-request   {:form-status :disabled}
                          :application-list-page      0
                          :filters                    {:language-requirement {:unreviewed  true
                                                                              :fulfilled   true
                                                                              :unfulfilled true}
                                                       :degree-requirement   {:unreviewed  true
                                                                              :fulfilled   true
                                                                              :unfulfilled true}
                                                       :eligibility-state    {:unreviewed             true
                                                                              :eligible               true
                                                                              :uneligible             true
                                                                              :conditionally-eligible true}
                                                       :payment-obligation   {:unreviewed    true
                                                                              :obligated     true
                                                                              :not-obligated true}
                                                       :only-identified      {:identified   true
                                                                              :unidentified true}
                                                       :only-ssn             {:with-ssn    true
                                                                              :without-ssn true}
                                                       :active-status        {:active  true
                                                                              :passive false}
                                                       :base-education       {:pohjakoulutus_kk_ulk                     true
                                                                              :pohjakoulutus_lk                         true
                                                                              :pohjakoulutus_kk                         true
                                                                              :pohjakoulutus_amt                        true
                                                                              :pohjakoulutus_ulk                        true
                                                                              :pohjakoulutus_muu                        true
                                                                              :pohjakoulutus_avoin                      true
                                                                              :pohjakoulutus_yo_ammatillinen            true
                                                                              :pohjakoulutus_am                         true
                                                                              :pohjakoulutus_yo_ulkomainen              true
                                                                              :pohjakoulutus_yo                         true
                                                                              :pohjakoulutus_yo_kansainvalinen_suomessa true}}}
   :haut                 {}
   :hakukohteet          {}
   :fetching-haut        0
   :fetching-hakukohteet 0
   :banner               {:type :in-flow}})
