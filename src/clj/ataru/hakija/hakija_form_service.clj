(ns ataru.hakija.hakija-form-service
  (:require [ataru.cache.cache-service :as cache]
            [ataru.forms.form-store :as form-store]
            [ataru.forms.form-payment-info :as payment-info]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.forms.hakukohderyhmat :as hakukohderyhmat]
            [ataru.hakija.person-info-fields :refer [viewing-forbidden-person-info-field-ids
                                                     editing-forbidden-person-info-field-ids
                                                     editing-allowed-person-info-field-ids]]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.tarjonta-service.hakukohde :refer [populate-hakukohde-answer-options populate-attachment-deadlines]]
            [clj-time.core :as time]
            [clj-time.coerce :as t]
            [clj-time.format :as f]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [cheshire.core :as json]
            [schema.core :as s]
            [schema.coerce :as sc]
            [ring.swagger.coerce :as coerce]
            [ataru.schema.form-schema :as form-schema]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.hakija.form-role :as form-role]
            [ataru.util :as util :refer [assoc?]]
            [taoensso.timbre :as log]
            [ataru.demo-config :as demo]
            [ataru.hakija.toisen-asteen-yhteishaku-logic :as toisen-asteen-yhteishaku-logic]
            [ataru.kk-application-payment.utils :refer [has-payment-module?]]
            [ataru.attachment-deadline.attachment-deadline-protocol :as attachment-deadline]))

(defn- set-can-submit-multiple-applications-and-yhteishaku
  [multiple? yhteishaku? haku-oid field]
  (-> field
      (assoc-in [:params :can-submit-multiple-applications] multiple?)
      (assoc-in [:params :yhteishaku] yhteishaku?)
      (cond-> (not multiple?) (assoc-in [:params :haku-oid] haku-oid))))

(defn- map-if-ssn-or-email
  [f field]
  (if (or (= "ssn" (:id field))
          (= "email" (:id field)))
    (f field)
    field))

(defn set-submit-multiple-and-yhteishaku-if-ssn-or-email-field
  [multiple? yhteishaku? haku-oid]
  (partial map-if-ssn-or-email
           (partial set-can-submit-multiple-applications-and-yhteishaku
                    multiple? yhteishaku? haku-oid)))

