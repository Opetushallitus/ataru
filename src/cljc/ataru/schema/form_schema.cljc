(ns ataru.schema.form-schema
  (:require [ataru.application.review-states :as review-states]
            [ataru.application.field-types :refer [form-fields]]
            [ataru.hakija.application-validators :as validator]
            [schema.core :as s]
            [schema-tools.core :as st]))

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

(s/defschema LocalizedString {:fi                  s/Str
                              (s/optional-key :sv) s/Str
                              (s/optional-key :en) s/Str})

(s/defschema LocalizedStringOptional {(s/optional-key :fi) s/Str
                                      (s/optional-key :sv) s/Str
                                      (s/optional-key :en) s/Str})

(s/defschema Form {(s/optional-key :id)                s/Int
                   :name                               LocalizedStringOptional
                   :content                            (s/pred empty?)
                   (s/optional-key :languages)         [s/Str]
                   (s/optional-key :key)               s/Str
                   (s/optional-key :created-by)        s/Str
                   (s/optional-key :created-time)      #?(:clj  org.joda.time.DateTime
                                                          :cljs s/Str)
                   (s/optional-key :application-count) s/Int
                   (s/optional-key :deleted)           (s/maybe s/Bool)})

(s/defschema Module (s/enum :person-info))

(s/defschema Button {:fieldClass                              (s/eq "button")
                     :id                                      s/Str
                     (s/optional-key :label)                  LocalizedString
                     (s/optional-key :params)                 s/Any
                     :fieldType                               s/Keyword
                     (s/optional-key :belongs-to-hakukohteet) [s/Str]})

(s/defschema FormField {:fieldClass                                      (s/eq "formField")
                        :id                                              s/Str
                        (s/optional-key :cannot-view)                    s/Bool
                        (s/optional-key :cannot-edit)                    s/Bool
                        (s/optional-key :validators)                     [(apply s/enum (concat (keys validator/pure-validators)
                                                                                                (keys validator/async-validators)))]
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
                        :fieldType                                       (apply s/enum form-fields)
                        (s/optional-key :belongs-to-hakukohteet)         [s/Str]})

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
                          (s/optional-key :belongs-to-hakukohteet) [s/Str]})

(s/defschema BasicElement (s/conditional
                            #(= "formField" (:fieldClass %)) FormField
                            #(= "button" (:fieldClass %)) Button
                            :else InfoElement))

(s/defschema WrapperElement {:fieldClass                              (apply s/enum ["wrapperElement" "questionGroup"])
                             :id                                      s/Str
                             :fieldType                               (apply s/enum ["fieldset" "rowcontainer" "adjacentfieldset"])
                             :children                                [(s/conditional #(or (= "wrapperElement" (:fieldClass %))
                                                                                           (= "questionGroup" (:fieldClass %)))
                                                                                      (s/recursive #'WrapperElement)
                                                                                      :else
                                                                                      BasicElement)]
                             (s/optional-key :child-validator)        (s/enum :one-of :birthdate-and-gender-component)
                             (s/optional-key :params)                 s/Any
                             (s/optional-key :label)                  LocalizedString
                             (s/optional-key :label-amendment)        LocalizedString ; Additional info which can be displayed next to the label
                             (s/optional-key :module)                 Module
                             (s/optional-key :belongs-to-hakukohteet) [s/Str]})

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
   :haku-name                          LocalizedStringOptional
   :prioritize-hakukohteet             s/Bool
   :max-hakukohteet                    (s/maybe s/Int)
   :can-submit-multiple-applications   s/Bool
   (s/optional-key :default-hakukohde) FormTarjontaHakukohde
   (s/optional-key :hakuaika-dates)    {:start                               s/Int
                                        (s/optional-key :end)                (s/maybe s/Int)
                                        :on                                  s/Bool
                                        :hakukierros-end                     (s/maybe s/Int)
                                        :jatkuva-haku?                       s/Bool
                                        :attachment-modify-grace-period-days (s/maybe s/Int)}})

