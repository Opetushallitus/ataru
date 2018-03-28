(ns ataru.virkailija.db
  (:require
   [ataru.application.review-states :as review-states]
   [ataru.virkailija.application-sorting :as application-sorting]))

(def default-db
  {:editor               {:forms               nil
                          :autosave            nil          ; autosave stop function, see autosave.cljs
                          :selected-form-key   nil
                          :used-by-haut        {:fetching? false
                                                :error?    false}
                          :email-template-lang "fi"}
   ; Initial active panel on page load.
   :active-panel         :editor
   :application          {:review                     {}
                          :attachment-state-filter    (mapv first review-states/attachment-hakukohde-review-types)
                          :processing-state-filter    (mapv first review-states/application-hakukohde-processing-states)
                          :selection-state-filter     (mapv first review-states/application-hakukohde-selection-states)
                          :sort                       application-sorting/initial-sort
                          :application-list-expanded? true}
   :haut                 {}
   :hakukohteet          {}
   :fetching-haut        0
   :fetching-hakukohteet 0
   :banner               {:type :in-flow}}
  )
