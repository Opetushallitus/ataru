(ns ataru.schema.form-schema
  (:require [ataru.application.review-states :as review-states]
            [ataru.hakija.application-validators :as validator]
            [schema.core :as s]))

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

(s/defschema LocalizedString {:fi                  s/Str
                              (s/optional-key :sv) s/Str
                              (s/optional-key :en) s/Str})

(s/defschema LocalizedStringOptional {(s/optional-key :fi) s/Str
                                      (s/optional-key :sv) s/Str
                                      (s/optional-key :en) s/Str})

(s/defschema Module (s/enum :person-info))

(s/defschema Button {:fieldClass                              (s/eq "button")
                     :id                                      s/Str
                     (s/optional-key :label)                  LocalizedString
                     (s/optional-key :params)                 s/Any
                     :fieldType                               s/Keyword
                     (s/optional-key :belongs-to-hakukohteet) (s/maybe [s/Str])})

(s/defschema FormField {:fieldClass                                      (s/eq "formField")
                        :id                                              s/Str
                        (s/optional-key :validators)                     [(apply s/enum (keys validator/validators))]
                        (s/optional-key :rules)                          {s/Keyword s/Any}
                        (s/optional-key :blur-rules)                     {s/Keyword s/Any}
                        (s/optional-key :label)                          LocalizedString
                        (s/optional-key :label-amendment)                LocalizedString
                        (s/optional-key :initialValue)                   (s/cond-pre LocalizedString s/Int)
                        (s/optional-key :params)                         s/Any
                        (s/optional-key :no-blank-option)                s/Bool
                        (s/optional-key :exclude-from-answers)           s/Bool
                        (s/optional-key :exclude-from-answers-if-hidden) s/Bool
                        (s/optional-key :koodisto-source)                {:uri                             s/Str
                                                                          :version                         s/Int
                                                                          (s/optional-key :default-option) s/Any
                                                                          (s/optional-key :title)          s/Str}
                        (s/optional-key :options)                        [{:value                          s/Str
                                                                           (s/optional-key :label)         LocalizedString
                                                                           (s/optional-key :description)   LocalizedString
                                                                           (s/optional-key :default-value) (s/maybe s/Bool)
                                                                           (s/optional-key :followups)     [(s/if (comp some? :children) (s/recursive #'WrapperElement) (s/recursive #'BasicElement))]}]
                        :fieldType                                       (apply s/enum ["textField"
                                                                                        "textArea"
                                                                                        "dropdown"
                                                                                        "singleChoice"
                                                                                        "multipleChoice"
                                                                                        "koodistoField"
                                                                                        "attachment"
                                                                                        "hakukohteet"])
                        (s/optional-key :belongs-to-hakukohteet)         (s/maybe [s/Str])})

(s/defschema InfoElement {:fieldClass                              (s/eq "infoElement")
                          :id                                      s/Str
                          :fieldType                               (apply s/enum ["h1"
                                                                                  "h3"
                                                                                  "link"
                                                                                  "p"
                                                                                  "bulletList"
                                                                                  "dateRange"
                                                                                  "endOfDateRange"])
                          (s/optional-key :params)                 s/Any
                          (s/optional-key :label)                  LocalizedString
                          (s/optional-key :text)                   LocalizedString
                          (s/optional-key :belongs-to-hakukohteet) (s/maybe [s/Str])})

(s/defschema BasicElement (s/conditional
                            #(= "formField" (:fieldClass %)) FormField
                            #(= "button" (:fieldClass %)) Button
                            :else InfoElement))

(s/defschema WrapperElement {:fieldClass                              (apply s/enum ["wrapperElement"])
                             :id                                      s/Str
                             :fieldType                               (apply s/enum ["fieldset" "rowcontainer" "adjacentfieldset"])
                             :children                                [(s/conditional #(= "wrapperElement" (:fieldClass %))
                                                                                      (s/recursive #'WrapperElement)
                                                                                      :else
                                                                                      BasicElement)]
                             (s/optional-key :child-validator)        (s/enum :one-of :birthdate-and-gender-component)
                             (s/optional-key :params)                 s/Any
                             (s/optional-key :label)                  LocalizedString
                             (s/optional-key :label-amendment)        LocalizedString ; Additional info which can be displayed next to the label
                             (s/optional-key :module)                 Module
                             (s/optional-key :belongs-to-hakukohteet) (s/maybe [s/Str])})

(s/defschema FormWithContent
  (merge Form
         {:content                           [(s/if (comp some? :children) WrapperElement BasicElement)]
          (s/optional-key :organization-oid) (s/maybe s/Str)}))

(s/defschema FormTarjontaHakukohde
  {:oid                          s/Str
   :name                         LocalizedStringOptional
   :tarjoaja-name                LocalizedStringOptional
   (s/optional-key :form-key)    (s/maybe s/Str)
   (s/optional-key :koulutukset) [{:oid                  s/Str
                                   :koulutuskoodi-name   LocalizedStringOptional
                                   :tutkintonimike-name  LocalizedStringOptional
                                   :tarkenne             (s/maybe s/Str)}]})

(s/defschema FormTarjontaMetadata
  {:hakukohteet                        [FormTarjontaHakukohde]
   :haku-oid                           s/Str
   :haku-name                          s/Str
   :max-hakukohteet                    (s/maybe s/Int)
   (s/optional-key :default-hakukohde) FormTarjontaHakukohde
   (s/optional-key :hakuaika-dates)    {:start                s/Int
                                        (s/optional-key :end) (s/maybe s/Int)
                                        :on                   s/Bool}})

(s/defschema File
  {:key                      s/Str
   :content-type             s/Str
   :filename                 s/Str
   :size                     s/Int
   :virus-scan-status        s/Str
   :final                    s/Bool
   :uploaded                 #?(:clj  org.joda.time.DateTime
                                :cljs #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$")
   (s/optional-key :deleted) (s/maybe #?(:clj  org.joda.time.DateTime
                                         :cljs #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$"))})

(s/defschema FormWithContentAndTarjontaMetadata
  (merge FormWithContent {:tarjonta FormTarjontaMetadata}))

(s/defschema Answer {:key                          s/Str,
                     :value                        (s/cond-pre s/Str
                                                               s/Int
                                                               [(s/cond-pre s/Str File)])
                     :fieldType                    (apply s/enum ["textField"
                                                                  "textArea"
                                                                  "dropdown"
                                                                  "multipleChoice"
                                                                  "singleChoice"
                                                                  "attachment"
                                                                  "hakukohteet"])
                     (s/optional-key :cannot-edit) s/Bool
                     (s/optional-key :cannot-view) s/Bool
                     (s/optional-key :label)       (s/maybe (s/cond-pre
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
   (s/optional-key :preferred-name) (s/maybe s/Str)
   (s/optional-key :last-name)      (s/maybe s/Str)
   (s/optional-key :created-time)   org.joda.time.DateTime
   (s/optional-key :haku)           (s/maybe s/Str)
   (s/optional-key :secret)         s/Str})

(s/defschema Application
  {(s/optional-key :key)                s/Str
   :form                                s/Int
   :lang                                s/Str
   :answers                             [Answer]
   (s/optional-key :applications-count) s/Int
   (s/optional-key :state)              (s/maybe s/Str)
   (s/optional-key :hakukohde)          (s/maybe [s/Str])
   (s/optional-key :haku)               (s/maybe s/Str)
   (s/optional-key :id)                 s/Int
   (s/optional-key :created-time)       org.joda.time.DateTime
   (s/optional-key :secret)             s/Str
   (s/optional-key :form-key)           s/Str
   (s/optional-key :tarjonta)           FormTarjontaMetadata
   (s/optional-key :person-oid)         (s/maybe s/Str)})

(def application-states
  (apply s/enum (keys review-states/application-review-states)))

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

(s/defschema Hakukohde {:oid               s/Str
                        :name              s/Str
                        :application-count s/Int
                        :unprocessed       s/Int
                        :incomplete        s/Int})

(s/defschema TarjontaHaku {:oid               s/Str
                           :name              s/Str
                           :application-count s/Int
                           :unprocessed       s/Int
                           :incomplete        s/Int
                           :hakukohteet       [Hakukohde]})

(s/defschema DirectFormHaku {:name              s/Str
                             :key               s/Str
                             :application-count s/Int
                             :unprocessed       s/Int
                             :incomplete        s/Int})

(s/defschema Haut {:tarjonta-haut    [TarjontaHaku]
                   :direct-form-haut [DirectFormHaku]})

(s/defschema ApplicationFeedback {:form-key   s/Str
                                  :form-id    s/Int
                                  :form-name  s/Str
                                  :user-agent s/Str
                                  :rating     s/Int
                                  :feedback   (s/maybe s/Str)})
