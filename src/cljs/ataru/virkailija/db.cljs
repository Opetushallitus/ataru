(ns ataru.virkailija.db
  (:require
   [ataru.application.review-states :as review-states]))

(def default-attachment-review-states
  (into
   {}
   (map (fn [[state _]] [state false])
        review-states/attachment-hakukohde-review-types)))

(def default-filters {:language-requirement          {:unreviewed  true
                                                      :fulfilled   true
                                                      :unfulfilled true}
                      :degree-requirement            {:unreviewed  true
                                                      :fulfilled   true
                                                      :unfulfilled true}
                      :eligibility-state             {:unreviewed             true
                                                      :eligible               true
                                                      :uneligible             true
                                                      :conditionally-eligible true}
                      :eligibility-set-automatically {:yes true
                                                      :no  true}
                      :payment-obligation            {:unreviewed    true
                                                      :obligated     true
                                                      :not-obligated true}
                      :only-identified               {:identified   true
                                                      :unidentified true}
                      :only-ssn                      {:with-ssn    true
                                                      :without-ssn true}
                      :active-status                 {:active  true
                                                      :passive false}
                      :base-education                {:pohjakoulutus_kk_ulk                     true
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
                                                      :pohjakoulutus_yo_kansainvalinen_suomessa true}})

(def default-db
  {:editor                     {:forms               nil
                                :autosave            nil ; autosave stop function, see autosave.cljs
                                :selected-form-key   nil
                                :used-by-haut        {:fetching? false
                                                      :error?    false}
                                :email-template-lang "fi"
                                :organizations       {:org-select-organizations    true
                                                      :org-select-hakukohde-groups true
                                                      :results-page                0}}
   ; Initial active panel on page load.
   :active-panel               :editor
   :application                {:applications                      []
                                :applications-to-render            25
                                :review-state-counts               (into {} (map #(vector (first %) 0) review-states/application-hakukohde-processing-states))
                                :selection-state-counts            (into {} (map #(vector (first %) 0) review-states/application-hakukohde-selection-states))
                                :attachment-state-counts           (into {} (map #(vector (first %) 0) review-states/attachment-hakukohde-review-types-with-no-requirements))
                                :review                            {}
                                :attachment-state-filter           (set (mapv first review-states/attachment-hakukohde-review-types-with-no-requirements))
                                :processing-state-filter           (set (mapv first review-states/application-hakukohde-processing-states))
                                :selection-state-filter            (set (mapv first review-states/application-hakukohde-selection-states))
                                :attachment-review-states          {} ; TODO : Tämä nimi kannattaisi muuttaa kuvaamaan, että kyse on filtteröintiehdosta. Tai koota filterit omaan alidokumenttiinsa
                                :question-answer-filtering-options {}
                                :fetching-applications?            false
                                :sort                              {:order-by "applicant-name"
                                                                    :order    "asc"}
                                :selected-time-column              "created-time"
                                :application-list-expanded?        true
                                :mass-information-request          {:form-status :disabled}
                                :filters                           default-filters
                                :filters-checkboxes                default-filters
                                :ensisijaisesti?                   false
                                :ensisijaisesti?-checkbox          false
                                :rajaus-hakukohteella              nil
                                :rajaus-hakukohteella-value        nil}
   :haut                       {}
   :hakukohteet                {}
   :fetching-haut              0
   :fetching-hakukohteet       0
   :banner                     {:type :in-flow}
   :show-hakukierros-paattynyt false})
