(ns ataru.virkailija.editor.components.toolbar
  (:require [ataru.component-data.component :as component]
            [ataru.component-data.base-education-module-higher :as kk-base-education-module]
            [ataru.component-data.base-education-module-2nd :refer [base-education-2nd-module]]
            [ataru.component-data.base-education-continuous-admissions-module :refer [base-education-continuous-admissions-module]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [ataru.component-data.arvosanat-module :as arvosanat]
            [ataru.component-data.koski-tutkinnot-module :refer [koski-tutkinnot-module]]))

(defn- toolbar-elements []
  [[:form-section component/form-section {:data-test-id "component-toolbar-lomakeosio"}]
   [:single-choice-button component/single-choice-button {:data-test-id "component-toolbar-painikkeet-yksi-valittavissa"}]
   [:single-choice-button-koodisto (fn [metadata]
                                     (assoc (component/single-choice-button metadata)
                                            :koodisto-source {:uri "" :title "" :version 1}
                                            :options []))]
   [:dropdown component/dropdown {:data-test-id "component-toolbar-dropdown"}]
   [:dropdown-koodisto (fn [metadata]
                         (assoc (component/dropdown metadata)
                                :koodisto-source {:uri "" :title "" :version 1}
                                :options []))
    {:data-test-id "component-toolbar-dropdown-koodisto"}]
   [:multiple-choice component/multiple-choice {:data-test-id "component-toolbar-multiple-choice"}]
   [:multiple-choice-koodisto (fn [metadata]
                                (assoc (component/multiple-choice metadata)
                                       :koodisto-source {:uri "" :title "" :version 1}
                                       :options []))
    {:data-test-id "component-toolbar-multiple-choice-koodisto"}]
   [:text-field component/text-field {:data-test-id "component-toolbar-tekstikenttä"}]
   [:text-area component/text-area {:data-test-id "component-toolbar-tekstialue"}]
   [:adjacent-fieldset component/adjacent-fieldset {:data-test-id "component-toolbar-adjacent-fieldset"}]
   [:attachment component/attachment]
   [:question-group component/question-group]
   [:info-element component/info-element]
   [:modal-info-element component/modal-info-element]
   [:kk-base-education-module kk-base-education-module/base-education-module-higher]
   [:base-education-module-2nd base-education-2nd-module]
   [:base-education-continuous-admission base-education-continuous-admissions-module]
   [:pohjakoulutusristiriita component/pohjakoulutusristiriita]
   [:lupa-sahkoiseen-asiointiin component/lupa-sahkoiseen-asiointiin]
   [:lupatiedot-kk component/lupatiedot-kk]
   [:lupatiedot-toinen-aste component/lupatiedot-toinen-aste]
   [:guardian-contact-information component/huoltajan-yhteystiedot]
   [:harkinnanvaraisuus component/harkinnanvaraisuus]
   [:tutkinnot koski-tutkinnot-module]])

(def followup-toolbar-element-names
  #{:text-field
    :text-area
    :single-choice-button
    :single-choice-button-koodisto
    :dropdown
    :dropdown-koodisto
    :multiple-choice
    :multiple-choice-koodisto
    :info-element
    :modal-info-element
    :attachment
    :adjacent-fieldset
    :question-group
    :tutkinnot})

(def question-group-toolbar-element-names
  #{:text-field
    :text-area
    :single-choice-button
    :single-choice-button-koodisto
    :dropdown
    :dropdown-koodisto
    :multiple-choice
    :multiple-choice-koodisto
    :info-element
    :modal-info-element
    :attachment
    :adjacent-fieldset})

(defn- followup-toolbar-elements []
  (filter
    (fn [[el-name _]] (contains? followup-toolbar-element-names el-name))
    (toolbar-elements)))

(defn- question-group-toolbar-elements []
  (filter
    (fn [[el-name _]] (contains? question-group-toolbar-element-names el-name))
    (toolbar-elements)))

(def ^:private adjacent-fieldset-toolbar-elements
  {:text-field (comp (fn [text-field] (assoc text-field :params {:adjacent true}))
                    component/text-field)})

(defn- component-toolbar [_ _ _]
  (fn [path _elements generator]
    (let [base-education-module-exists?   (subscribe [:editor/base-education-module-exists?])
          pohjakoulutusristiriita-exists? (subscribe [:editor/pohjakoulutusristiriita-exists?])
          tutkinnot-component-exists? (subscribe [:editor/tutkinnot-component-exists?])
          hakeminen-tunnistautuneena-not-allowed? (not @(subscribe [:editor/allow-hakeminen-tunnistautuneena?]))
          tutkinto-question-group-allowed? @(subscribe [:editor/get-component-param :allow-tutkinto-question-group path])
          elements (if tutkinto-question-group-allowed?
                     (conj _elements [:question-group-tutkinto component/question-group-tutkinto])
                     _elements)]
      (into [:ul.form__add-component-toolbar--list]
            (for [[component-name generate-fn {:keys [data-test-id]}] elements
                  :when (and (not (and (vector? path)
                                       (= :children (second path))
                                       (= :form-section component-name)))
                             (not (and @base-education-module-exists?
                                       (contains? #{:base-education-module :kk-base-education-module :base-education-module-2nd} component-name)))
                             (not (and @pohjakoulutusristiriita-exists?
                                       (= :pohjakoulutusristiriita component-name)))
                             (not (and (or hakeminen-tunnistautuneena-not-allowed? @tutkinnot-component-exists?)
                                       (= :tutkinnot component-name))))]
              [:li.form__add-component-toolbar--list-item
               [:a {:on-click (fn [evt]
                                (.preventDefault evt)
                                (generator generate-fn))
                    :data-test-id data-test-id}
                @(subscribe [:editor/virkailija-translation component-name])]])))))


(defn custom-add-component [_ _ _ _]
  (let [mouse-over?  (r/atom false)
        form-locked? (subscribe [:editor/form-locked?])]
    (fn [toolbar path generator root-level-add-component?]
      [:div.editor-form__add-component-toolbar
       {:class          (when @form-locked? "disabled")
        :on-mouse-enter #(reset! mouse-over? true)
        :on-mouse-leave #(reset! mouse-over? false)
        :data-test-id   (if root-level-add-component?
                          "component-toolbar"
                          "component-subform-toolbar")}
       (cond @form-locked?
             [:div.plus-component.plus-component--disabled [:span "+"]]
             @mouse-over?
             [component-toolbar path toolbar generator]
             :else
             [:div.plus-component [:span "+"]])])))

(defn handle-root-level-components [elements]
  (conj elements
        [:arvosanat-peruskoulu arvosanat/arvosanat-peruskoulu {:data-test-id "component-toolbar-arvosanat"}]
        [:arvosanat-lukio arvosanat/arvosanat-lukio]))

(defn add-component [path root-level-add-component?]
  (let [elements (cond-> (toolbar-elements)
                         root-level-add-component?
                         (handle-root-level-components))]
    [custom-add-component elements path
     (fn [generate-fn]
       (dispatch [:generate-component generate-fn path]))
     root-level-add-component?]))

(defn followup-toolbar [option-path generator]
  [custom-add-component (followup-toolbar-elements) option-path generator true])

(defn question-group-toolbar [option-path generator]
  [custom-add-component (question-group-toolbar-elements) option-path generator true])

(defn adjacent-fieldset-toolbar [path generator]
  [custom-add-component adjacent-fieldset-toolbar-elements path generator true])