(s/defschema Haku
  {:oid s/Str
   :name LocalizedStringOptional
   :hakuajat [{:start java.time.ZonedDateTime
               (s/optional-key :end) java.time.ZonedDateTime}]})

(s/defschema Hakukohde
  {:oid s/Str
   :haku-oid s/Str
   :name LocalizedStringOptional
   :tarjoaja-name LocalizedStringOptional})

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
                                                               [(s/cond-pre s/Str
                                                                            File
                                                                            [(s/cond-pre s/Str s/Int File)])])
                     :fieldType                    (apply s/enum form-fields)
                     (s/optional-key :cannot-view) s/Bool
                     (s/optional-key :label)       (s/maybe (s/cond-pre
                                                              LocalizedString
                                                              s/Str))})

(def review-requirement-values
  (->> review-states/hakukohde-review-types
       (map last)
       (mapcat (partial map first))
       distinct))

;; Header-level info about application, doesn't contain the actual answers
(s/defschema ApplicationInfo
  {:id                                             s/Int
   :key                                            s/Str
   :lang                                           s/Str
   :state                                          s/Str
   :score                                          (s/maybe s/Int)
   :new-application-modifications                  s/Int
   :person                                         {:preferred-name s/Str
                                                    :last-name      s/Str}
   (s/optional-key :form)                          s/Int
   (s/optional-key :created-time)                  org.joda.time.DateTime
   (s/optional-key :haku)                          (s/maybe s/Str)
   (s/optional-key :hakukohde)                     (s/maybe [s/Str])
   (s/optional-key :secret)                        s/Str
   (s/optional-key :application-hakukohde-reviews) [{:requirement (apply s/enum review-states/hakukohde-review-type-names)
                                                     :state       (apply s/enum review-requirement-values)
                                                     :hakukohde   s/Str}]}) ; "form" or oid

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
   (s/optional-key :virkailija-secret)  s/Str
   (s/optional-key :form-key)           s/Str
   (s/optional-key :tarjonta)           FormTarjontaMetadata
   (s/optional-key :person-oid)         (s/maybe s/Str)})

(s/defschema Person
  {:oid                                 (s/maybe s/Str)
   :turvakielto                         s/Bool
   :yksiloity                           s/Bool
   :first-name                          s/Str
   :preferred-name                      s/Str
   :last-name                           s/Str
   :gender                              s/Str
   :nationality                         s/Str
   (s/optional-key :gender-string)      s/Str
   (s/optional-key :nationality-string) s/Str
   (s/optional-key :ssn)                (s/maybe s/Str)
   (s/optional-key :birth-date)         s/Str})

(s/defschema ApplicationWithPerson
  (-> Application
      (st/dissoc :person-oid)
      (st/assoc :person Person)))

(s/defschema OmatsivutApplication
  {:oid s/Str
   :key s/Str
   :state s/Str
   :secret s/Str
   :haku s/Str
   :email s/Str
   :hakukohteet [s/Str]})

(s/defschema Hakutoive
  {:processingState   s/Str
   :eligibilityState  s/Str
   :paymentObligation s/Str
   :hakukohdeOid      s/Str})

(s/defschema VtsApplication
  {:oid                s/Str ; (:key application)
   :hakuOid            s/Str
   :henkiloOid         s/Str
   :asiointikieli      s/Str
   :hakutoiveet        [Hakutoive]
   :email              (s/maybe s/Str)})

(s/defschema HakurekisteriApplication
  {:oid                 s/Str
   :personOid           s/Str
   :applicationSystemId s/Str
   :kieli               s/Str
   :hakukohteet         [s/Str]
   :email               s/Str
   :matkapuhelin        s/Str
   :lahiosoite          s/Str
   :postinumero         s/Str
   :postitoimipaikka    (s/maybe s/Str)
   :asuinmaa            s/Str
   :kotikunta           (s/maybe s/Str)
   :paymentObligations  {s/Str s/Str}
   :kkPohjakoulutus     [s/Str]})

