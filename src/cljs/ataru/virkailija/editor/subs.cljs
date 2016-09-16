(ns ataru.virkailija.editor.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [register-sub]]))

(register-sub
  :editor/selected-form
  (fn [db _]
    (reaction
      (get-in @db [:editor :forms (get-in @db [:editor :selected-form-id])]))))

(register-sub
  :editor/languages
  (fn [db]
    (reaction
      (let [lang-path [:editor :forms (-> @db :editor :selected-form-id) :languages]
            languages (map keyword
                           (or (get-in @db lang-path) [:fi]))]
        languages))))
