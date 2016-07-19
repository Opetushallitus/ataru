(ns ataru.schema.form-schema
  (:require [schema.core :as s]
            [schema-tools.core :as st]
            [clojure.string :as str]))

(s/defschema PositiveInteger
  (s/both (s/pred pos? 'pos?) s/Int))

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

(s/defschema Form {(s/optional-key :id)            s/Int
                   :name                           s/Str
                   (s/optional-key :modified-by)   s/Str
                   (s/optional-key :modified-time) #?(:clj org.joda.time.DateTime
                                                      :cljs s/Str)
                   s/Any                           s/Any})

(s/defschema LocalizedString {:fi                  s/Str
                              (s/optional-key :sv) s/Str
                              (s/optional-key :en) s/Str})

(s/defschema Module (s/enum :person-info))

(s/defschema Option {:value                  s/Str
                     (s/optional-key :label) LocalizedString})

(s/defschema Button {:fieldClass              (s/eq "button")
                     :id                      s/Str
                     (s/optional-key :label)  LocalizedString
                     (s/optional-key :params) s/Any
                     :fieldType               s/Keyword})

(s/defschema FormField {:fieldClass (s/eq "formField")
                        :id s/Str
                        :required s/Bool
                        (s/optional-key :label) LocalizedString
                        (s/optional-key :helpText) LocalizedString
                        (s/optional-key :initialValue) (s/cond-pre LocalizedString s/Int)
                        (s/optional-key :params) s/Any
                        (s/optional-key :options) [Option]
                        :fieldType (apply s/enum ["textField"
                                                  "textArea"
                                                  "nameField"
                                                  "emailField"
                                                  "moneyField"
                                                  "finnishBusinessIdField"
                                                  "iban"
                                                  "bic"
                                                  "dropdown"
                                                  "radioButton"
                                                  "checkboxButton"
                                                  "namedAttachment"
                                                  "koodistoField"])})

(s/defschema InfoElement {:fieldClass (s/eq "infoElement")
                          :id s/Str
                          :fieldType (apply s/enum ["h1"
                                                    "h3"
                                                    "link"
                                                    "p"
                                                    "bulletList"
                                                    "dateRange"
                                                    "endOfDateRange"])
                          (s/optional-key :params) s/Any
                          (s/optional-key :label) LocalizedString
                          (s/optional-key :text) LocalizedString})

(s/defschema BasicElement (s/conditional
                            #(= "formField" (:fieldClass %)) FormField
                            #(= "button" (:fieldClass %)) Button
                            :else InfoElement))

(s/defschema WrapperElement {:fieldClass              (apply s/enum ["wrapperElement"])
                             :id                      s/Str
                             :fieldType               (apply s/enum ["theme" "fieldset" "growingFieldset" "growingFieldsetChild" "rowcontainer" ])
                             :children                [(s/conditional #(= "wrapperElement" (:fieldClass %))
                                                         (s/recursive #'WrapperElement)
                                                         :else
                                                         BasicElement)]
                             (s/optional-key :params) s/Any
                             (s/optional-key :label)  LocalizedString
                             (s/optional-key :helpText) LocalizedString
                             (s/optional-key :module) Module})

(s/defschema FormWithContent
  (merge Form
         {:content [(s/if (comp some? :children) WrapperElement FormField)]}))

(s/defschema Answer {:key s/Str,
                     :value (s/cond-pre s/Str
                                        s/Int
                                        [s/Str])
                     :fieldType (apply s/enum ["textField"
                                               "textArea"
                                               "dropdown"])
                     :label (s/cond-pre
                              LocalizedString
                              s/Str)})

(s/defschema Application
  {(s/optional-key :key)           s/Str
   :form                           Long
   :lang                           s/Str
   :answers                        [Answer]
   :state                          (s/enum :received)
   (s/optional-key :modified-time) org.joda.time.DateTime})

(s/defschema ApplicationRequest
  ; limit number of applications returned
  {(s/optional-key :limit) (s/both PositiveInteger (s/pred (partial >= 100) 'less-than-one-hundred))
   (s/optional-key :sort) (s/enum :by-date)
   (s/optional-key :lang) s/Str})