(s/defschema OnrApplication
  {:oid          s/Str
   :haku         (s/maybe s/Str)
   :form         s/Str
   :kansalaisuus s/Str
   :aidinkieli   s/Str
   :matkapuhelin s/Str
   :email        s/Str
   :lahiosoite   s/Str
   :postinumero  s/Str
   :passinNumero (s/maybe s/Str)
   :idTunnus     (s/maybe s/Str)})

(s/defschema TilastokeskusApplication
  {:hakemus_oid    s/Str
   :haku_oid       s/Str
   :henkilo_oid    s/Str
   :hakukohde_oids [s/Str]})

(def event-types (s/enum "updated-by-applicant"
                         "updated-by-virkailija"
                         "received-from-applicant"
                         "review-state-change"
                         "hakukohde-review-state-change"
                         "modification-link-sent"))

(s/defschema Event
  {:event-type                        event-types
   :time                              org.joda.time.DateTime
   :id                                s/Int
   :application-key                   s/Str
   (s/optional-key :new-review-state) (s/maybe s/Str)
   (s/optional-key :hakukohde)        (s/maybe s/Str)
   (s/optional-key :review-key)       (s/maybe s/Str)
   :first-name                        (s/maybe s/Str)
   :last-name                         (s/maybe s/Str)})

(def hakukohde-review-types-schema
  (reduce (fn [acc [kw _ states]]
            (assoc acc (s/optional-key kw) (apply s/enum (map first states))))
          {}
          review-states/hakukohde-review-types))

(s/defschema HakukohdeReviews
  {s/Keyword hakukohde-review-types-schema})

(s/defschema ReviewNote
  {:id                            s/Int
   :application-key               s/Str
   :notes                         s/Str
   :first-name                    (s/maybe s/Str)
   :last-name                     (s/maybe s/Str)
   (s/optional-key :created-time) org.joda.time.DateTime})

(s/defschema Review
  {:id                                 s/Int
   :application-key                    s/Str
   (s/optional-key :modified-time)     org.joda.time.DateTime
   :state                              s/Str
   (s/optional-key :score)             (s/maybe s/Int)
   (s/optional-key :hakukohde-reviews) HakukohdeReviews})

(s/defschema ApplicationCountsHakukohde {:oid               s/Str
                                         :name              LocalizedStringOptional
                                         :application-count s/Int
                                         :processed         s/Int
                                         :unprocessed       s/Int
                                         :haku              s/Str})

(s/defschema TarjontaHaku {:oid                    s/Str
                           :name                   LocalizedStringOptional
                           :haku-application-count s/Int
                           :application-count      s/Int
                           :processed              s/Int
                           :unprocessed            s/Int
                           :hakukohteet            [ApplicationCountsHakukohde]})

(s/defschema DirectFormHaku {:name                   LocalizedStringOptional
                             :key                    s/Str
                             :haku-application-count s/Int
                             :application-count      s/Int
                             :processed              s/Int
                             :unprocessed            s/Int})

(s/defschema Haut {:tarjonta-haut    [TarjontaHaku]
                   :direct-form-haut [DirectFormHaku]})

(s/defschema ApplicationFeedback {:form-key   s/Str
                                  :form-id    s/Int
                                  :form-name  s/Str
                                  :user-agent s/Str
                                  :rating     s/Int
                                  :feedback   (s/maybe s/Str)})

(s/defschema PermissionCheckDto {:personOidsForSamePerson [s/Str]
                                 :organisationOids [s/Str]
                                 :loggedInUserRoles [s/Str]})

(s/defschema PermissionCheckResponseDto {:accessAllowed s/Bool
                                         (s/optional-key :errorMessage) s/Str})

(s/defschema InformationRequest {:subject                         s/Str
                                 :message                         s/Str
                                 :application-key                 s/Str
                                 (s/optional-key :id)             s/Int
                                 (s/optional-key :created-time)   #?(:clj  org.joda.time.DateTime
                                                                     :cljs s/Str)
                                 (s/optional-key :first-name)     s/Str
                                 (s/optional-key :last-name)      s/Str})

(s/defschema ReviewSetting {:setting-kwd s/Str
                            :enabled     s/Bool})

(s/defschema VirkailijaSettings {:review {s/Keyword s/Bool}})
