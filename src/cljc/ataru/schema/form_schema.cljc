(ns ataru.schema.form-schema
  (:require [ataru.hakija.application-validators :as validator]
            [schema.core :as s]
            [schema-tools.core :as st]
            [clojure.string :as str]))

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

(s/defschema Form {(s/optional-key :id)                s/Int
                   :name                               s/Str
                   :content                            (s/pred empty?)
                   (s/optional-key :languages)         [s/Str]
                   (s/optional-key :key)               s/Str
                   (s/optional-key :created-by)        s/Str
                   (s/optional-key :created-time)      #?(:clj  org.joda.time.DateTime
                                                          :cljs s/Str)
                   (s/optional-key :application-count) s/Int
                   (s/optional-key :deleted)           (s/maybe s/Bool)})

(s/defschema Haku {:haku              s/Str
                   :haku-name         s/Str
                   :application-count s/Int})

(s/defschema LocalizedString {:fi                  s/Str
                              (s/optional-key :sv) s/Str
                              (s/optional-key :en) s/Str})

(s/defschema Module (s/enum :person-info))

(s/defschema Button {:fieldClass              (s/eq "button")
                     :id                      s/Str
                     (s/optional-key :label)  LocalizedString
                     (s/optional-key :params) s/Any
                     :fieldType               s/Keyword})

(s/defschema FormField {:fieldClass                            (s/eq "formField")
                        :id                                    s/Str
                        (s/optional-key :validators)           [(apply s/enum (keys validator/validators))]
                        (s/optional-key :rules)                {s/Keyword s/Any}
                        (s/optional-key :label)                LocalizedString
                        (s/optional-key :initialValue)         (s/cond-pre LocalizedString s/Int)
                        (s/optional-key :params)               s/Any
                        (s/optional-key :no-blank-option)      s/Bool
                        (s/optional-key :exclude-from-answers) s/Bool
                        (s/optional-key :koodisto-source)      {:uri                             s/Str
                                                                :version                         s/Int
                                                                (s/optional-key :default-option) s/Any
                                                                (s/optional-key :title)          s/Str}
                        (s/optional-key :options)              [{:value                          s/Str
                                                                 (s/optional-key :label)         LocalizedString
                                                                 (s/optional-key :default-value) (s/maybe s/Bool)
                                                                 (s/optional-key :followups)     [(s/if (comp some? :children) (s/recursive #'WrapperElement) (s/recursive #'BasicElement))]}]
                        :fieldType                             (apply s/enum ["textField"
                                                                              "textArea"
                                                                              "dropdown"
                                                                              "singleChoice"
                                                                              "multipleChoice"
                                                                              "koodistoField"])})

(s/defschema InfoElement {:fieldClass              (s/eq "infoElement")
                          :id                      s/Str
                          :fieldType               (apply s/enum ["h1"
                                                                  "h3"
                                                                  "link"
                                                                  "p"
                                                                  "bulletList"
                                                                  "dateRange"
                                                                  "endOfDateRange"])
                          (s/optional-key :params) s/Any
                          (s/optional-key :label)  LocalizedString
                          (s/optional-key :text)   LocalizedString})

(s/defschema BasicElement (s/conditional
                            #(= "formField" (:fieldClass %)) FormField
                            #(= "button" (:fieldClass %)) Button
                            :else InfoElement))

(s/defschema WrapperElement {:fieldClass                       (apply s/enum ["wrapperElement"])
                             :id                               s/Str
                             :fieldType                        (apply s/enum ["fieldset" "rowcontainer" "adjacentfieldset"])
                             :children                         [(s/conditional #(= "wrapperElement" (:fieldClass %))
                                                                               (s/recursive #'WrapperElement)
                                                                               :else
                                                                               BasicElement)]
                             (s/optional-key :child-validator) (s/enum :one-of)
                             (s/optional-key :params)          s/Any
                             (s/optional-key :label)           LocalizedString
                             (s/optional-key :label-amendment) LocalizedString ; Additional info which can be displayed next to the label
                             (s/optional-key :module)          Module})

(s/defschema FormWithContent
  (merge Form
         {:content                           [(s/if (comp some? :children) WrapperElement BasicElement)]
          (s/optional-key :organization-oid) (s/maybe s/Str)}))

(s/defschema FormTarjontaMetadata
  {:hakukohde-oid                       s/Str
   :hakukohde-name                      s/Str
   :haku-oid                            s/Str
   :haku-name                           s/Str
   (s/optional-key :koulutukset)        [{:oid                  s/Str
                                          :koulutuskoodi-name   (s/maybe s/Str)
                                          :tutkintonimike-name  (s/maybe s/Str)
                                          :koulutusohjelma-name (s/maybe s/Str)
                                          :tarkenne             (s/maybe s/Str)}]
   (s/optional-key :haku-tarjoaja-name) (s/maybe s/Str)
   (s/optional-key :hakuaika-dates)     {:start                s/Int
                                         (s/optional-key :end) (s/maybe s/Int)
                                         :on                   s/Bool}})

(s/defschema FormWithContentAndTarjontaMetadata
  (merge FormWithContent {:tarjonta FormTarjontaMetadata}))

(s/defschema Answer {:key                    s/Str,
                     :value                  (s/cond-pre s/Str
                                                         s/Int
                                                         [s/Str])
                     :fieldType              (apply s/enum ["textField"
                                                            "textArea"
                                                            "dropdown"
                                                            "multipleChoice"
                                                            "singleChoice"])
                     (s/optional-key :label) (s/maybe (s/cond-pre
                                                        LocalizedString
                                                        s/Str))})

;; Header-level info about application, doesn't contain the actual answers
(s/defschema ApplicationInfo
  {:id                              s/Int
   :key                             s/Str
   :lang                            s/Str
   :state                           s/Str
   :score                           (s/maybe s/Int)
   (s/optional-key :form)           s/Int
   (s/optional-key :applicant-name) (s/maybe s/Str)
   (s/optional-key :created-time)   org.joda.time.DateTime})

(s/defschema Application
  {(s/optional-key :key)          s/Str
   :form                          s/Int
   :lang                          s/Str
   :answers                       [Answer]
   (s/optional-key :hakukohde)    s/Str
   (s/optional-key :id)           s/Int
   (s/optional-key :created-time) org.joda.time.DateTime
   (s/optional-key :secret)       s/Str
   (s/optional-key :form-key)     s/Str
   (s/optional-key :tarjonta)     FormTarjontaMetadata})

(def application-states (s/enum "unprocessed"
                                "processing"
                                "invited-to-interview"
                                "invited-to-exam"
                                "not-selected"
                                "selected"
                                "applicant-has-accepted"
                                "rejected"
                                "canceled"))

(def event-types (s/enum "updated-by-applicant"
                         "received-from-applicant"
                         "review-state-change"))

(s/defschema Event
  {:event-type                        event-types
   :time                              org.joda.time.DateTime
   :id                                s/Int
   :application-key                   s/Str
   (s/optional-key :new-review-state) (s/maybe application-states)})

(s/defschema Review
  {:id                             s/Int
   :application-key                s/Str
   (s/optional-key :modified-time) org.joda.time.DateTime
   :state                          application-states
   (s/optional-key :score)         (s/maybe s/Int)
   :notes                          (s/maybe s/Str)})
