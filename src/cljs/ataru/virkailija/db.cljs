(ns ataru.virkailija.db)

(def default-db
  {:editor {:forms nil
            :autosave nil ; autosave stop function, see autosave.cljs
            :selected-form-id nil}
   ; Initial active panel on page load.
   :active-panel :editor
   :application {:review {}}})
