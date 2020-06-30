(ns ataru.hakija.components.form-field-label-component
  (:require [ataru.application-common.application-field-common :as application-field]
            [ataru.util :as util]
            [re-frame.core :as re-frame]))

(defn form-field-label [_ _]
  (let [languages  (re-frame/subscribe [:application/default-languages])]
    (fn [field-descriptor idx]
      (let [label (util/non-blank-val (:label field-descriptor) @languages)]
        [:label.application__form-field-label
         {:for (application-field/form-field-id field-descriptor idx)}
         [:span (str label (application-field/required-hint field-descriptor))]
         [application-field/scroll-to-anchor field-descriptor]]))))