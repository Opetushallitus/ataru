(ns ataru.component-data.koski-tutkinnot-module
  (:require
    [ataru.translations.texts :refer [koski-tutkinnot-texts]]
    [ataru.component-data.component :as component]
    [ataru.component-data.form-property-component :as form-property-component]))

(defn- tutkinto-tasot [metadata]
  [{:id "perusopetus"
    :label (:perusopetus-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)}
   {:id "yo"
    :label (:yo-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)}
   {:id "amm-perus"
    :label (:amm-perustutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)}
   {:id "amm"
    :label (:amm-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)}
   {:id "amm-erikois"
    :label (:amm-erikoistutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)}
   {:id "kk-alemmat"
    :label (:alemmat-kk-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)}
   {:id "kk-ylemmat"
    :label (:ylemmat-kk-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)}
   {:id "tohtori"
    :label (:tohtori-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)}
   {:id "ei-koskesta"
    :label (:ei-koski-tutkinnot-label koski-tutkinnot-texts)
    :default-value true
    :followup-label (:ei-koski-followup-label koski-tutkinnot-texts)
    :forced true
    :followups  [(assoc (component/text-field metadata)
                   :label (:ei-koski-tutkinto-followup-label koski-tutkinnot-texts))
                 (assoc (component/text-field metadata)
                   :label (:ei-koski-koulutusohjelma-followup-label koski-tutkinnot-texts))
                 (assoc (component/text-field metadata)
                   :label (:ei-koski-oppilaitos-followup-label koski-tutkinnot-texts))
                 (assoc (component/text-field metadata)
                   :label (:ei-koski-valmistumispvm-followup-label koski-tutkinnot-texts)
                   :params {:info-text {:label (:ei-koski-valimistumispvm-infotext-label koski-tutkinnot-texts)}})
                 (assoc (component/attachment metadata)
                   :label (:ei-koski-liitteet-followup-label koski-tutkinnot-texts)
                   :validators []
                   :params {:mail-attachment? false
                            :info-text {:enabled? true
                                        :value (:ei-koski-liitteet-infotext-value koski-tutkinnot-texts)}})

                 ]}])

(defn koski-tutkinnot-questions [metadata]
    [(assoc (component/info-element metadata)
        :label (:info-label koski-tutkinnot-texts))
     (assoc (form-property-component/property-multiple-choice metadata)
        :category "tutkinto-properties"
        :label (:tutkintotaso-label koski-tutkinnot-texts)
        :description (:tutkintotaso-description koski-tutkinnot-texts)
        :options (tutkinto-tasot metadata))])


(defn koski-tutkinnot-module [metadata]
  (assoc (component/form-section metadata)
    :fieldType "tutkinnot"
    :id "koski-tutkinnot-wrapper"
    :label (:section-label koski-tutkinnot-texts)
    :description (:section-description koski-tutkinnot-texts)
    :field-list (:field-list koski-tutkinnot-texts)
    :children (koski-tutkinnot-questions metadata)))
