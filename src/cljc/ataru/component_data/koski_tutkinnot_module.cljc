(ns ataru.component-data.koski-tutkinnot-module
  (:require
    [ataru.translations.texts :refer [koski-tutkinnot-texts]]
    [ataru.component-data.component :as component]))

(defn koski-tutkinnot-questions [metadata]
    [(assoc (component/info-element metadata)
      :label (:info-label koski-tutkinnot-texts))])


(defn koski-tutkinnot-module [metadata]
  (assoc (component/form-section metadata)
    :fieldType "tutkinnot"
    :id "koski-tutkinnot-wrapper"
    :label (:section-label koski-tutkinnot-texts)
    :description (:section-description koski-tutkinnot-texts)
    ;;:show-child-component-names true
    :children (koski-tutkinnot-questions metadata)))
















;;(fn [content path]
;;    (let [languages (subscribe [:editor/languages])
;;          virkailija-lang (subscribe [:editor/virkailija-lang])]
;;    [:div.editor-form__component-wrapper
;;     [text-header-component/text-header (:id content) (get-in content [:label @virkailija-lang]) path (:metadata content)
;;      :foldable? false
;;      :can-cut? true
;;      :can-copy? false
;;      :can-remove? true
;;      :data-test-id "tutkinnot-header"]