(defn- update-ssn-and-email-fields-in-person-module
  [multiple? yhteishaku? haku-oid form]
  (when-let [person-module-idx (util/first-index-of #(= (:module %) "person-info") (:content form))]
    (let [person-module     (nth (:content form) person-module-idx)
          new-person-module (update person-module :children (partial walk/prewalk (set-submit-multiple-and-yhteishaku-if-ssn-or-email-field multiple? yhteishaku? haku-oid)))]
      (assoc-in form [:content person-module-idx] new-person-module))))

(defn populate-can-submit-multiple-applications
  [form tarjonta-info]
  (let [multiple?   (get-in tarjonta-info [:tarjonta :can-submit-multiple-applications] true)
        yhteishaku? (get-in tarjonta-info [:tarjonta :yhteishaku] false)
        haku-oid    (get-in tarjonta-info [:tarjonta :haku-oid])]
    (or
      (update-ssn-and-email-fields-in-person-module multiple? yhteishaku? haku-oid form)
      (update form :content
              (fn [content]
                (walk/prewalk (set-submit-multiple-and-yhteishaku-if-ssn-or-email-field multiple? yhteishaku? haku-oid) content))))))

(defn- custom-deadline [field]
  (get-in field [:params :deadline]))

(def deadline-format (f/formatter "dd.MM.yyyy HH:mm" (time/time-zone-for-id "Europe/Helsinki")))

(defn- editing-allowed-by-custom-deadline? [now field]
  (some->> (custom-deadline field)
           (f/parse deadline-format)
           (time/before? now)))

(defn- editing-allowed-by-hakuaika?
  [attachment-deadline-service now field hakuajat application-in-processing-state? application-submitted haku]
  (let [hakuaika            (hakuaika/select-hakuaika-for-field now field hakuajat)
        hakuaika-start      (some-> hakuaika :start t/from-long)
        hakuaika-end        (some-> hakuaika :end t/from-long)
        attachment-edit-end (attachment-deadline/attachment-deadline-for-hakuaika attachment-deadline-service application-submitted haku hakuaika)
        hakukierros-end     (some-> hakuaika :hakukierros-end t/from-long)
        after?              (fn [t] (or (nil? t)
                                        (time/after? now t)))
        before?             (fn [t] (and (some? t)
                                         (time/before? now t)))]
    (or (nil? hakuaika)
        (and (not (and application-in-processing-state? (:jatkuva-or-joustava-haku? hakuaika)))
             (after? hakuaika-start)
             (or (before? hakuaika-end)
                 (nil? hakuaika-end)
                 (and (before? attachment-edit-end)
                      (= "attachment" (:fieldType field)))
                 (and (before? hakukierros-end)
                      (contains? editing-allowed-person-info-field-ids
                        (keyword (:id field)))))))))

(def lupatiedot-field-ids #{:lupatiedot-toinen-aste :paatos-opiskelijavalinnasta-sahkopostiin :koulutusmarkkinointilupa :valintatuloksen-julkaisulupa :asiointikieli})

(defn- is-lupatieto-field?
  [field]
  (contains? lupatiedot-field-ids (keyword (:id field))))

(defn- is-editing-allowed-person-info-field?
  [field]
  (contains? editing-allowed-person-info-field-ids (keyword (:id field))))

(defn- editing-allowed-for-person-info-field?
  [roles field]
  (or (and (form-role/virkailija? roles)
           (not (form-role/with-henkilo? roles)))
      (not (contains? editing-forbidden-person-info-field-ids (keyword (:id field))))))

(defn- editing-allowed-by-deadlines?
  [attachment-deadline-service now field hakuajat roles application-in-processing-state? field-deadline application-submitted haku]
  (or (form-role/virkailija? roles)
      (cond (some? field-deadline)
            (time/before? now (:deadline field-deadline))
            (custom-deadline field)
            (editing-allowed-by-custom-deadline? now field)
            :else
            (editing-allowed-by-hakuaika? attachment-deadline-service now field hakuajat application-in-processing-state? application-submitted haku))))

(defn- application-not-in-processing?
  [roles hakuajat application-in-processing-state?]
  (or (form-role/virkailija? roles)
      (not (and (empty? (:uniques hakuajat))
                application-in-processing-state?))))

(defn- editing-allowed-by-toisen-asteen-yhteishaku-restrictions?
  [use-toisen-asteen-yhteishaku-restrictions? field]
  (or (not use-toisen-asteen-yhteishaku-restrictions?)
      (is-lupatieto-field? field)
      (is-editing-allowed-person-info-field? field)))

(defn- editing-allowed-by-kk-payment-status?
  [roles has-overdue-payment?]
  (or (form-role/virkailija? roles)
      (not has-overdue-payment?)))

(defn- uneditable?
  [attachment-deadline-service now field hakuajat roles application-in-processing-state? field-deadline
   use-toisen-asteen-yhteishaku-restrictions? has-overdue-payment? application-submitted haku]
  (not (and (editing-allowed-for-person-info-field? roles field)
            (editing-allowed-by-deadlines? attachment-deadline-service now field hakuajat roles application-in-processing-state? field-deadline application-submitted haku)
            (application-not-in-processing? roles hakuajat application-in-processing-state?)
            (editing-allowed-by-toisen-asteen-yhteishaku-restrictions? use-toisen-asteen-yhteishaku-restrictions? field)
            (editing-allowed-by-kk-payment-status? roles has-overdue-payment?))))

(defn- combine-old-priorisoivat-ryhmat-with-new
  [haku-oid old-priorisoivat hakukohderyhmat-with-settings]
  (let [oid-is-found (fn [oid old-ones]
                       (boolean (when (seq old-ones) (seq (filter #(= oid (:hakukohderyhma-oid %)) old-ones)))))
        priorisoivat (filter #(:priorisoiva %) hakukohderyhmat-with-settings)
        not-found-in-old (filter #(not (oid-is-found (:hakukohderyhma-oid %) old-priorisoivat)) priorisoivat)
        transformed-to-priorisoivat (map #(merge {} {:hakukohderyhma-oid (:hakukohderyhma-oid %)
                                                 :prioriteetit (mapv vector (:prioriteettijarjestys %))
                                                 :haku-oid haku-oid}) not-found-in-old)]
    (concat old-priorisoivat transformed-to-priorisoivat)))

(defn- combine-old-rajaavat-ryhmat-with-new
  [haku-oid old-rajaavat hakukohderyhmat-with-settings]
    (let [oid-is-found (fn [oid old-ones]
                         (if (seq old-ones)
                           (boolean (seq (filter #(= oid (:hakukohderyhma-oid %)) old-ones)))
                           false))
          rajaavat (filter #(:rajaava %) hakukohderyhmat-with-settings)
          not-found-in-old (filter #(not (oid-is-found (:hakukohderyhma-oid %) old-rajaavat)) rajaavat)
          transformed-to-rajaavat (map #(merge {} {:hakukohderyhma-oid (:hakukohderyhma-oid %)
                                          :raja (:max-hakukohteet %)
                                          :haku-oid haku-oid}) not-found-in-old)]
      (concat old-rajaavat transformed-to-rajaavat))
  )

(defn- cannot-view-field?
  [roles field]
  (and
    (or
      (contains? viewing-forbidden-person-info-field-ids
             (keyword (:id field)))
      (boolean (:sensitive-answer field)))
    (not (form-role/virkailija? roles))))

(defn flag-uneditable-and-unviewable-field
  [attachment-deadline-service now hakuajat roles application-in-processing-state? field-deadlines use-toisen-asteen-yhteishaku-restrictions? has-overdue-payment? application-submitted haku field]
  (if (= "formField" (:fieldClass field))
    (let [cannot-view? (cannot-view-field? roles field)
          cannot-edit? (or cannot-view?
                           (uneditable?
                            attachment-deadline-service
                            now
                            field
                            hakuajat
                            roles
                            application-in-processing-state?
                            (get field-deadlines (:id field))
                            use-toisen-asteen-yhteishaku-restrictions?
                            has-overdue-payment?
                            application-submitted
                            haku))]
      (assoc field
             :cannot-view cannot-view?
             :cannot-edit cannot-edit?))
    field))

(s/defn ^:always-validate flag-uneditable-and-unviewable-fields :- s/Any
  [form :- s/Any
   now :- s/Any
   hakuajat :- s/Any
   roles :- [form-role/FormRole]
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}
   use-toisen-asteen-yhteishaku-restrictions? :- s/Bool
   has-overdue-payment? :- s/Bool
   attachment-deadline-service :- s/Any
   application-submitted :- s/Any
   haku :- s/Any]
  (update form :content (partial util/map-form-fields
                                 (partial flag-uneditable-and-unviewable-field
                                          attachment-deadline-service
                                          now
                                          hakuajat
                                          roles
                                          application-in-processing-state?
                                          field-deadlines
                                          use-toisen-asteen-yhteishaku-restrictions?
                                          has-overdue-payment?
                                          application-submitted
                                          haku))))

(s/defn ^:always-validate remove-required-hakija-validator-if-virkailija :- s/Any
  [form :- s/Any
   roles :- [form-role/FormRole]]
  (if (form-role/virkailija? roles)
    (update form :content
            (fn [content]
              (walk/prewalk
                (fn [field]
                  (if (= "formField" (:fieldClass field))
                    (update field :validators (partial remove #{"required-hakija"}))
                    field))
                content)))
    form))

(defn- parse-date
  [date-str]
  (when (and (some? date-str) (not (string/blank? date-str)))
    (f/parse (:date f/formatters) date-str)))

(defn- before-hakuaika-and-grace-period?
  [hakuajat now]
  (let [hakuaika (hakuaika/first-by-start (vec (:uniques hakuajat)))
        hakuaika-start (some-> hakuaika :start t/from-long)
        grace-period (time/days demo/demo-validity-grace-period-days)]
    (if (some? hakuaika-start)
      (let [first-valid-moment (time/minus hakuaika-start grace-period)]
        (time/before? now first-valid-moment))
      true)))

(defn- within-demo-validity-period?
  [form now]
  (let [demo-validity-start (parse-date (get-in form [:properties :demo-validity-start]))
        demo-validity-end   (parse-date (get-in form [:properties :demo-validity-end]))]
    (boolean
      (and
        (some? demo-validity-start)
        (some? demo-validity-end)
        (let [first-valid-moment   (time/with-time-at-start-of-day demo-validity-start)
              first-invalid-moment (time/with-time-at-start-of-day (time/plus demo-validity-end (time/days 1)))
              valid-interval       (time/interval first-valid-moment first-invalid-moment)]
          (time/within? valid-interval now))))))

(defn- is-demo-allowed?
  [form hakuajat now]
  (and
    (before-hakuaika-and-grace-period? hakuajat now)
    (within-demo-validity-period? form now)))

(defn- populate-demo-allowed
  [form hakuajat now]
  (assoc form :demo-allowed (is-demo-allowed? form hakuajat now)))

(s/defn ^:always-validate fetch-form-by-id :- s/Any
  ([id :- s/Any
   roles :- [form-role/FormRole]
   form-by-id-cache :- s/Any
   koodisto-cache :- s/Any
   hakukohteet :- s/Any
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}
   attachment-deadline-service :- s/Any
   application-submitted :- s/Any
   haku :- s/Any]
  (fetch-form-by-id id roles form-by-id-cache koodisto-cache hakukohteet
                    application-in-processing-state? field-deadlines false false false attachment-deadline-service
                    application-submitted haku))
  ([id :- s/Any
   roles :- [form-role/FormRole]
   form-by-id-cache :- s/Any
   koodisto-cache :- s/Any
   hakukohteet :- s/Any
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}
   use-toisen-asteen-yhteishaku-restrictions? :- s/Bool
   uses-payment-module? :- (s/maybe s/Bool)
   has-overdue-payment? :- s/Bool
   attachment-deadline-service :- s/Any
   application-submitted :- s/Any
   haku :- s/Any]
  (let [now      (time/now)
        hakuajat (hakuaika/index-hakuajat hakukohteet)]
    (when-let [form (cache/get-from form-by-id-cache (str id))]
      (when (not (:deleted form))
        (if (and uses-payment-module? (not (has-payment-module? form)))
          (throw (RuntimeException. (str "Haku should use payment module, but form id " id " with key " (:key form) " does not have one")))
          (-> (koodisto/populate-form-koodisto-fields koodisto-cache form)
              (remove-required-hakija-validator-if-virkailija roles)
              (populate-attachment-deadlines now hakuajat field-deadlines attachment-deadline-service
                                             application-submitted haku)
              (flag-uneditable-and-unviewable-fields now hakuajat roles application-in-processing-state?
                                                     field-deadlines use-toisen-asteen-yhteishaku-restrictions?
                                                     has-overdue-payment? attachment-deadline-service
                                                     application-submitted haku)
              (populate-demo-allowed hakuajat now))))))))

(s/defn ^:always-validate fetch-form-by-key :- s/Any
  [key :- s/Any
   roles :- [form-role/FormRole]
   form-by-id-cache :- s/Any
   koodisto-cache :- s/Any
   hakukohteet :- s/Any
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}
   attachment-deadline-service :- s/Any
   application-submitted :- s/Any
   haku :- s/Any]
  (when-let [latest-id (form-store/latest-id-by-key key)]
    (fetch-form-by-id latest-id
                      roles
                      form-by-id-cache
                      koodisto-cache
                      hakukohteet
                      application-in-processing-state?
                      field-deadlines
                      attachment-deadline-service
                      application-submitted
                      haku)))

(s/defn ^:always-validate fetch-form-by-haku-oid-and-id :- s/Any
  [form-by-id-cache :- s/Any
   tarjonta-service :- s/Any
   koodisto-cache :- s/Any
   organization-service :- s/Any
   ohjausparametrit-service :- s/Any
   hakukohderyhma-settings-cache :- s/Any
   haku-oid :- s/Any
   id :- s/Int
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}
   roles :- [form-role/FormRole]
   use-toisen-asteen-yhteishaku-restrictions? :- s/Bool
   has-overdue-payment? :- s/Bool
   attachment-deadline-service :- s/Any
   application-submitted :- s/Any
   haku :- s/Any]
  (let [tarjonta-info (tarjonta-parser/parse-tarjonta-info-by-haku koodisto-cache tarjonta-service organization-service ohjausparametrit-service haku-oid)
        hakukohteet (get-in tarjonta-info [:tarjonta :hakukohteet])
        hakukohderyhmat (distinct (mapcat #(:hakukohderyhmat %) hakukohteet))
        hakukohderyhmat-with-settings (map #(assoc (cache/get-from hakukohderyhma-settings-cache %) :hakukohderyhma-oid %) hakukohderyhmat)
        old-rajaavat (:ryhmat (hakukohderyhmat/rajaavat-hakukohderyhmat haku-oid))
        old-priorisoivat (:ryhmat (hakukohderyhmat/priorisoivat-hakukohderyhmat tarjonta-service haku-oid))
        rajaavat (combine-old-rajaavat-ryhmat-with-new haku-oid old-rajaavat hakukohderyhmat-with-settings)
        priorisoivat (combine-old-priorisoivat-ryhmat-with-new haku-oid old-priorisoivat hakukohderyhmat-with-settings)
        uses-payment-module? (get-in tarjonta-info [:tarjonta :maksullinen-kk-haku?])
        form (fetch-form-by-id
               id
               roles
               form-by-id-cache
               koodisto-cache
               hakukohteet
               application-in-processing-state?
               field-deadlines
               use-toisen-asteen-yhteishaku-restrictions?
               uses-payment-module?
               has-overdue-payment?
               attachment-deadline-service
               application-submitted
               haku)]
    (if (and (some? form) (some? tarjonta-info))
      (-> form
          (merge tarjonta-info)
          (assoc? :priorisoivat-hakukohderyhmat priorisoivat)
          (assoc? :rajaavat-hakukohderyhmat rajaavat)
          (payment-info/add-payment-info-if-higher-education (:tarjonta tarjonta-info))
          (populate-hakukohde-answer-options tarjonta-info)
          (populate-can-submit-multiple-applications tarjonta-info))
      (log/warn "Form (id: " id ", haku-oid: " haku-oid ", hakukohteet: " hakukohteet ") cannot be fetched. Possible reason can be missing hakukohteet."))))

(s/defn ^:always-validate fetch-form-by-haku-oid :- s/Any
  [form-by-id-cache :- s/Any
   tarjonta-service :- s/Any
   koodisto-cache :- s/Any
   organization-service :- s/Any
   ohjausparametrit-service :- s/Any
   hakukohderyhma-settings-cache :- s/Any
   haku-oid :- s/Any
   application-in-processing-state? :- s/Bool
   field-deadlines :- {s/Str form-schema/FieldDeadline}
   roles :- [form-role/FormRole]
   is-rewrite-secret-used? :- s/Bool
   has-overdue-payment? :- s/Bool
   attachment-deadline-service :- s/Any
   application-submitted :- s/Any]
  (let [haku      (tarjonta/get-haku tarjonta-service haku-oid)
        use-toisen-asteen-yhteishaku-restrictions? (toisen-asteen-yhteishaku-logic/use-toisen-asteen-yhteishaku-restrictions?
                                                     roles
                                                     is-rewrite-secret-used?
                                                     haku)
        latest-id (some-> haku
                          :ataru-form-key
                          form-store/latest-id-by-key)]
    (when latest-id
      (fetch-form-by-haku-oid-and-id form-by-id-cache
                                     tarjonta-service
                                     koodisto-cache
                                     organization-service
                                     ohjausparametrit-service
                                     hakukohderyhma-settings-cache
                                     haku-oid
                                     latest-id
                                     application-in-processing-state?
                                     field-deadlines
                                     roles
                                     use-toisen-asteen-yhteishaku-restrictions?
                                     has-overdue-payment?
                                     attachment-deadline-service
                                     application-submitted
                                     haku))))

(defn latest-form-id-by-key
  [key]
  (form-store/latest-id-by-key key))

(defn- form-by-haku-oid-cache-key
  [haku-oid roles]
  (apply str
    haku-oid
    "#" false
    (sort (map #(str "#" (name %)) roles))))

(s/defn ^:always-validate fetch-form-by-haku-oid-str-cached :- s/Any
  [form-by-haku-oid-str-cache :- s/Any
   haku-oid :- s/Str
   roles :- [form-role/FormRole]]
  (cache/get-from form-by-haku-oid-str-cache
    (form-by-haku-oid-cache-key haku-oid roles)))

(s/defn ^:always-validate clear-form-by-haku-oid-str-cache :- s/Any
  [form-by-haku-oid-str-cache :- s/Any
   haku-oid :- s/Str
   roles :- [form-role/FormRole]]
  (cache/remove-from form-by-haku-oid-str-cache
    (form-by-haku-oid-cache-key haku-oid roles)))

(s/defn ^:always-validate fetch-form-by-hakukohde-oid-str-cached :- s/Any
  [tarjonta-service :- s/Any
   form-by-haku-oid-str-cache :- s/Any
   hakukohde-oid :- s/Str
   roles :- [form-role/FormRole]]
  (when-let [hakukohde (tarjonta/get-hakukohde tarjonta-service hakukohde-oid)]
    (fetch-form-by-haku-oid-str-cached form-by-haku-oid-str-cache
                                       (:haku-oid hakukohde)
                                       roles)))

(defn fetch-latest-version-by-id [form-id]
  (form-store/fetch-latest-version form-id))

(def form-coercer (sc/coercer! form-schema/FormWithContentAndTarjontaMetadata
                               coerce/json-schema-coercion-matcher))

(defrecord FormByHakuOidStrCacheLoader [form-by-id-cache
                                        koodisto-cache
                                        ohjausparametrit-service
                                        organization-service
                                        tarjonta-service
                                        hakukohderyhma-settings-cache
                                        attachment-deadline-service]
  cache/CacheLoader
  (load [_ key]
    (let [[haku-oid _aips? & roles] (clojure.string/split key #"#")]
      (when-let [form (fetch-form-by-haku-oid form-by-id-cache
                                              tarjonta-service
                                              koodisto-cache
                                              organization-service
                                              ohjausparametrit-service
                                              hakukohderyhma-settings-cache
                                              haku-oid
                                              false
                                              {}
                                              (map keyword roles)
                                              false
                                              false
                                              attachment-deadline-service
                                              nil)]
        (json/generate-string (form-coercer form)))))
  (load-many [this keys]
    (into {} (keep #(when-let [v (cache/load this %)] [% v]) keys)))
  (load-many-size [_] 1)
  (check-schema [_ _] nil))

(defn is-demo-allowed
  [form-by-haku-oid-str-cache haku-oid]
  (let [form (fetch-form-by-haku-oid-str-cached form-by-haku-oid-str-cache haku-oid [:hakija])
        parsed-form (json/parse-string form true)]
    (get parsed-form :demo-allowed)))
