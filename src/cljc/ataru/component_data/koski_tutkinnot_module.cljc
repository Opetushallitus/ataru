(ns ataru.component-data.koski-tutkinnot-module
  (:require
    [ataru.translations.texts :refer [koski-tutkinnot-texts]]
    [ataru.component-data.component :as component]
    [ataru.component-data.form-property-component :as form-property-component]))

(def tutkinto-property-component-category "tutkinto-properties")
(def itse-syotetty-option-id "itse-syotetty")
(def perusopetus-option-id "perusopetus")
(def yo-option-id "yo")
(def amm-perus-option-id "amm-perus")
(def amm-option-id "amm")
(def amm-erikois-option-id "amm-erikois")
(def kk-alemmat-option-id "kk-alemmat")
(def kk-ylemmat-option-id "kk-ylemmat")
(def tohtori-option-id "tohtori")
(def tutkinto-nimi-field-postfix "tutkinto-nimi")
(def koulutusohjelma-field-postfix "koulutusohjelma")
(def oppilaitos-field-postfix "oppilaitos")
(def valmistumispvm-field-postfix "valmistumispvm")

(def koski-tutkinto-option-ids [perusopetus-option-id yo-option-id amm-perus-option-id amm-option-id
                                amm-erikois-option-id kk-alemmat-option-id kk-ylemmat-option-id tohtori-option-id])

(defn- tutkinto-tasot [metadata]
  [{:id perusopetus-option-id
    :label (:perusopetus-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id (str perusopetus-option-id "-" tutkinto-nimi-field-postfix)
                  :label (:tutkinto-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str perusopetus-option-id "-" koulutusohjelma-field-postfix)
                  :label (:koulutusohjelma-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str perusopetus-option-id "-" oppilaitos-field-postfix)
                  :label (:oppilaitos-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str perusopetus-option-id "-" valmistumispvm-field-postfix)
                  :label (:valmistumispvm-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})]}

   {:id yo-option-id
    :label (:yo-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id (str yo-option-id "-" tutkinto-nimi-field-postfix)
                  :label (:tutkinto-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str yo-option-id "-" koulutusohjelma-field-postfix)
                  :label (:koulutusohjelma-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str yo-option-id "-" oppilaitos-field-postfix)
                  :label (:oppilaitos-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str yo-option-id "-" valmistumispvm-field-postfix)
                  :label (:valmistumispvm-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})]}
   {:id amm-perus-option-id
    :label (:amm-perustutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id (str amm-perus-option-id "-" tutkinto-nimi-field-postfix)
                  :label (:tutkinto-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str amm-perus-option-id "-" koulutusohjelma-field-postfix)
                  :label (:koulutusohjelma-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str amm-perus-option-id "-" oppilaitos-field-postfix)
                  :label (:oppilaitos-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str amm-perus-option-id "-" valmistumispvm-field-postfix)
                  :label (:valmistumispvm-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})]}
   {:id amm-option-id
    :label (:amm-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id (str amm-option-id "-" tutkinto-nimi-field-postfix)
                  :label (:tutkinto-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str amm-option-id "-" koulutusohjelma-field-postfix)
                  :label (:koulutusohjelma-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str amm-option-id "-" oppilaitos-field-postfix)
                  :label (:oppilaitos-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str amm-option-id "-" valmistumispvm-field-postfix)
                  :label (:valmistumispvm-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})]}
   {:id amm-erikois-option-id
    :label (:amm-erikoistutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id (str amm-erikois-option-id "-" tutkinto-nimi-field-postfix)
                  :label (:tutkinto-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str amm-erikois-option-id "-" koulutusohjelma-field-postfix)
                  :label (:koulutusohjelma-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str amm-erikois-option-id "-" oppilaitos-field-postfix)
                  :label (:oppilaitos-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str amm-erikois-option-id "-" valmistumispvm-field-postfix)
                  :label (:valmistumispvm-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})]}
   {:id kk-alemmat-option-id
    :label (:alemmat-kk-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id (str kk-alemmat-option-id "-" tutkinto-nimi-field-postfix)
                  :label (:tutkinto-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str kk-alemmat-option-id "-" koulutusohjelma-field-postfix)
                  :label (:koulutusohjelma-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str kk-alemmat-option-id "-" oppilaitos-field-postfix)
                  :label (:oppilaitos-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str kk-alemmat-option-id "-" valmistumispvm-field-postfix)
                  :label (:valmistumispvm-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})]}
   {:id kk-ylemmat-option-id
    :label (:ylemmat-kk-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id (str kk-ylemmat-option-id "-" tutkinto-nimi-field-postfix)
                  :label (:tutkinto-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str kk-ylemmat-option-id "-" koulutusohjelma-field-postfix)
                  :label (:koulutusohjelma-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str kk-ylemmat-option-id "-" oppilaitos-field-postfix)
                  :label (:oppilaitos-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str kk-ylemmat-option-id "-" valmistumispvm-field-postfix)
                  :label (:valmistumispvm-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})]}
   {:id tohtori-option-id
    :label (:tohtori-tutkinnot-label koski-tutkinnot-texts)
    :followup-label (:koski-followup-label koski-tutkinnot-texts)
    :followups [(assoc (component/text-field metadata)
                  :id (str tohtori-option-id "-" tutkinto-nimi-field-postfix)
                  :label (:tutkinto-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str tohtori-option-id "-" koulutusohjelma-field-postfix)
                  :label (:koulutusohjelma-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str tohtori-option-id "-" oppilaitos-field-postfix)
                  :label (:oppilaitos-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                (assoc (component/text-field metadata)
                  :id (str tohtori-option-id "-" valmistumispvm-field-postfix)
                  :label (:valmistumispvm-followup-label koski-tutkinnot-texts)
                  :params {:transparent true})
                ]}
   {:id itse-syotetty-option-id
    :label (:itse-syotetty-tutkinnot-label koski-tutkinnot-texts)
    :default-value true
    :followup-label (:itse-syotetty-followup-label koski-tutkinnot-texts)
    :forced true
    :params {:allow-tutkinto-question-group true}
    :followups  [(assoc (component/question-group-tutkinto metadata)
                   :label (:itse-syotetty-tutkinto-group-label koski-tutkinnot-texts)
                   :children
                   [(assoc (component/text-field metadata)
                      :id (str itse-syotetty-option-id "-" tutkinto-nimi-field-postfix)
                      :validators []
                      :label (:tutkinto-followup-label koski-tutkinnot-texts))
                    (assoc (component/text-field metadata)
                      :id (str itse-syotetty-option-id "-" koulutusohjelma-field-postfix)
                      :validators []
                      :label (:koulutusohjelma-followup-label koski-tutkinnot-texts))
                    (assoc (component/text-field metadata)
                      :id (str itse-syotetty-option-id "-" oppilaitos-field-postfix)
                      :validators []
                      :label (:oppilaitos-followup-label koski-tutkinnot-texts))
                    (assoc (component/text-field metadata)
                      :id (str itse-syotetty-option-id "-" valmistumispvm-field-postfix)
                      :validators []
                      :label (:valmistumispvm-followup-label koski-tutkinnot-texts)
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
        :category tutkinto-property-component-category
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

(defn is-tutkinto-configuration-component? [field-descriptor]
  (= tutkinto-property-component-category (:category field-descriptor)))

(defn is-koski-tutkinto-option [id]
  (some? (some #(when (= id %) %) koski-tutkinto-option-ids)))
