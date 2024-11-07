(ns ataru.component-data.koski-tutkinnot-module
  (:require
    [ataru.translations.texts :refer [koski-tutkinnot-texts]]
    [ataru.component-data.component :as component]
    [ataru.component-data.form-property-component :as form-property-component]))

(defn- tutkinto-tasot [metadata]
  [{:id "perusopetus"
    :label (:perusopetus-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id "perusopetus-tutkinto-nimi"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "perusopetus-koulutusohjelma"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "perusopetus-oppilaitos"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "perusopetus-valmistumispvm"
                  :params {:transparent true})]}

   {:id "yo"
    :label (:yo-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id "yo-tutkinto-nimi"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "yo-koulutusohjelma"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "yo-oppilaitos"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "yo-valmistumispvm"
                  :params {:transparent true})]}
   {:id "amm-perus"
    :label (:amm-perustutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id "amm-perus-tutkinto-nimi"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "amm-perus-koulutusohjelma"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "amm-perus-oppilaitos"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "amm-perus-valmistumispvm"
                  :params {:transparent true})]}
   {:id "amm"
    :label (:amm-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id "amm-tutkinto-nimi"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "amm-koulutusohjelma"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "amm-oppilaitos"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "amm-valmistumispvm"
                  :params {:transparent true})]}
   {:id "amm-erikois"
    :label (:amm-erikoistutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id "amm-erikois-tutkinto-nimi"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "amm-erikois-koulutusohjelma"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "amm-erikois-oppilaitos"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "amm-erikois-valmistumispvm"
                  :params {:transparent true})]}
   {:id "kk-alemmat"
    :label (:alemmat-kk-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id "kk-alemmat-tutkinto-nimi"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "kk-alemmat-koulutusohjelma"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "kk-alemmat-oppilaitos"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "kk-alemmat-valmistumispvm"
                  :params {:transparent true})]}
   {:id "kk-ylemmat"
    :label (:ylemmat-kk-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id "kk-ylemmat-tutkinto-nimi"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "kk-ylemmat-koulutusohjelma"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "kk-ylemmat-oppilaitos"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "kk-ylemmat-valmistumispvm"
                  :params {:transparent true})]}
   {:id "tohtori"
    :label (:tohtori-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id "tohtori-tutkinto-nimi"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "tohtori-koulutusohjelma"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "tohtori-oppilaitos"
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id "tohtori-valmistumispvm"
                  :params {:transparent true})
                ]}
   {:id "itse-syotetty"
    :label (:itse-syotetty-tutkinnot-label koski-tutkinnot-texts)
    :default-value true
    :followup-label (:itse-syotetty-followup-label koski-tutkinnot-texts)
    :forced true
    :params {:allow-tutkinto-question-group true}
    :followups  [(assoc (component/question-group-tutkinto metadata)
                   :label (:itse-syotetty-tutkinto-group-label koski-tutkinnot-texts)
                   :children
                   [(assoc (component/text-field metadata)
                      :id "itse-syotetty-tutkinto-nimi"
                      :validators []
                      :label (:itse-syotetty-tutkinto-followup-label koski-tutkinnot-texts))
                    (assoc (component/text-field metadata)
                      :id "itse-syotetty-koulutusohjelma"
                      :validators []
                      :label (:itse-syotetty-koulutusohjelma-followup-label koski-tutkinnot-texts))
                    (assoc (component/text-field metadata)
                      :id "itse-syotetty-oppilaitos"
                      :validators []
                      :label (:itse-syotetty-oppilaitos-followup-label koski-tutkinnot-texts))
                    (assoc (component/text-field metadata)
                      :id "itse-syotetty-valmistumispvm"
                      :validators []
                      :label (:itse-syotetty-valmistumispvm-followup-label koski-tutkinnot-texts)
                      :params {:info-text {:label (:itse-syotetty-valimistumispvm-infotext-label koski-tutkinnot-texts)}})
                    (assoc (component/attachment metadata)
                      :label (:itse-syotetty-liitteet-followup-label koski-tutkinnot-texts)
                      :validators []
                      :params {:mail-attachment? false
                               :info-text {:enabled? true
                                           :value (:itse-syotetty-liitteet-infotext-value koski-tutkinnot-texts)}})])]}])

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
    :tutkinnot {:description (:section-description koski-tutkinnot-texts)
                :field-list (:field-list koski-tutkinnot-texts)}
    :children (koski-tutkinnot-questions metadata)))
