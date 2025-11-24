(ns ataru.schema.form-schema
  (:require [ataru.application.review-states :as review-states]
            [ataru.application.field-types :refer [form-fields]]
            [ataru.schema.button-schema :as button-schema]
            [ataru.schema.child-validator-schema :as child-validator-schema]
            [ataru.schema.info-element-schema :as info-element-schema]
            [ataru.schema.modal-info-element-schema :as modal-info-element-schema]
            [ataru.schema.validator-schema :as validator-schema]
            [ataru.schema.module-schema :as module-schema]
            [ataru.schema.form-element-schema :as form-schema]
            [ataru.schema.priorisoiva-hakukohderyhma-schema :as priorisoiva-hakukohderyhma-schema]
            [ataru.schema.params-schema :as params-schema]
            [ataru.schema.pohjakoulutus-ristiriita-schema :as pohjakoulutus-ristiriita-schema]
            [ataru.schema.koski-tutkinnot-schema :as tutkinnot-schema]
            [ataru.user-rights :as user-rights]
            [clojure.string :as string]
            [ataru.schema.element-metadata-schema :as element-metadata-schema]
            [ataru.schema.localized-schema :as localized-schema]
            [schema.coerce :as c]
            [schema.core :as s]
            [schema-tools.core :as st]
            [ataru.schema.form-properties-schema :refer [FormProperties]]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-types]])
  #?(:clj (:import [org.joda.time DateTime])))

