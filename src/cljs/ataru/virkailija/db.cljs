(ns ataru.virkailija.db
  (:require [ataru.application.review-states :refer [application-review-states]]))

(def default-db
  {:editor {:forms nil
            :autosave nil ; autosave stop function, see autosave.cljs
            :selected-form-key nil}
   ; Initial active panel on page load.
   :active-panel :editor
   :application {:review {}
                 :filter (mapv first application-review-states)}})
