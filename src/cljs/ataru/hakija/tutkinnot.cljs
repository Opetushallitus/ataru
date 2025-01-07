(ns ataru.hakija.tutkinnot
  (:require [re-frame.core :as re-frame]
            [ataru.util :as util]
            [ataru.application-common.application-field-common :as common]))

;; TODO Muokkaa UI -speksin mukaiseksi
(defn tutkinnot [field-descriptor _]
  (let [languages  (re-frame/subscribe [:application/default-languages])
        lang  @(re-frame/subscribe [:application/form-language])
        label (util/non-blank-val (:label field-descriptor) @languages)
        header (util/non-blank-val (:text field-descriptor) @languages)]
  [:div.application__wrapper-element
   [:div.application__wrapper-heading
    [:h2 label]
    [common/scroll-to-anchor field-descriptor]]
   [:div.application__wrapper-contents
    [:div.application__form-field
      [:span.application__form-field-label header
       [:span.application__form-field-label.application__form-field-label--required
        (common/required-hint field-descriptor lang)]]
     [:ul
      (doall
        (for [tutkinto @(re-frame/subscribe [:application/tutkinnot])]
          ^{:key (:key tutkinto)}
          [:li (get-in tutkinto [:tutkintonimi lang])]))]

     ]]]))