;        __.,,------.._
;     ,'"   _      _   "`.
;    /.__, ._  -=- _ "`    Y
;   (.____.-.`      ""`   j
;    VvvvvvV`.Y,.    _.,-'       ,     ,     ,
;        Y    ||,   '"\         ,/    ,/    ./
;        |   ,'  ,     `-..,'_,'/___,'/   ,'/   ,
;   ..  ,;,,',-'"\,'  ,  .     '     ' ""' '--,/    .. ..
; ,'. `.`---'     `, /  , Y -=-    ,'   ,   ,. .`-..||_|| ..
;ff\\`. `._        /f ,'j j , ,' ,   , f ,  \=\ Y   || ||`||_..
;l` \` `.`."`-..,-' j  /./ /, , / , / /l \   \=\l   || `' || ||...
; `  `   `-._ `-.,-/ ,' /`"/-/-/-/-"'''"`.`.  `'.\--`'--..`'_`' || ,
;            "`-_,',  ,'  f    ,   /      `._    ``._     ,  `-.`'//         ,
;          ,-"'' _.,-'    l_,-'_,,'          "`-._ . "`. /|     `.'\ ,       |
;        ,',.,-'"          \=) ,`-.         ,    `-'._`.V |       \ // .. . /j
;        |f\\               `._ )-."`.     /|         `.| |        `.`-||-\\/
;        l` \`                 "`._   "`--' j          j' j          `-`---'
;         `  `                     "`_,-','/       ,-'"  /
;                                 ,'",__,-'       /,, ,-'
;                                 Vvv'            VVv'
(declare BasicElement)
(declare WrapperElement)

(defn form-field-schema
  [option-condition-schema]
  {:fieldClass                                      (s/eq "formField")
   :id                                              s/Str
   :fieldType                                       (apply s/enum form-fields)
   :metadata                                        element-metadata-schema/ElementMetadata
   (s/optional-key :cannot-view)                    s/Bool
   (s/optional-key :cannot-edit)                    s/Bool
   (s/optional-key :validators)                     [validator-schema/Validator]
   (s/optional-key :rules)                          {s/Keyword s/Any}
   (s/optional-key :blur-rules)                     {s/Keyword s/Any}
   (s/optional-key :label)                          localized-schema/LocalizedString
   (s/optional-key :label-amendment)                localized-schema/LocalizedString
   (s/optional-key :unselected-label)               localized-schema/LocalizedString
   (s/optional-key :unselected-label-icon)          [(s/one s/Str "icon component")]
   (s/optional-key :initialValue)                   (s/cond-pre localized-schema/LocalizedString s/Int)
   (s/optional-key :params)                         params-schema/Params
   (s/optional-key :no-blank-option)                s/Bool
   (s/optional-key :exclude-from-answers)           s/Bool
   (s/optional-key :exclude-from-answers-if-hidden) s/Bool
   (s/optional-key :version)                        s/Str
   (s/optional-key :koodisto-ordered-by-user)       s/Bool
   (s/optional-key :sort-by-label)                  s/Bool
   (s/optional-key :koodisto-source)                {:uri                             s/Str
                                                     :version                         s/Int
                                                     (s/optional-key :default-option) s/Any
                                                     (s/optional-key :title)          s/Str
                                                     (s/optional-key :allow-invalid?) s/Bool}
   (s/optional-key :section-visibility-conditions)  [{:section-name s/Str
                                                      :condition    option-condition-schema}]
   (s/optional-key :options)                        [{:value                                      s/Str
                                                      (s/optional-key :label)                     localized-schema/LocalizedStringOptional
                                                      (s/optional-key :description)               localized-schema/LocalizedStringOptional
                                                      (s/optional-key :selection-limit)           (s/maybe s/Int)
                                                      (s/optional-key :default-value)             (s/maybe s/Bool)
                                                      (s/optional-key :hidden)                    (s/maybe s/Bool)
                                                      (s/optional-key :condition)                 option-condition-schema
                                                      (s/optional-key :belongs-to-hakukohteet)    [s/Str]
                                                      (s/optional-key :belongs-to-hakukohderyhma) [s/Str]
                                                      (s/optional-key :followups)                 [(s/if (comp some? :children) (s/recursive #'WrapperElement) (s/recursive #'BasicElement))]}]
   (s/optional-key :belongs-to-hakukohteet)         [s/Str]
   (s/optional-key :belongs-to-hakukohderyhma)      [s/Str]
   (s/optional-key :per-hakukohde)                  s/Bool
   (s/optional-key :sensitive-answer)               s/Bool})

(s/defschema TextFieldOptionCondition
  {:comparison-operator                 (s/enum "<" "=" ">")
   (s/optional-key :data-type)          (s/eq "int")
   (s/optional-key :answer-compared-to) s/Int})

(s/defschema TextField
  (form-field-schema TextFieldOptionCondition))

(s/defschema ChoiceFieldOptionCondition
  {:comparison-operator                 (s/enum "=")
   (s/optional-key :data-type)          (s/eq "str")
   (s/optional-key :answer-compared-to) s/Str})

(s/defschema ChoiceField
  (form-field-schema ChoiceFieldOptionCondition))

(s/defschema FormField
  (s/conditional
    #(#{"dropdown" "singleChoice" "multipleChoice"} (:fieldType %))
    ChoiceField

    :else
    TextField))

(s/defschema FormPropertyField
  {:id                            s/Str
   :fieldClass                    (s/eq "formPropertyField")
   :fieldType                     (apply s/enum ["multipleOptions"])
   :category                      s/Str
   :exclude-from-answers          (s/eq true)
   :metadata                      element-metadata-schema/ElementMetadata
   :label                         localized-schema/LocalizedStringOptional
   :description                   localized-schema/LocalizedStringOptional
   (s/optional-key :mandatory)    s/Bool
   (s/optional-key :validate-info) localized-schema/LocalizedStringOptional
   (s/optional-key :params)       params-schema/Params
   (s/optional-key :rules)        {s/Keyword s/Any}
   (s/optional-key :property-options) [{:id                               s/Str
                                        :label                            localized-schema/LocalizedStringOptional
                                        (s/optional-key :default-value)   s/Bool
                                        (s/optional-key :params)          params-schema/Params
                                        (s/optional-key :description)     localized-schema/LocalizedStringOptional
                                        (s/optional-key :forced)          s/Bool
                                        (s/optional-key
                                          :allow-user-followups)          s/Bool
                                        (s/optional-key :followup-label)  localized-schema/LocalizedStringOptional
                                        (s/optional-key :followups)       [(s/if (comp some? :children)
                                                                           (s/recursive #'WrapperElement)
                                                                           (s/recursive #'BasicElement))]}]})

(s/defschema BasicElement
  (s/conditional
   #(= "formField" (:fieldClass %)) FormField
   #(= "button" (:fieldClass %)) button-schema/Button
   #(= "pohjakoulutusristiriita" (:fieldClass %)) pohjakoulutus-ristiriita-schema/Pohjakoulutusristiriita
   #(= "modalInfoElement" (:fieldClass %)) modal-info-element-schema/ModalInfoElement
   #(= "formPropertyField" (:fieldClass %)) FormPropertyField
   :else info-element-schema/InfoElement))

(s/defschema WrapperBase    {:fieldClass                                  (apply s/enum ["wrapperElement" "questionGroup"])
                             :id                                          s/Str
                             :fieldType                                   (apply s/enum ["fieldset" "rowcontainer" "adjacentfieldset" "tutkinnot" "tutkintofieldset" "embedded"])
                             :children                                    [(s/conditional
                                                                             #(or (= "wrapperElement" (:fieldClass %))
                                                                                  (= "questionGroup" (:fieldClass %)))
                                                                             (s/recursive #'WrapperElement)
                                                                             :else
                                                                             BasicElement)]
                             :metadata                                    element-metadata-schema/ElementMetadata
                             (s/optional-key :version)                    s/Str
                             (s/optional-key :child-validator)            child-validator-schema/ChildValidator
                             (s/optional-key :params)                     params-schema/Params
                             (s/optional-key :label)                      localized-schema/LocalizedString
                             (s/optional-key :label-amendment)            localized-schema/LocalizedString ; Additional info which can be displayed next to the label
                             (s/optional-key :module)                     module-schema/Module
                             (s/optional-key :belongs-to-hakukohteet)     [s/Str]
                             (s/optional-key :belongs-to-hakukohderyhma)  [s/Str]})

(s/defschema WrapperElement
  (s/conditional
   #(= "tutkinnot" (:fieldType %)) (merge WrapperBase tutkinnot-schema/Tutkinnot)
   :else WrapperBase))

(def Content (s/if (comp some? :children) WrapperElement BasicElement))

(s/defschema RajaavaHakukohderyhma
  {:haku-oid           s/Str
   :hakukohderyhma-oid s/Str
   :raja               s/Int})

(s/defschema SelectionLimit
  {:question-id s/Str
   :answer-id   s/Str})

(s/defschema FormSelectionLimit
  {(s/optional-key :selection-id) s/Str
   :limit-reached                 [SelectionLimit]})

(s/defschema FormWithContent
  (merge form-schema/Form
         {:content                                       [Content]
          (s/optional-key :priorisoivat-hakukohderyhmat) [priorisoiva-hakukohderyhma-schema/PriorisoivaHakukohderyhma]
          (s/optional-key :rajaavat-hakukohderyhmat)     [RajaavaHakukohderyhma]
          (s/optional-key :organization-oid)             (s/maybe s/Str)}))

(s/defschema UpdateElementOperation
  {:type        (s/eq "update")
   :old-element (s/if (comp some? :children) WrapperElement BasicElement)
   :new-element (s/if (comp some? :children) WrapperElement BasicElement)})

(s/defschema DeleteElementOperation
  {:type    (s/eq "delete")
   :element (s/if (comp some? :children) WrapperElement BasicElement)})

(s/defschema CreateMoveElement
  {:sibling-above (s/maybe s/Str)
   :sibling-below (s/maybe s/Str)
   :elements      [(s/if (comp some? :children) WrapperElement BasicElement)]})

(s/defschema CreateMoveGroupOperation
  {:type   (s/eq "create-move-group")
   :groups [CreateMoveElement]})

(s/defschema FormDetails
  {:name                        localized-schema/LocalizedStringOptional
   (s/optional-key :languages)  [s/Str]
   (s/optional-key :properties) FormProperties})

(s/defschema UpdateFormDetailsOperation
  {:type (s/eq "update-form-details")
   :old-form FormDetails
   :new-form FormDetails})

(def Operation (s/conditional
                  #(= "update-form-details" (:type %)) UpdateFormDetailsOperation
                  #(= "create-move-group" (:type %)) CreateMoveGroupOperation
                  #(= "update" (:type %)) UpdateElementOperation
                  #(= "delete" (:type %)) DeleteElementOperation))

(s/defschema Hakuaika
  {:label                               {:start                 localized-schema/LocalizedDateTime
                                         :end                   localized-schema/LocalizedDateTime
                                         :end-time              localized-schema/LocalizedDateTime}
   :start                               s/Int
   :end                                 (s/maybe s/Int)
   :on                                  s/Bool
   :jatkuva-haku?                       s/Bool
   :joustava-haku?                      s/Bool
   :jatkuva-or-joustava-haku?           s/Bool
   :attachment-modify-grace-period-days (s/maybe s/Int)
   :hakukierros-end                     (s/maybe s/Int)})

(s/defschema Koulutus
  {:oid                       s/Str
   :koulutuskoodi-name        localized-schema/LocalizedStringOptional
   :koulutusohjelma-name      localized-schema/LocalizedStringOptional
   :tutkintonimike-names      [localized-schema/LocalizedStringOptional]
   (s/optional-key :tarkenne) s/Str})

(s/defschema PostinumeroJaPaikka
  {(s/optional-key :koodiUri) (s/maybe s/Str)
   (s/optional-key :nimi)     (s/maybe  s/Str)})

(s/defschema Toimitusosoite
  {(s/optional-key :osoite)      (s/maybe localized-schema/LocalizedStringOptional)
   (s/optional-key :postinumero) (s/maybe {(s/optional-key :fi) (s/maybe PostinumeroJaPaikka)
                                           (s/optional-key :sv) (s/maybe PostinumeroJaPaikka)
                                           (s/optional-key :en) (s/maybe PostinumeroJaPaikka)})
   (s/optional-key :verkkosivu)   (s/maybe s/Str)})


(s/defschema HakukohdeLiite
  {(s/optional-key :tyyppi)                (s/maybe s/Str)
   (s/optional-key :toimitusaika)          (s/maybe localized-schema/LocalizedDateTime)
   (s/optional-key :toimitetaan-erikseen)  (s/maybe s/Bool)
   (s/optional-key :toimitusosoite)        Toimitusosoite})

(s/defschema FormTarjontaHakukohde
  {:oid                                                                          s/Str
   :name                                                                         localized-schema/LocalizedStringOptional
   :can-be-applied-to?                                                           s/Bool
   (s/optional-key :archived)                                                    (s/maybe s/Bool)
   :kohdejoukko-korkeakoulu?                                                     s/Bool
   :tarjoaja-name                                                                localized-schema/LocalizedStringOptional
   :tarjoaja-oids                                                                (s/maybe [s/Str])
   (s/optional-key :form-key)                                                    (s/maybe s/Str)
   :hakukohderyhmat                                                              [s/Str]
   :hakuaika                                                                     Hakuaika
   :koulutukset                                                                  [Koulutus]
   (s/optional-key :koulutustyyppikoodi)                                         (s/maybe s/Str)
   :applicable-base-educations                                                   [s/Str]
   ;; jyemp
   (s/optional-key :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja?) s/Bool
   (s/optional-key :yo-amm-autom-hakukelpoisuus)                                 s/Bool
   (s/optional-key :liitteet)                                                    [HakukohdeLiite]
   (s/optional-key :liitteet-onko-sama-toimitusosoite?)                          s/Bool
   (s/optional-key :liitteiden-toimitusosoite)                                   (s/maybe Toimitusosoite)
   (s/optional-key :liitteet-onko-sama-toimitusaika?)                            s/Bool
   (s/optional-key :liitteiden-toimitusaika)                                     (s/maybe localized-schema/LocalizedDateTime)
   (s/optional-key :opetuskieli-koodi-urit)                                      [s/Str]})

(s/defschema FormTarjontaMetadata
  {:hakukohteet                        [FormTarjontaHakukohde]
   :haku-oid                           s/Str
   :hakuaika                           Hakuaika
   :haku-name                          localized-schema/LocalizedStringOptional
   :prioritize-hakukohteet             s/Bool
   :max-hakukohteet                    (s/maybe s/Int)
   :can-submit-multiple-applications   s/Bool
   :kohdejoukko-uri                    s/Str
   :hakutapa-uri                       s/Str
   :yhteishaku                         (s/maybe s/Bool)
   (s/optional-key :maksullinen-kk-haku?) (s/maybe s/Bool)
   (s/optional-key :kohdejoukon-tarkenne-uri) (s/maybe s/Str)
   (s/optional-key :alkamiskausi)      (s/maybe s/Str)
   (s/optional-key :alkamisvuosi)      (s/maybe s/Int)})

(s/defschema Haku
  {:oid                                        s/Str
   :name                                       localized-schema/LocalizedStringOptional
   :hakukohteet                                [s/Str]
   :ylioppilastutkinto-antaa-hakukelpoisuuden? s/Bool
   :kohdejoukko-uri                            s/Str
   :hakutapa-uri                               s/Str
   :yhteishaku                                 s/Bool
   :prioritize-hakukohteet                     s/Bool
   :can-submit-multiple-applications           s/Bool
   :sijoittelu                                 s/Bool
   :hakuajat                                   [{:hakuaika-id          s/Str
                                                 :start                org.joda.time.DateTime
                                                 (s/optional-key :end) org.joda.time.DateTime}]
   :haun-tiedot-url                            s/Str
   (s/optional-key :kohdejoukon-tarkenne-uri)  (s/maybe s/Str)
   (s/optional-key :alkamiskausi)              (s/maybe s/Str)
   (s/optional-key :alkamisvuosi)              (s/maybe s/Int)
   (s/optional-key :hakukausi-vuosi)           s/Int
   (s/optional-key :ataru-form-key)            s/Str
   (s/optional-key :max-hakukohteet)           s/Int
   (s/optional-key :valinnat-estetty-time-window) (s/maybe {:dateStart (s/maybe s/Int)
                                                            :dateEnd   (s/maybe s/Int)})
   (s/optional-key :maksullinen-kk-haku?) s/Bool})

(s/defschema Hakukohderyhma
  {:oid             s/Str
   :name            localized-schema/LocalizedStringOptional
   :hakukohderyhma? (s/eq true)
   :active?         s/Bool})

(s/defschema Hakukohde
  {:oid                                                                          s/Str
   :hakukohteen-tiedot-url                                                       s/Str
   :can-be-applied-to?                                                           s/Bool
   (s/optional-key :archived)                                                    (s/maybe s/Bool)
   :haku-oid                                                                     s/Str
   :koulutus-oids                                                                [s/Str]
   :name                                                                         localized-schema/LocalizedStringOptional
   :tarjoaja-name                                                                localized-schema/LocalizedStringOptional
   :tarjoaja-oids                                                                [s/Str]
   :ryhmaliitokset                                                               [s/Str]
   (s/optional-key :hakuaika-id)                                                 s/Str
   (s/optional-key :hakuajat)                                                    [{:start                org.joda.time.DateTime
                                                                                   (s/optional-key :end) org.joda.time.DateTime}]
   (s/optional-key :koulutustyyppikoodi)                                         (s/maybe s/Str)
   :hakukelpoisuusvaatimus-uris                                                  [s/Str]
   :ylioppilastutkinto-antaa-hakukelpoisuuden?                                   s/Bool
   ;; jyemp
   (s/optional-key :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja?) s/Bool
   (s/optional-key :yo-amm-autom-hakukelpoisuus)                                 s/Bool
   (s/optional-key :liitteet)                                                    [HakukohdeLiite]
   (s/optional-key :liitteet-onko-sama-toimitusosoite?)                          s/Bool
   (s/optional-key :liitteiden-toimitusosoite)                                   (s/maybe Toimitusosoite)
   (s/optional-key :liitteet-onko-sama-toimitusaika?)                            s/Bool
   (s/optional-key :liitteiden-toimitusaika)                                     (s/maybe localized-schema/LocalizedDateTime)
   (s/optional-key :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita?)     (s/maybe s/Bool)
   (s/optional-key :tutkintoon-johtava?)                                         s/Bool
   (s/optional-key :opetuskieli-koodi-urit)                                      [s/Str]})

(s/defschema HakukohdeSearchResult
  (assoc Hakukohde :user-organization? s/Bool))

(s/defschema HakukohdeSearchResultWithSelectionStateInfo
  (assoc HakukohdeSearchResult (s/optional-key :selection-state-used) (s/maybe s/Bool)))

(s/defschema Koodi
  {:uri                     s/Str
   :version                 s/Int
   :value                   s/Str
   :label                   localized-schema/LocalizedStringOptional
   :valid                   {(s/optional-key :start) java.time.ZonedDateTime
                             (s/optional-key :end)   java.time.ZonedDateTime}
   (s/optional-key :within) [(s/recursive #'Koodi)]})

(s/defschema Preview
  {:key          s/Str
   :content-type s/Str
   :size         s/Int
   :uploaded     #?(:clj  org.joda.time.DateTime
                    :cljs #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$")
   :deleted      (s/maybe #?(:clj  org.joda.time.DateTime
                             :cljs #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$"))})

(s/defschema File
  {:key                             s/Str
   :content-type                    s/Str
   :filename                        s/Str
   :size                            s/Int
   (s/optional-key :page-count)     (s/maybe s/Int)
   :virus-scan-status               s/Str
   :final                           s/Bool
   :uploaded                        #?(:clj  org.joda.time.DateTime
                                       :cljs #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$")
   (s/optional-key :deleted)        (s/maybe #?(:clj  org.joda.time.DateTime
                                                :cljs #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$"))
   (s/optional-key :preview-status) (s/enum "not_supported" "not_generated" "finished" "error" "started")
   (s/optional-key :previews)       [Preview]
   (s/optional-key :content-disposition) (s/maybe s/Str)})

(s/defschema FormWithContentAndTarjontaMetadata
  (merge FormWithContent {:tarjonta FormTarjontaMetadata}))

(defn- is-question-group-answer? [value]
  (and (vector? value)
       (not-empty value)
       (or (vector? (first value))
           (nil? (first value)))))

(s/defschema Value
  (s/conditional #(and (is-question-group-answer? %)
                       (some (fn [values] (some nil? values)) %))
                 [(s/maybe [(s/one (s/maybe s/Str) "single choice value")])]
                 #(and (is-question-group-answer? %)
                       (some (fn [values] (some map? values)) %))
                 [(s/maybe [File])]
                 is-question-group-answer?
                 [(s/maybe [s/Str])]
                 #(and (vector? %) (some map? %))
                 [File]
                 vector?
                 [s/Str]
                 :else
                 (s/maybe s/Str)))

(s/defschema Answer
  {:key                          s/Str
   :value                        Value
   :fieldType                    (apply s/enum form-fields)
   (s/optional-key :duplikoitu-kysymys-hakukohde-oid) (s/maybe s/Str)
   (s/optional-key :original-question) (s/maybe s/Str)
   (s/optional-key :cannot-view) s/Bool
   (s/optional-key :label)       (s/maybe (s/cond-pre
                                           localized-schema/LocalizedString
                                           s/Str))
   (s/optional-key :duplikoitu-followup-hakukohde-oid) (s/maybe s/Str)
   (s/optional-key :original-followup) (s/maybe s/Str)})

(def review-requirement-values
  (->> review-states/hakukohde-review-types
       (map last)
       (mapcat (partial map first))
       distinct))

(s/defschema HakukohdeReviewTypeNames
  (apply s/enum review-states/hakukohde-review-type-names))

;; Header-level info about application, doesn't contain the actual answers
(s/defschema ApplicationInfo
  {:id                                              s/Int
   :key                                             s/Str
   (s/optional-key :lang)                           s/Str
   :state                                           s/Str
   :score                                           (s/maybe s/Num)
   :new-application-modifications                   s/Int
   :person                                          {(s/optional-key :oid) s/Str
                                                     :preferred-name       s/Str
                                                     :last-name            s/Str
                                                     :yksiloity            s/Bool
                                                     (s/optional-key :ssn) s/Str
                                                     :dob                  s/Str}
   :submitted                                       org.joda.time.DateTime
   :base-education                                  [s/Str]
   (s/optional-key :form)                           s/Int
   (s/optional-key :created-time)                   org.joda.time.DateTime
   (s/optional-key :haku)                           (s/maybe s/Str)
   (s/optional-key :hakukohde)                      (s/maybe [s/Str])
   (s/optional-key :secret)                         s/Str
   (s/optional-key :application-hakukohde-reviews)  [{:requirement                    HakukohdeReviewTypeNames
                                                      :state                          (apply s/enum review-requirement-values)
                                                      (s/optional-key :modified-time) org.joda.time.DateTime
                                                      :hakukohde                      s/Str}] ; "form" or oid
   (s/optional-key :application-attachment-reviews) [{:attachment-key s/Str
                                                      :state          (apply s/enum review-states/attachment-review-type-names)
                                                      :hakukohde      s/Str}]
   :eligibility-set-automatically                   [s/Str]
   (s/optional-key :tunnistautuminen)               (s/maybe s/Str)
   (s/optional-key :kk-payment-state)               (s/maybe s/Str)})

(s/defschema Application
  {(s/optional-key :key)                s/Str
   :form                                s/Int
   (s/optional-key :lang)               s/Str
   :answers                             [Answer]
   (s/optional-key :applications-count) s/Int
   (s/optional-key :state)              (s/maybe s/Str)
   (s/optional-key :hakukohde)          (s/maybe [s/Str])
   (s/optional-key :haku)               (s/maybe s/Str)
   (s/optional-key :id)                 s/Int
   (s/optional-key :created-time)       org.joda.time.DateTime
   (s/optional-key :secret)             s/Str
   (s/optional-key :virkailija-secret)  s/Str
   (s/optional-key :selection-id)       s/Str
   (s/optional-key :form-key)           s/Str
   (s/optional-key :tarjonta)           FormTarjontaMetadata
   (s/optional-key :person-oid)         (s/maybe s/Str)
   (s/optional-key :strict-warnings-on-unchanged-edits?) (s/maybe s/Bool)
   (s/optional-key :tunnistautunut)     (s/maybe s/Bool)
   (s/optional-key :tunnistautuminen)   (s/maybe s/Str)
   (s/optional-key :save-koski-tutkinnot) (s/maybe s/Bool) })

(s/defschema Person
  {(s/optional-key :oid)         s/Str
   (s/optional-key :turvakielto) s/Bool
   (s/optional-key :yksiloity)   s/Bool
   :first-name                   s/Str
   :preferred-name               s/Str
   :last-name                    s/Str
   :nationality                  [(s/constrained [s/Str] #(= 1 (count %)))]
   (s/optional-key :birth-date)  s/Str
   (s/optional-key :gender)      s/Str
   (s/optional-key :language)    s/Str
   (s/optional-key :ssn)         s/Str
   (s/optional-key :minor)       s/Bool})

(s/defschema PaymentStatus
  {:application-key  s/Str
   :state            s/Str
   :reason           (s/maybe s/Str)
   :due-date         #?(:clj  (s/maybe DateTime)
                        :cljs (s/maybe s/Str))
   :total-sum        (s/maybe s/Str)
   :created-at       #?(:clj  (s/maybe DateTime)
                        :cljs (s/maybe s/Str))
   :modified-at      #?(:clj  (s/maybe DateTime)
                        :cljs (s/maybe s/Str))
   :required-at      #?(:clj  (s/maybe DateTime)
                        :cljs (s/maybe s/Str))
   :reminder-sent-at #?(:clj  (s/maybe DateTime)
                        :cljs (s/maybe s/Str))
   :approved-at      #?(:clj  (s/maybe DateTime)
                        :cljs (s/maybe s/Str))})

(s/defschema KkPaymentState
  {(s/optional-key :payment) PaymentStatus
   (s/optional-key :history) (s/maybe [PaymentStatus])})

(s/defschema ApplicationWithPerson
  (-> Application
      (st/dissoc :person-oid)
      (st/assoc :can-edit? s/Bool)
      (st/assoc :rights-by-hakukohde {s/Str [user-rights/Right]})
      (st/assoc :person Person)))

(s/defschema ApplicationWithPersonFormPaymentAndTutkinnot
  {:application (-> Application
                    (st/assoc (s/optional-key :application-identifier) s/Str)
                    (st/dissoc :person-oid)
                    (st/assoc :cannot-edit-because-in-processing s/Bool))
   :person      Person
   :form        (s/conditional #(contains? % :tarjonta) FormWithContentAndTarjontaMetadata
                               :else FormWithContent)
   :kk-payment  KkPaymentState
   (s/optional-key :koski-tutkinnot) [tutkinnot-schema/AtaruKoskiTutkinto]})

(s/defschema OmatsivutApplication
  {:oid s/Str
   :key s/Str
   :state s/Str
   :secret s/Str
   :haku (s/maybe s/Str)
   :email s/Str
   :hakukohteet [s/Str]
   :submitted org.joda.time.DateTime
   :form-name (s/maybe localized-schema/LocalizedStringOptional)})

(s/defschema Hakutoive
  {:processingState                s/Str
   :eligibilityState               s/Str
   :paymentObligation              s/Str
   :kkApplicationPaymentObligation s/Str
   :hakukohdeOid                   s/Str
   :languageRequirement            s/Str
   :degreeRequirement              s/Str})

(s/defschema ValintaHakutoive
  (merge Hakutoive
         {(s/optional-key :harkinnanvaraisuus) (apply s/enum harkinnanvaraisuus-types)}))

(s/defschema HakutoiveHarkinnanvaraisuudella
  {:hakukohdeOid s/Str
   :harkinnanvaraisuudenSyy (apply s/enum harkinnanvaraisuus-types)})

(s/defschema VtsApplication
  {:oid              s/Str ; (:key application)
   :hakuOid          s/Str
   :henkiloOid       s/Str
   :asiointikieli    s/Str
   :lahiosoite       s/Str
   :postinumero      s/Str
   :postitoimipaikka s/Str
   :maa              s/Str
   :hakutoiveet      [Hakutoive]
   :email            (s/maybe s/Str)})

(s/defschema ValintaUiApplication
  {:oid           s/Str
   :hakuOid       s/Str
   :personOid     s/Str
   :sukunimi      s/Str
   :etunimet      s/Str
   :asiointiKieli {:kieliKoodi  s/Str
                   :kieliTyyppi s/Str}
   :henkilotunnus (s/maybe s/Str)
   :lahiosoite    s/Str
   :postinumero   s/Str
   :hakutoiveet   [Hakutoive]})

(s/defschema HakurekisteriApplication
  {:oid                         s/Str
   :personOid                   s/Str
   :createdTime                 s/Str
   :hakemusFirstSubmittedTime   s/Str
   :applicationSystemId         s/Str
   :kieli                       s/Str
   :hakukohteet                 [s/Str]
   :email                       s/Str
   :matkapuhelin                s/Str
   :lahiosoite                  s/Str
   :postinumero                 s/Str
   :postitoimipaikka            (s/maybe s/Str)
   :asuinmaa                    s/Str
   :kotikunta                   (s/maybe s/Str)
   :paymentObligations          {s/Str s/Str}
   :attachments                 {s/Str s/Str}
   :eligibilities               {s/Str s/Str}
   :kkPohjakoulutus             [s/Str]
   :kkPohjakoulutusLomake       [s/Str]
   :sahkoisenAsioinninLupa      s/Bool
   :valintatuloksenJulkaisulupa s/Bool
   :koulutusmarkkinointilupa    s/Bool
   :korkeakoulututkintoVuosi    (s/maybe s/Int)})

(s/defschema ApplicationPersonInfo
  {:oid       s/Str
   :ssn       (s/maybe s/Str)
   :personOid (s/maybe s/Str)})

(s/defschema HakurekisteriHakukohde
  {:oid                            s/Str
   :harkinnanvaraisuus             (apply s/enum harkinnanvaraisuus-types)
   :terveys                        (s/maybe s/Bool)
   :aiempiPeruminen                (s/maybe s/Bool)
   :kiinnostunutKaksoistutkinnosta (s/maybe s/Bool)
   :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta (s/maybe s/Bool)})

(s/defschema GuardianContactInfo
              {:etunimi (s/maybe s/Str)
               :sukunimi (s/maybe s/Str)
               :matkapuhelin (s/maybe s/Str)
               :email (s/maybe s/Str)})

(s/defschema UrheilijanLisakysymykset
  {:keskiarvo                   (s/maybe s/Str)
   :peruskoulu                  (s/maybe s/Str)
   :tamakausi                   (s/maybe s/Str)
   :viimekausi                  (s/maybe s/Str)
   :toissakausi                 (s/maybe s/Str)
   :sivulaji                    (s/maybe s/Str)
   :valmennusryhma_seurajoukkue (s/maybe s/Str)
   :valmennusryhma_piirijoukkue (s/maybe s/Str)
   :valmennusryhma_maajoukkue   (s/maybe s/Str)
   :valmentaja_nimi             (s/maybe s/Str)
   :valmentaja_email            (s/maybe s/Str)
   :valmentaja_puh              (s/maybe s/Str)
   :laji                        (s/maybe s/Str)
   :seura                       (s/maybe s/Str)
   :liitto                      (s/maybe s/Str)})

(s/defschema HakurekisteriApplicationToinenAste
  {:oid                                  s/Str
   :personOid                            s/Str
   :createdTime                          s/Str
   :kieli                                s/Str
   :hakukohteet                          [HakurekisteriHakukohde]
   :email                                s/Str
   :matkapuhelin                         s/Str
   :lahiosoite                           s/Str
   :postinumero                          s/Str
   :postitoimipaikka                     (s/maybe s/Str)
   :asuinmaa                             s/Str
   :kotikunta                            (s/maybe s/Str)
   :attachments                          {s/Str s/Str}
   :pohjakoulutus                        s/Str
   :kiinnostunutOppisopimusKoulutuksesta (s/maybe s/Bool)
   :sahkoisenAsioinninLupa               s/Bool
   :valintatuloksenJulkaisulupa          s/Bool
   :koulutusmarkkinointilupa             s/Bool
   :tutkintoVuosi                        (s/maybe s/Int)
   :tutkintoKieli                        (s/maybe s/Str)
   :huoltajat                            [GuardianContactInfo]
   :hakemusFirstSubmittedTime            s/Str
   :urheilijanLisakysymykset             (s/maybe UrheilijanLisakysymykset)
   :urheilijanLisakysymyksetAmmatillinen (s/maybe UrheilijanLisakysymykset)})

(s/defschema OnrApplication
  {:oid          s/Str
   :henkiloOid    s/Str
   :haku         (s/maybe s/Str)
   :form         s/Str
   :kansalaisuus [s/Str]
   :aidinkieli   s/Str
   :matkapuhelin s/Str
   :email        s/Str
   :lahiosoite   s/Str
   :postinumero  s/Str
   :passinNumero (s/maybe s/Str)
   :idTunnus     (s/maybe s/Str)})

(s/defschema TilastokeskusApplication
  {:hakemus_oid                       s/Str
   :hakemus_tila                      s/Str
   :haku_oid                          s/Str
   :henkilo_oid                       s/Str
   :hakukohde_oids                    [s/Str]
   :kotikunta                         (s/maybe s/Str)
   :asuinmaa                          (s/maybe s/Str)
   :submitted                         org.joda.time.DateTime
   :pohjakoulutus_kk                  [{:pohjakoulutuskklomake          s/Str
                                        (s/optional-key :suoritusvuosi) s/Int}]
   :pohjakoulutus_kk_ulk_country      (s/maybe s/Str)
   (s/optional-key :pohjakoulutus_2aste)               (s/maybe s/Str)
   (s/optional-key :pohjakoulutus_2aste_suorituskieli) (s/maybe s/Str)
   (s/optional-key :pohjakoulutus_2aste_lahtokoulu_oid) (s/maybe s/Str)
   :hakutoiveet                       [{:hakukohde_oid (s/maybe s/Str)
                                        :sija          s/Int
                                        :harkinnanvaraisuuden_syy (s/maybe s/Str)}]})

(s/defschema ValintaApplication
  {:hakemusOid          s/Str
   :personOid           s/Str
   :hakuOid             s/Str
   :asiointikieli       (s/enum "fi" "sv" "en")
   :hakutoiveet         [ValintaHakutoive]
   :maksuvelvollisuus   {s/Str s/Str}
   :keyValues           {s/Str Value}})

(s/defschema ValintapisteApplication
  {:hakemus_oid         s/Str
   :henkilo_oid         s/Str
   :hakemus_tila        s/Str
   :haku_oid            s/Str
   :hakukohde_oids      [s/Str]})

(s/defschema Kieli
  {:kieliKoodi  s/Str
   :kieliTyyppi s/Str})

(s/defschema SiirtoApplication
  {:hakemusOid  s/Str
   :personOid   s/Str
   :person      {:oidHenkilo    s/Str
                 :etunimet      s/Str
                 :syntymaaika   s/Str
                 :hetu          (s/maybe s/Str)
                 :sukunimi      s/Str
                 :turvakielto   s/Bool
                 :sukupuoli     s/Str
                 :aidinkieli    (s/maybe Kieli)
                 :kansalaisuudet [s/Str]
                 :kutsumanimi  (s/maybe s/Str)
                 :asiointiKieli Kieli}
   :hakuOid     (s/maybe s/Str)
   :hakutoiveet [s/Str]
   :attachments {s/Str Value}
   :keyValues   {s/Str Value}
   :state        s/Str
   :originallySubmitted s/Str
   :versionCreated      s/Str
   :versionModified     s/Str
   :hakukohdeReviews             [{:requirement                    s/Str
                                   :state                          s/Str
                                   (s/optional-key :modified-time) org.joda.time.DateTime
                                   :hakukohde                      s/Str}]
   :hakukohdeAttachmentReviews   [{:attachment    s/Str
                                   :state         s/Str
                                   :hakukohde     s/Str}]
   :applicationReviewNotes [{:notes s/Str
                             :state (s/maybe s/Str)
                             :hakukohde (s/maybe s/Str)
                             :virkailijaOid (s/maybe s/Str)
                             :created org.joda.time.DateTime}]
   :applicationPaymentStates [{:state (s/maybe s/Str)
                               :reason (s/maybe s/Str)
                               :dueDate (s/maybe s/Str)
                               :total (s/maybe s/Num)
                               :modified org.joda.time.DateTime
                               :reminderSentAt (s/maybe org.joda.time.DateTime)}]})

(def event-types (s/enum "updated-by-applicant"
                         "updated-by-virkailija"
                         "received-from-applicant"
                         "received-from-virkailija"
                         "review-state-change"
                         "hakukohde-review-state-change"
                         "eligibility-state-automatically-changed"
                         "payment-obligation-automatically-changed"
                         "attachment-review-state-change"
                         "modification-link-sent"
                         "kk-application-payment-reminder-email-sent"
                         "kk-application-payment-email-resent"
                         "field-deadline-set"
                         "field-deadline-unset"
                         "person-found-matching"
                         "person-dob-or-gender-conflict"
                         "information-request-reminder-sent"
                         "decision-payment-reminder-sent"))

(s/defschema Event
  {:event-type                                event-types
   :time                                      org.joda.time.DateTime
   :id                                        s/Int
   :application-key                           s/Str
   (s/optional-key :new-review-state)         (s/maybe s/Str)
   (s/optional-key :hakukohde)                (s/maybe s/Str)
   (s/optional-key :review-key)               (s/maybe s/Str)
   :first-name                                (s/maybe s/Str)
   :last-name                                 (s/maybe s/Str)
   (s/optional-key :virkailija-organizations) [{:oid                              s/Str
                                                :name                             localized-schema/LocalizedStringOptional
                                                :type                             (s/enum :organization :group)
                                                (s/optional-key :hakukohderyhma?) s/Bool
                                                (s/optional-key :active?)         s/Bool}]})

(def hakukohde-review-types-schema
  (reduce (fn [acc [kw _ states]]
            (assoc acc (s/optional-key kw) (apply s/enum (map first states))))
          {}
          review-states/hakukohde-review-types))

(s/defschema HakukohdeReviews
  {s/Keyword hakukohde-review-types-schema})

(s/defschema AttachmentReviews
  {s/Keyword {s/Keyword (apply s/enum review-states/attachment-review-type-names)}})

(s/defschema FieldDeadline
  {:field-id s/Str
   :deadline org.joda.time.DateTime})

(s/defschema ReviewNote
  {:id                                        s/Int
   :application-key                           s/Str
   :notes                                     s/Str
   :first-name                                (s/maybe s/Str)
   :last-name                                 (s/maybe s/Str)
   (s/optional-key :virkailija-organizations) [{:oid                              s/Str
                                                :name                             localized-schema/LocalizedStringOptional
                                                :type                             (s/enum :organization :group)
                                                (s/optional-key :hakukohderyhma?) s/Bool
                                                (s/optional-key :active?)         s/Bool}]
   (s/optional-key :hakukohde)                (s/maybe s/Str)
   (s/optional-key :state-name)               HakukohdeReviewTypeNames
   (s/optional-key :created-time)             org.joda.time.DateTime})

(s/defschema Review
  {:id                                  s/Int
   :application-key                     s/Str
   (s/optional-key :modified-time)      org.joda.time.DateTime
   :state                               s/Str
   (s/optional-key :score)              (s/maybe s/Num)
   (s/optional-key :hakukohde-reviews)  HakukohdeReviews
   (s/optional-key :attachment-reviews) AttachmentReviews})

(s/defschema ApplicationCountsHakukohde {:oid               s/Str
                                         :application-count s/Int
                                         :processed         s/Int
                                         :unprocessed       s/Int})

(s/defschema TarjontaHaku {:oid                    s/Str
                           :haku-application-count s/Int
                           :application-count      s/Int
                           :processed              s/Int
                           :unprocessed            s/Int
                           :hakukohteet            [ApplicationCountsHakukohde]})

(s/defschema DirectFormHaku {:name                   localized-schema/LocalizedStringOptional
                             :key                    s/Str
                             :haku-application-count s/Int
                             :application-count      s/Int
                             :processed              s/Int
                             :unprocessed            s/Int})

(s/defschema Haut {:tarjonta-haut    {s/Str TarjontaHaku}
                   :direct-form-haut {s/Str DirectFormHaku}
                   :haut             {s/Str Haku}
                   :hakukohteet      {s/Str HakukohdeSearchResultWithSelectionStateInfo}
                   :hakukohderyhmat  {s/Str Hakukohderyhma}})

(s/defschema ApplicationFeedback {:form-key   s/Str
                                  :form-id    s/Int
                                  :form-name  s/Str
                                  :user-agent s/Str
                                  :rating     s/Int
                                  :haku-oid   (s/maybe s/Str)
                                  :feedback   (s/maybe s/Str)})
(s/defschema HasAppliedParams {:haku-oid s/Str
                               (s/optional-key :ssn) (s/maybe s/Str)
                               (s/optional-key :email) (s/maybe s/Str)
                               (s/optional-key :eidas-id) (s/maybe s/Str)})

(s/defschema PermissionCheckDto {:personOidsForSamePerson          [s/Str]
                                 :organisationOids                 [s/Str]
                                 :loggedInUserRoles                [s/Str]
                                 (s/optional-key :loggedInUserOid) s/Str})

(s/defschema PermissionCheckResponseDto {:accessAllowed s/Bool
                                         (s/optional-key :errorMessage) s/Str})

(defn- length-at-most-200 [s]
  (<= (count s) 200))

(s/defschema NewInformationRequest (s/constrained
                                     {:subject         (s/constrained s/Str length-at-most-200)
                                      (s/optional-key :recipient-target) s/Str
                                      :message         s/Str
                                      :application-key s/Str
                                      :add-update-link s/Bool
                                      (s/optional-key :single-message) s/Bool
                                      (s/optional-key :send-reminder?) s/Bool
                                      (s/optional-key :reminder-days) (s/maybe s/Int)}
                                     (fn [{:keys [send-reminder? reminder-days]}]
                                       (or (and send-reminder? (pos? reminder-days))
                                           (and (not send-reminder?) (nil? reminder-days))))
                                     'reminder-days-only-when-send-reminder-true))

(s/defschema InformationRequest {:id              s/Num
                                 :subject         s/Str
                                 (s/optional-key :recipient-target) s/Str
                                 :message         s/Str
                                 :application-key s/Str
                                 :message-type    s/Str
                                 :created-time    #?(:clj  org.joda.time.DateTime
                                                     :cljs s/Str)
                                 :first-name      s/Str
                                 :last-name       s/Str
                                 :send-reminder-time #?(:clj  (s/maybe org.joda.time.DateTime)
                                                        :cljs (s/maybe s/Str))})

(s/defschema ReviewSetting {:setting-kwd s/Str
                            :enabled     s/Bool})

(s/defschema VirkailijaSettings {:review {s/Keyword s/Bool}})

(def form-coercion-matchers {module-schema/Module                  keyword
                             child-validator-schema/ChildValidator keyword
                             validator-schema/Validator            keyword})

(def form-coercer (c/coercer! FormWithContent form-coercion-matchers))

(s/defschema EmailTemplate {:lang           (s/enum "fi" "sv" "en")
                            :content        s/Str
                            :content-ending s/Str
                            :signature      s/Str
                            :subject        (s/constrained s/Str (comp not string/blank?))})

(s/defschema Sort
  (s/conditional #(= "applicant-name" (:order-by %))
                 {:order-by                (s/eq "applicant-name")
                  :order                   (s/enum "asc" "desc")
                  (s/optional-key :offset) {:key            s/Str
                                            :last-name      s/Str
                                            :preferred-name s/Str}}
                 #(= "created-time" (:order-by %))
                 {:order-by                (s/eq "created-time")
                  :order                   (s/enum "asc" "desc")
                  (s/optional-key :offset) {:key          s/Str
                                            :created-time org.joda.time.DateTime}}
                 #(= "submitted" (:order-by %))
                 {:order-by                (s/eq "submitted")
                  :order                   (s/enum "asc" "desc")
                  (s/optional-key :offset) {:key       s/Str
                                            :submitted org.joda.time.DateTime}}))

(s/defschema QueryAttachmentReviewStates
  {s/Keyword
   (into
    {}
    (map (fn [[state _]] [(keyword state) s/Bool])
         review-states/attachment-hakukohde-review-types))})

(defn- length-less-than-1000 [s]
  (< (count s) 1000))

(s/defschema OptionAnswers
  {:key                                     s/Str
   :options                                 [(s/maybe (s/constrained s/Str length-less-than-1000))]
   (s/optional-key :use-original-question)  (s/maybe s/Bool)
   (s/optional-key :use-original-followup)  (s/maybe s/Bool)})

(s/defschema ApplicationQuery
  {(s/optional-key :form-key)             s/Str
   (s/optional-key :hakukohde-oid)        s/Str
   (s/optional-key :hakukohderyhma-oid)   s/Str
   (s/optional-key :haku-oid)             s/Str
   (s/optional-key :ensisijaisesti)       s/Bool
   (s/optional-key :rajaus-hakukohteella) s/Str
   (s/optional-key :ssn)                  s/Str
   (s/optional-key :dob)                  s/Str
   (s/optional-key :email)                s/Str
   (s/optional-key :name)                 s/Str
   (s/optional-key :person-oid)           s/Str
   (s/optional-key :application-oid)      s/Str
   :attachment-review-states              QueryAttachmentReviewStates
   (s/optional-key :option-answers)       [OptionAnswers]
   :sort                                  Sort
   (s/optional-key :states-and-filters)   {:filters                             {s/Keyword (s/conditional map? {s/Keyword s/Any} :else s/Bool)}
                                           :attachment-states-to-include        [s/Str]
                                           :processing-states-to-include        [s/Str]
                                           (s/optional-key :school-filter)      (s/maybe s/Str)
                                           (s/optional-key :classes-of-school)  (s/maybe [s/Str])}})

(s/defschema ApplicationQueryResponse
  {:sort         Sort
   :applications [ApplicationInfo]})

(s/defschema KayttaaValintalaskentaaResponse
  {:hakukohde-oid   s/Str
   :valintalaskenta s/Bool})

(s/defschema HakukohderyhmaSettings
  {:rajaava                                                    s/Bool
   :max-hakukohteet                                            (s/maybe s/Int)
   (s/optional-key :priorisoiva)                               s/Bool
   (s/optional-key :prioriteettijarjestys)                     [s/Str]
   :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja s/Bool
   :yo-amm-autom-hakukelpoisuus                                s/Bool
   s/Any                                                       s/Any})

(s/defschema OpiskelijaLuokkatietoResponse
  {:oppilaitos-name localized-schema/LocalizedStringOptional
   :luokka s/Str})

(s/defschema Arvosana
  {:key                          s/Keyword
   :value                        s/Str
   :label                        localized-schema/LocalizedString
   (s/optional-key :lang)        (s/maybe localized-schema/LocalizedString)
   (s/optional-key :valinnaiset) [s/Str]})

(s/defschema PohjakoulutusResponse
  {(s/optional-key :pohjakoulutus)        {:value s/Str
                                           :label localized-schema/LocalizedString}
   (s/optional-key :opetuskieli)          {:value s/Str
                                           :label localized-schema/LocalizedString}
   (s/optional-key :suoritusvuosi)        s/Str
   (s/optional-key :lisapistekoulutukset) [s/Keyword]
   (s/optional-key :arvosanat)            [Arvosana]})

(s/defschema SyntheticApplication
  {(s/optional-key :personOid)      s/Str
   (s/optional-key :applicationOid) s/Str

   :hakuOid          s/Str
   :hakukohdeOid     s/Str
   :sukunimi         s/Str
   :etunimi          s/Str
   :kutsumanimi      s/Str
   :kansalaisuus     s/Str
   :syntymaAika      (s/maybe s/Str)
   :syntymapaikka    (s/maybe s/Str)
   :henkilotunnus    (s/maybe s/Str)
   :sukupuoli        (s/maybe s/Str)
   :passinNumero     (s/maybe s/Str)
   :idTunnus         (s/maybe s/Str)
   :sahkoposti       s/Str
   :puhelinnumero    s/Str
   :asuinmaa         s/Str
   :osoite           s/Str
   :postinumero      s/Str
   :postitoimipaikka (s/maybe s/Str)
   :kaupunkiJaMaa    (s/maybe s/Str)
   :kotikunta        (s/maybe s/Str)
   :aidinkieli       s/Str
   :asiointikieli    s/Str

   :toisenAsteenSuoritus    s/Str
   :toisenAsteenSuoritusmaa (s/maybe s/Str)})

(s/defschema FormPaymentInfo
  {:paymentType                    (s/maybe s/Str)
   (s/optional-key :processingFee) s/Str
   (s/optional-key :decisionFee)   s/Str})
