(ns ataru.hakija.components.label-component
  (:require [ataru.application-common.application-field-common :as application-field]
            [ataru.util :as util]
            [re-frame.core :as re-frame]))

(defn label [field-descriptor]
  (let [languages  (re-frame/subscribe [:application/default-languages])
        label-meta (if-let [label-id (application-field/id-for-label field-descriptor)]
                     {:id label-id}
                     {:for (:id field-descriptor)})]
    (fn [field-descriptor]
      (let [label (util/non-blank-val (:label field-descriptor) @languages)]
        [:label.application__form-field-label
         label-meta
         [:span (str label (application-field/required-hint field-descriptor))]
         [application-field/scroll-to-anchor field-descriptor]]))))
