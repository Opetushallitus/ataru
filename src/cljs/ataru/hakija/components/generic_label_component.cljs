(ns ataru.hakija.components.generic-label-component
  (:require [ataru.application-common.application-field-common :as application-field]
            [ataru.util :as util]
            [re-frame.core :as re-frame]))

(defn generic-label [_ _]
  (let [languages  (re-frame/subscribe [:application/default-languages])]
    (fn [field-descriptor idx]
      (let [label (util/non-blank-val (:label field-descriptor) @languages)]
        [:div.application__form-field-label
         {:id (application-field/id-for-label field-descriptor idx)}
         [:span (str label (application-field/required-hint field-descriptor))]
         [application-field/scroll-to-anchor field-descriptor]]))))
