(ns ataru.schema
  (:require [oph.soresu.form.schema :as soresu]
            [schema.core :as s]))

(soresu/create-form-schema [] [] [])

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
(intern 'oph.soresu.form.schema
        'LocalizedString
        {:fi s/Str
         (s/maybe :sv) s/Str
         (s/maybe :en) s/Str})

(s/defschema Form
  {(s/optional-key :id) s/Int
   :name                s/Str
   :modified-by         s/Str
   s/Any                s/Any})

(s/defschema FormWithContent
  (merge Form
         {:content [(s/if (comp some? :children) soresu/WrapperElement soresu/FormField)]}))


