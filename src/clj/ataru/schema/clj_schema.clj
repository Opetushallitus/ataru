(ns ataru.schema.clj-schema
  (:require [ataru.schema :as schema]
            [schema.core :as s]
            [schema-tools.core :as st]
            [oph.soresu.form.schema :as soresu]
            [clojure.string :as str]))

(s/defschema PositiveInteger
  (s/both (s/pred pos? 'pos?) s/Int))

(s/defschema OptionalLocalizedString
  {:fi                  s/Str
   (s/optional-key :sv) s/Str
   (s/optional-key :en) s/Str})

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
; memoized function, runs only once -
; it overwrites some of soresus schemas with little changes
((memoize
   (fn []

     (intern 'oph.soresu.form.schema
             'LocalizedString
             OptionalLocalizedString)

     (intern 'oph.soresu.form.schema
             'Option
             (st/assoc
               soresu/Option
               (s/optional-key :label) OptionalLocalizedString))

     nil)))

(soresu/create-form-schema [] [] [])

(s/defschema Form schema/Form)

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
                     :label OptionalLocalizedString})

(s/defschema Application
  {:form                           Long
   :lang                           s/Str
   :answers                        [Answer]
   (s/optional-key :modified-time) org.joda.time.DateTime})

(s/defschema ApplicationRequest
  {(s/optional-key :limit) (s/both PositiveInteger (s/pred (partial >= 100) 'less-than-one-hundred))
   (s/optional-key :sort) (s/enum :by-date)
   (s/optional-key :lang) s/Str})
