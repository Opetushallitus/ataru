(ns ataru.schema.form-schema
  (:require [ataru.schema.soresu-schema :as soresu]
            [schema.core :as s]
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

(soresu/create-form-schema [] [] [])

(s/defschema Form {(s/optional-key :id)            s/Int
                   :name                           s/Str
                   (s/optional-key :modified-by)   s/Str
                   (s/optional-key :modified-time) #?(:clj org.joda.time.DateTime
                                                      :cljs s/Str)
                   s/Any                           s/Any})

(s/defschema FormWithContent
  (merge Form
         {:content [(s/if (comp some? :children) soresu/WrapperElement soresu/FormField)]}))

(s/defschema Answer {:key s/Str,
                     :value (s/cond-pre s/Str
                                        s/Int
                                        [s/Str])
                     :fieldType (apply s/enum ["textField"
                                               "textArea"
                                               "dropdown"])
                     :label (s/cond-pre
                              soresu/LocalizedString
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

(s/defschema Answers
  "Answers consists of a key (String) value pairs, where value may be String or an array of more answers"
  { :value [soresu/Answer] })

(s/defschema Submission {:id Long
                         :created_at s/Inst
                         :form Long
                         :version Long
                         :version_closed (s/maybe s/Inst)
                         :answers Answers})

(s/defschema SubmissionValidationError
  {:error s/Str
   (s/optional-key :info) s/Any})

(s/defschema SubmissionValidationErrors
  "Submission validation errors contain a mapping from field id to list of validation errors"
  {s/Keyword [SubmissionValidationError]})
