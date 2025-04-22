(ns ataru.hakija.components.tutkinnot
  (:require [ataru.component-data.koski-tutkinnot-module :as ktm]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]
            [ataru.util :as util]
            [clojure.string :refer [join]]
            [re-frame.core :refer [dispatch subscribe]]
            [ataru.translations.translation-util :as tu]
            [ataru.application-common.application-field-common :refer [scroll-to-anchor]]))

(defn tutkinto-group [label field-descriptor idx can-remove lang child-components]
  [:div.application__tutkinto-group-container
   [:div.application__tutkinto-header
    label
    (when can-remove
      [:div.application__tutkinto-header.remove-tutkinto
       [:a.application__tutkinto-header.remove-tutkinto.link
        {:on-click (fn add-question-group-row [event]
                     (.preventDefault event)
                     (dispatch [:application/remove-question-group-row
                                field-descriptor
                                idx]))}
        [:i.zmdi.zmdi-delete.application__tutkinto-button-icon]
        [:span.application__tutkinto-button-text (tu/get-hakija-translation :poista lang)]]])]
   [:div.application__form-multi-choice-followups-outer-container
    {:tab-index 0}
    [:div.application__form-multi-choice-followups-indicator]
    (into [:div.application__tutkinto-entity-container] child-components)]])

(defn add-button [on-click lang]
  [:div.application__show-additional-tutkinnot
   [:button.application__show-additional-tutkinnot.button
    {:on-click on-click}
    [:i.zmdi.zmdi-plus.application__tutkinto-button-icon]
    [:span.application__show-additional-tutkinnot.button-text (tu/get-hakija-translation :add-tutkinto lang)]]])

(defn add-tutkinto-button [field-descriptor lang]
  (add-button (fn [event]
                (.preventDefault event)
                (dispatch [:application/add-question-group-row field-descriptor]))
              lang))

(defn hide-additional-tutkinnot-button [on-click lang]
  [:div.application__hide-additional-tutkinnot
   [:a.application__hide-additional-tutkinnot.link
    {:on-click on-click}
    [:i.zmdi.zmdi-delete.application__tutkinto-button-icon]
    [:span.application__tutkinto-button-text (tu/get-hakija-translation :poista-osio lang)]]])

(defn fixed-tutkinto-item [tutkinto _ _]
  (let [lang @(subscribe [:application/form-language])
        localized-val (fn [field] (get-in tutkinto [field (keyword lang)]))
        upper-row (join ", "
                        (filterv #(some? %) [(localized-val :tutkintonimi) (localized-val :koulutusohjelmanimi)
                                             (:valmistumispvm tutkinto)]))
        lower-row (localized-val :toimipistenimi)]
    (fn [_ id checked?]
      (let [set-checked-as-needed (fn [] (if checked?
                                           {:class " checked-koski-tutkinto"}
                                           nil))]
        [:div.application__fixed-koski-tutkinto-item (set-checked-as-needed)
         [:div.application__fixed-koski-tutkinto-item.inner-content (set-checked-as-needed)
          [:input.application__form-checkbox.embedded
           (merge {:id        (str "checkbox-" id)
                   :type      "checkbox"
                   :read-only true
                   :checked   checked?
                   :role      "option"})]]
         [:div.application__fixed-koski-tutkinto-item.inner-content (set-checked-as-needed)
          [:span.application__fixed-koski-tutkinto-item.inner-content.upper-row (set-checked-as-needed) upper-row]
          (when lower-row
            [:span lower-row])]]))))

(defn- visible-tutkinto-field? [field-descriptor]
  (and @(subscribe [:application/visible? (keyword (:id field-descriptor))])
       (not (get-in field-descriptor [:params :transparent]))))

(defn tutkinnot-wrapper-field
  [field-descriptor _ _]
  (let [label (util/non-blank-val (:label field-descriptor) @(subscribe [:application/default-languages]))
        lang @(subscribe [:application/form-language])]
    (fn [field-descriptor _ render-field]
      (let [any-koski-tutkinnot? @(subscribe [:application/any-koski-tutkinnot?])
            show-itse-syotetyt? @(subscribe [:application/show-itse-syotetyt-tutkinnot?])
            itse-syotetty-content (tutkinto-util/find-itse-syotetty-content-beneath field-descriptor)
            on-click-to-show-additional-itse-syotetyt (fn [event]
                                                        (.preventDefault event)
                                                        (dispatch [:application/set-itse-syotetyt-visibility true]))
            on-click-to-hide-additional-itse-syotetyt (fn [event]
                                                        (.preventDefault event)
                                                        (dispatch [:application/clear-and-hide-itse-syotetyt]))]
        [:div.application__wrapper-element
         [:div.application__wrapper-heading
          [:h2 label]
          [scroll-to-anchor field-descriptor]]
         (into [:div.application__wrapper-contents]
               (for [child (:children field-descriptor)
                     :when @(subscribe [:application/visible-koski-wrapper-child? child])]
                 (if (ktm/is-tutkinto-configuration-component? child)
                   [:div
                    [:div
                     (into [:div]
                           (for [koski-item @(subscribe [:application/koski-tutkinnot])]
                             (let [id (:id koski-item)
                                   level (:level koski-item)
                                   question-group-of-level (tutkinto-util/get-question-group-of-level child level)
                                   answer-idx (tutkinto-util/get-tutkinto-idx level id)
                                   checked? (some? answer-idx)
                                   on-toggle (fn [event]
                                               (.preventDefault event)
                                               (if answer-idx
                                                 (dispatch [:application/remove-tutkinto-row
                                                            question-group-of-level
                                                            answer-idx])
                                                 (dispatch [:application/add-tutkinto-row
                                                            question-group-of-level
                                                            (tutkinto-util/id-field-of-level question-group-of-level level)
                                                            (:id koski-item)])))]
                               ^{:key id}
                               [:div.application__tutkinto-group-container
                                [:div
                                 {:on-click on-toggle}
                                 [fixed-tutkinto-item koski-item id checked?]]
                                (when checked?
                                  (let [additional-followups (filter visible-tutkinto-field?
                                                                     (:children question-group-of-level))]
                                    (when (seq additional-followups)
                                      [:div.application__form-multi-choice-followups-outer-container
                                       {:tab-index 0}
                                       [:div.application__form-multi-choice-followups-indicator]
                                       (into [:div.application__tutkinto-entity-container]
                                             (for [followup additional-followups]
                                               (with-meta [render-field followup answer-idx]
                                                          {:key (str (:id followup) "-" answer-idx)})))])))])))
                     (when any-koski-tutkinnot?
                       [:div
                        (if show-itse-syotetyt?
                          [hide-additional-tutkinnot-button on-click-to-hide-additional-itse-syotetyt lang]
                          [add-button on-click-to-show-additional-itse-syotetyt lang])])]
                    (for [followup itse-syotetty-content]
                      (with-meta [render-field followup nil] {:key (:id followup)}))]
                   (with-meta [render-field child nil] {:key (:id child)}))))]))))

