(ns ataru.virkailija.db
  (:require
   [ataru.application.review-states :as review-states]
   [ataru.virkailija.application-sorting :as application-sorting]))

(def default-db
  {:editor       {:forms             nil
                  :autosave          nil                    ; autosave stop function, see autosave.cljs
                  :selected-form-key nil
                  :used-by-haut      {:fetching? false
                                      :error?    false}}
   ; Initial active panel on page load.
   :active-panel :editor
   :application  {:review                     {}
                  :processing-state-filter    (mapv first review-states/application-hakukohde-processing-states)
                  :selection-state-filter     (mapv first review-states/application-hakukohde-selection-states)
                  :sort                       application-sorting/initial-sort
                  :application-list-expanded? true}
   :banner       {:type :in-flow}})
