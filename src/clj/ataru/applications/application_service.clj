(ns ataru.applications.application-service
  (:require
    [ataru.applications.application-access-control :as aac]
    [ataru.applications.application-store :as application-store]
    [ataru.applications.excel-export :as excel]
    [ataru.config.core :refer [config]]
    [ataru.email.application-email-confirmation :as email]
    [ataru.forms.form-store :as form-store]
    [ataru.hakija.hakija-form-service :as hakija-form-service]
    [ataru.information-request.information-request-store :as information-request-store]
    [ataru.koodisto.koodisto :as koodisto]
    [ataru.organization-service.organization-service :as organization-service]
    [ataru.person-service.birth-date-converter :as bd-converter]
    [ataru.person-service.person-service :as person-service]
    [ataru.tarjonta-service.hakukohde :refer [populate-hakukohde-answer-options]]
    [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
    [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
    [ataru.tutkintojen-tunnustaminen :as tutkintojen-tunnustaminen]
    [ataru.util :as util]
    [ataru.valinta-tulos-service.service :as valinta-tulos-service]
    [ataru.applications.filtering :as application-filtering]
    [clojure.data :refer [diff]]
    [ataru.virkailija.editor.form-utils :refer [visible?]]
    [taoensso.timbre :as log]
    [ataru.schema.form-schema :as ataru-schema]
    [ataru.organization-service.session-organizations :as session-orgs]
    [schema.core :as s]
    [ataru.dob :as dob])
  (:import
    java.io.ByteArrayInputStream
    java.security.SecureRandom
    java.util.Base64
    [javax.crypto Cipher]
    [javax.crypto.spec GCMParameterSpec SecretKeySpec]))

(defn- extract-koodisto-fields [field-descriptor-list]
  (reduce
    (fn [result {:keys [children id koodisto-source options followups]}]
      (cond
        (some? children)
        (merge result (extract-koodisto-fields children))

        (some :followups options)
        (merge result (extract-koodisto-fields options))

        (not-empty followups)
        (merge result (extract-koodisto-fields followups))

        :else
        (cond-> result
          (every? some? [id koodisto-source])
          (assoc id (select-keys koodisto-source [:uri :version])))))
    {}
    field-descriptor-list))

(defn- parse-application-hakukohde-reviews
  [application-key]
  (reduce
    (fn [acc {:keys [hakukohde requirement state]}]
      (update-in acc [(or hakukohde :form)] assoc (keyword requirement) state))
    {}
    (application-store/get-application-hakukohde-reviews application-key)))

(defn- parse-application-attachment-reviews
  [application-key]
  (reduce
   (fn [acc {:keys [attachment-key state hakukohde]}]
     (assoc-in acc [hakukohde attachment-key] state))
   {}
   (application-store/get-application-attachment-reviews application-key)))

(defn- populate-form-fields
  [form koodisto-cache tarjonta-info]
  (-> (koodisto/populate-form-koodisto-fields koodisto-cache form)
      (populate-hakukohde-answer-options tarjonta-info)
      (hakija-form-service/populate-can-submit-multiple-applications tarjonta-info)))

(defn fields-equal? [[new-in-left new-in-right]]
  (and (nil? new-in-left)
       (nil? new-in-right)))

(defn- remove-irrelevant-changes [field]
  (-> field
      (update :params dissoc :info-text)
      (dissoc :metadata)))

(defn forms-differ? [application tarjonta-info form-left form-right]
  (and (not= (:id form-left) (:id form-right))
       (let [answers        (group-by :key (:answers application))
             hakutoiveet    (set (:hakukohde application))
             visible-fields (fn [form]
                                (let [flat-form-fields (util/flatten-form-fields (:content form))
                                      field-by-id (util/group-by-first :id flat-form-fields)]
                                  (->> flat-form-fields
                                       (filter util/answerable?)
                                       (filter #(visible? % field-by-id answers hakutoiveet
                                                          (-> tarjonta-info :tarjonta :hakukohteet)))
                                       (map remove-irrelevant-changes))))
             fields-left    (sort-by :id (visible-fields form-left))
             fields-right   (sort-by :id (visible-fields form-right))]
         (not (fields-equal? (diff fields-left fields-right))))))

(defn- enrich-virkailija-organizations
  [organization-service m]
  (cond-> m
          (contains? m :virkailija-organizations)
          (update :virkailija-organizations
                  (partial organization-service/get-organizations-for-oids organization-service))))

(defn- get-application-events
  [organization-service application-key]
  (map (partial enrich-virkailija-organizations organization-service)
       (application-store/get-application-events application-key)))

(defn ->form-query
  [key]
  {:form key})

(defn ->hakukohderyhma-query
  [ryhman-hakukohteet
   authorized-organization-oids
   haku-oid
   ensisijaisesti
   rajaus-hakukohteella]
  (let [kayttajan-hakukohteet (filter #(some authorized-organization-oids (:tarjoaja-oids %))
                                      ryhman-hakukohteet)]
    (if ensisijaisesti
      (if (some? rajaus-hakukohteella)
        {:haku                         haku-oid
         :ensisijainen-hakukohde       [rajaus-hakukohteella]
         :ensisijaisesti-hakukohteissa (map :oid ryhman-hakukohteet)}
        {:haku                         haku-oid
         :ensisijainen-hakukohde       (map :oid kayttajan-hakukohteet)
         :ensisijaisesti-hakukohteissa (map :oid ryhman-hakukohteet)})
      {:haku      haku-oid
       :hakukohde (map :oid ryhman-hakukohteet)})))

(defn ->haku-query
  [haku-oid]
  {:haku haku-oid})

(defn ->ssn-query
  [ssn]
  {:ssn ssn})

(defn ->dob-query
  [dob]
  {:dob dob})

(defn ->email-query
  [email]
  {:email email})

(defn ->name-query
  [name]
  {:name name})

(defn ->person-oid-query
  [person-oid]
  {:person-oid person-oid})

(defn ->application-oid-query
  [application-oid]
  {:application-oid application-oid})

(defn ->application-oids-query
  [application-oids]
  {:application-oids application-oids})

(defn ->attachment-review-states-query
  [attachment-review-states-query]
  (when (seq attachment-review-states-query)
    {:attachment-review-states
     (into
      {}
      (map (fn [[field-id states]]
             [(name field-id)
              (keep #(when (second %) (name (first %))) states)])
           attachment-review-states-query))}))

(defn ->option-answers-query
  [option-answers]
  (when (seq option-answers)
    {:option-answers
     (into
      {}
      (map (fn [[field-id options]]
             [(name field-id) options])
           option-answers))}))

(defn ->empty-query
  []
  {})

(defn ->and-query [& queries] (apply merge queries))

(defn- populate-applications-with-person-data
  [person-service applications]
  (let [persons (person-service/get-persons
                 person-service
                 (distinct (keep :person-oid applications)))]
    (map (fn [application]
           (let [onr-person (get persons (:person-oid application))
                 person     (if (or (:yksiloity onr-person)
                                    (:yksiloityVTJ onr-person))
                              (merge {:oid            (:oidHenkilo onr-person)
                                      :preferred-name (:kutsumanimi onr-person)
                                      :last-name      (:sukunimi onr-person)
                                      :yksiloity      true
                                      :dob            (bd-converter/convert-to-finnish-format (:syntymaaika onr-person))}
                                     (when (some? (:hetu onr-person))
                                       {:ssn (:hetu onr-person)}))
                              (merge {:preferred-name (:preferred-name application)
                                      :last-name      (:last-name application)
                                      :yksiloity      false
                                      :dob            (:dob application)}
                                     (when (some? (:person-oid application))
                                       {:oid (:person-oid application)})
                                     (when (some? (:ssn application))
                                       {:ssn (:ssn application)})))]
             (-> application
                 (assoc :person person)
                 (dissoc :ssn :dob :person-oid :preferred-name :last-name))))
         applications)))

(defn- save-application-hakukohde-reviews
  [application-key hakukohde-reviews session audit-logger]
  (doseq [[hakukohde review] hakukohde-reviews]
    (doseq [[review-requirement review-state] review]
      (application-store/save-application-hakukohde-review
        application-key
        (name hakukohde)
        (name review-requirement)
        (name review-state)
        session
        audit-logger))))

(defn- save-attachment-hakukohde-reviews
  [application-key attachment-reviews session audit-logger]
  (doseq [[hakukohde review] attachment-reviews
          [attachment-key review-state] review]
    (application-store/save-attachment-hakukohde-review
      application-key
      (name hakukohde)
      (name attachment-key)
      review-state
      session
      audit-logger)))

(defn- add-selected-hakukohteet
  [states-and-filters
   haku-oid
   hakukohde-oid
   hakukohderyhma-oid
   rajaus-hakukohteella
   ryhman-hakukohteet]
  (cond (some? hakukohde-oid)
        (assoc states-and-filters :selected-hakukohteet #{hakukohde-oid})
        (some? rajaus-hakukohteella)
        (assoc states-and-filters :selected-hakukohteet #{rajaus-hakukohteella})
        (and (some? haku-oid) (some? hakukohderyhma-oid))
        (->> (map :oid ryhman-hakukohteet)
             set
             (assoc states-and-filters :selected-hakukohteet))
        :else
        states-and-filters))

(defn- add-henkilo
  [henkilot application]
  (let [person        (get henkilot (:personOid application))
        asiointikieli (or (:asiointiKieli person)
                          (get {"fi" {:kieliKoodi  "fi"
                                      :kieliTyyppi "suomi"}
                                "sv" {:kieliKoodi  "sv"
                                      :kieliTyyppi "svenska"}
                                "en" {:kieliKoodi  "en"
                                      :kieliTyyppi "English"}}
                               (or (get {"1" "fi"
                                         "2" "sv"
                                         "3" "en"}
                                        ((:keyValues application) "asiointikieli"))
                                   (:lang application))))]
    (-> application
        (assoc :person (select-keys person
                                    [:oidHenkilo
                                     :etunimet
                                     :syntymaaika
                                     :hetu
                                     :sukunimi]))
        (assoc-in [:person :asiointiKieli] (select-keys asiointikieli
                                                        [:kieliKoodi
                                                         :kieliTyyppi]))
        (dissoc :personOid :lang))))

(defn- add-asiointikieli [henkilot application]
  (let [asiointikieli (or (get {"1" "fi"
                                "2" "sv"
                                "3" "en"}
                            ((:keyValues application) "asiointikieli"))
                          (get-in (get henkilot (:personOid application))
                            [:asiointiKieli :kieliKoodi])
                          (:asiointikieli application))]
    (assoc application :asiointikieli asiointikieli)))

(defn- remove-irrelevant-application_hakukohde_reviews [application]
  (let [relevant-hakukohteet        (set (:hakukohde application))]
    (if (not-empty relevant-hakukohteet)
      (let [relevant-hakukohde-reviews  (->> application
                                             :application-hakukohde-reviews
                                             (filter #(contains? relevant-hakukohteet (:hakukohde %))))]
        (assoc application :application-hakukohde-reviews relevant-hakukohde-reviews))
      application)))

(defn- get-application-list-by-query
    [person-service
     tarjonta-service
     authorized-organization-oids
     query
     sort
     states-and-filters]
    (let [applications            (->> (application-store/get-application-heading-list query sort)
                                       (map remove-irrelevant-application_hakukohde_reviews))
          authorized-applications (aac/filter-authorized
                                   tarjonta-service
                                   (some-fn (partial aac/authorized-by-form? authorized-organization-oids)
                                            (partial aac/authorized-by-tarjoajat? authorized-organization-oids))
                                   applications)]
      {:applications (if (application-filtering/person-info-needed-to-filter? (:filters states-and-filters))
                       (application-filtering/filter-applications
                        (populate-applications-with-person-data person-service authorized-applications)
                        states-and-filters)
                       (populate-applications-with-person-data
                        person-service
                        (application-filtering/filter-applications authorized-applications states-and-filters)))
       :sort         (merge {:order-by (:order-by sort)
                             :order    (:order sort)}
                            (when-let [a (first (drop 999 applications))]
                              {:offset (case (:order-by sort)
                                         "applicant-name" {:key            (:key a)
                                                           :last-name      (:last-name a)
                                                           :preferred-name (:preferred-name a)}
                                         "submitted"      {:key       (:key a)
                                                           :submitted (:submitted a)}
                                         "created-time"   {:key          (:key a)
                                                           :created-time (:created-time a)})}))}))

(defn- hakukohteiden-ehdolliset
  [valinta-tulos-service applications]
  (into {}
        (comp (mapcat :hakukohde)
              (distinct)
              (map #(vector % (valinta-tulos-service/hakukohteen-ehdolliset
                               valinta-tulos-service
                               %))))
        applications))

(defn ->hakukohde-query
    [tarjonta-service hakukohde-oid ensisijaisesti]
    (let [hakukohde (tarjonta-service/get-hakukohde tarjonta-service hakukohde-oid)]
      (merge {:haku (:haku-oid hakukohde)}
             (if ensisijaisesti
               {:ensisijainen-hakukohde [hakukohde-oid]}
               {:hakukohde [hakukohde-oid]}))))

(s/defn ^:always-validate query-applications-paged
    [organization-service
     person-service
     tarjonta-service
     session
     params :- ataru-schema/ApplicationQuery] :- ataru-schema/ApplicationQueryResponse
    (let [{:keys [form-key
                  hakukohde-oid
                  hakukohderyhma-oid
                  haku-oid
                  ensisijaisesti
                  rajaus-hakukohteella
                  ssn
                  dob
                  email
                  name
                  person-oid
                  application-oid
                  attachment-review-states
                  option-answers
                  sort
                  states-and-filters]} params
          ensisijaisesti               (boolean ensisijaisesti)
          authorized-organization-oids (session-orgs/run-org-authorized
                                        session
                                        organization-service
                                        [:view-applications :edit-applications]
                                        (fn [] (constantly false))
                                        (fn [oids] #(contains? oids %))
                                        (fn [] (constantly true)))
          ryhman-hakukohteet           (when (and (some? haku-oid) (some? hakukohderyhma-oid))
                                         (filter (fn [hakukohde]
                                                   (some #(= hakukohderyhma-oid %) (:ryhmaliitokset hakukohde)))
                                                 (tarjonta-service/hakukohde-search tarjonta-service haku-oid nil)))]
      (when-let [query (->and-query
                        (cond (some? form-key)
                              (->form-query form-key)
                              (and (some? haku-oid) (some? hakukohderyhma-oid))
                              (->hakukohderyhma-query
                               ryhman-hakukohteet
                               authorized-organization-oids
                               haku-oid
                               ensisijaisesti
                               rajaus-hakukohteella)
                              (some? hakukohde-oid)
                              (->hakukohde-query tarjonta-service hakukohde-oid ensisijaisesti)
                              (some? haku-oid)
                              (->haku-query haku-oid)
                              :else
                              (->empty-query))
                        (cond (some? ssn)
                              (->ssn-query ssn)
                              (and (some? dob) (dob/dob? dob))
                              (->dob-query dob)
                              (some? email)
                              (->email-query email)
                              (some? name)
                              (->name-query name)
                              (some? person-oid)
                              (->person-oid-query person-oid)
                              (some? application-oid)
                              (->application-oid-query application-oid))
                        (->attachment-review-states-query attachment-review-states)
                        (->option-answers-query option-answers))]
        (get-application-list-by-query
         person-service
         tarjonta-service
         authorized-organization-oids
         query
         sort
         (add-selected-hakukohteet states-and-filters
                                   haku-oid
                                   hakukohde-oid
                                   hakukohderyhma-oid
                                   rajaus-hakukohteella
                                   ryhman-hakukohteet)))))

(defprotocol ApplicationService
  (get-person [this application])
  (get-application-with-human-readable-koodis [this application-key session with-newest-form?])
  (get-excel-report-of-applications-by-key [this application-keys selected-hakukohde selected-hakukohderyhma included-ids session])
  (save-application-review [this session review])
  (mass-update-application-states [this session application-keys hakukohde-oid from-state to-state])
  (send-modify-application-link-email [this application-key session])
  (add-review-note [this session note])
  (get-application-version-changes [this koodisto-cache session application-key])
  (omatsivut-applications [this session person-oid])
  (get-applications-for-valintalaskenta [this session hakukohde-oid application-keys])
  (siirto-applications [this session hakukohde-oid application-keys])
  (suoritusrekisteri-applications [this haku-oid hakukohde-oids person-oids modified-after offset]))

(defrecord CommonApplicationService [organization-service
                                     tarjonta-service
                                     ohjausparametrit-service
                                     audit-logger
                                     person-service
                                     valinta-tulos-service
                                     koodisto-cache
                                     job-runner]
  ApplicationService
  (get-person
    [_ application]
    (let [person-from-onr (some->> (:person-oid application)
                                   (person-service/get-person person-service))]
      (person-service/parse-person application person-from-onr)))

  ;;  Get application that has human-readable koodisto values populated
  ;;  onto raw koodi values."
  (get-application-with-human-readable-koodis
    [this application-key session with-newest-form?]
    (when-let [application (aac/get-latest-application-by-key
                             organization-service
                             tarjonta-service
                             audit-logger
                             session
                             application-key)]
      (let [tarjonta-info         (tarjonta-parser/parse-tarjonta-info-by-haku
                                    koodisto-cache
                                    tarjonta-service
                                    organization-service
                                    ohjausparametrit-service
                                    (:haku application)
                                    (:hakukohde application))
            form-in-application   (form-store/fetch-by-id (:form application))
            newest-form           (form-store/fetch-by-key (:key form-in-application))
            form                  (populate-form-fields (if with-newest-form?
                                                          newest-form
                                                          form-in-application) koodisto-cache tarjonta-info)
            forms-differ?         (and (not with-newest-form?)
                                       (forms-differ? application tarjonta-info form
                                                      (populate-form-fields newest-form koodisto-cache tarjonta-info)))
            alternative-form      (some-> (when forms-differ?
                                            newest-form)
                                          (assoc :content [])
                                          (dissoc :organization-oid))
            hakukohde-reviews     (future (parse-application-hakukohde-reviews application-key))
            attachment-reviews    (future (parse-application-attachment-reviews application-key))
            events                (future (get-application-events organization-service application-key))
            review                (future (application-store/get-application-review application-key))
            review-notes          (future (map (partial enrich-virkailija-organizations organization-service)
                                               (application-store/get-application-review-notes application-key)))
            information-requests  (future (information-request-store/get-information-requests application-key))]
        (util/remove-nil-values {:application           (-> application
                                                            (dissoc :person-oid)
                                                            (assoc :person (get-person this application))
                                                            (merge tarjonta-info))
                                 :form                  form
                                 :latest-form           alternative-form
                                 :hakukohde-reviews     @hakukohde-reviews
                                 :attachment-reviews    @attachment-reviews
                                 :events                @events
                                 :review                @review
                                 :review-notes          @review-notes
                                 :information-requests  @information-requests}))))

  (get-excel-report-of-applications-by-key
    [_ application-keys selected-hakukohde selected-hakukohderyhma included-ids session]
    (when (aac/applications-access-authorized? organization-service tarjonta-service session application-keys [:view-applications :edit-applications])
      (let [applications                     (application-store/get-applications-by-keys application-keys)
            application-reviews              (->> applications
                                                  (map :key)
                                                  application-store/get-application-reviews-by-keys
                                                  (reduce #(assoc %1 (:application-key %2) %2) {}))
            application-review-notes         (->> applications
                                                  (map :key)
                                                  application-store/get-application-review-notes-by-keys
                                                  (group-by :application-key))
            onr-persons                      (->> (map :person-oid applications)
                                                  distinct
                                                  (filter some?)
                                                  (person-service/get-persons person-service))
            applications-with-persons        (map (fn [application]
                                                      (assoc application
                                                             :person (->> (:person-oid application)
                                                                          (get onr-persons)
                                                                          (person-service/parse-person application))))
                                                  applications)
            hakukohteiden-ehdolliset         (hakukohteiden-ehdolliset valinta-tulos-service applications)
            skip-answers-to-preserve-memory? (if (not-empty included-ids)
                                               (<= 200000 (count applications))
                                               (<= 4500 (count applications)))
            lang                             (keyword (or (-> session :identity :lang) :fi))]
        (when skip-answers-to-preserve-memory? (log/warn "Answers will be skipped to preserve memory"))
        (ByteArrayInputStream. (excel/export-applications applications-with-persons
                                                          application-reviews
                                                          application-review-notes
                                                          selected-hakukohde
                                                          selected-hakukohderyhma
                                                          skip-answers-to-preserve-memory?
                                                          included-ids
                                                          lang
                                                          hakukohteiden-ehdolliset
                                                          tarjonta-service
                                                          koodisto-cache
                                                          organization-service
                                                          ohjausparametrit-service)))))

  (save-application-review
    [_ session review]
    (let [application-key (:application-key review)]
      (when (aac/applications-access-authorized?
             organization-service
             tarjonta-service
             session
             [application-key]
             [:edit-applications])
        (when-let [event-id (application-store/save-application-review review session audit-logger)]
          (tutkintojen-tunnustaminen/start-tutkintojen-tunnustaminen-review-state-changed-job
           job-runner
           event-id))
        (save-application-hakukohde-reviews application-key (:hakukohde-reviews review) session audit-logger)
        (save-attachment-hakukohde-reviews application-key (:attachment-reviews review) session audit-logger)
        {:events (get-application-events organization-service application-key)})))

  (mass-update-application-states
    [_ session application-keys hakukohde-oid from-state to-state]
    (when (aac/applications-access-authorized?
           organization-service
           tarjonta-service
           session
           application-keys
           [:edit-applications])
      (application-store/mass-update-application-states
       session
       application-keys
       hakukohde-oid
       from-state
       to-state
       audit-logger)))

  (send-modify-application-link-email
    [_ application-key session]
    (when-let [application-id (:id (aac/get-latest-application-by-key
                                    organization-service
                                    tarjonta-service
                                    audit-logger
                                    session
                                    application-key))]
      (application-store/add-new-secret-to-application application-key)
      (email/start-email-submit-confirmation-job koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id)
      (enrich-virkailija-organizations
       organization-service
       (application-store/add-application-event {:application-key application-key
                                                 :event-type      "modification-link-sent"}
                                                session))))

  (add-review-note [_ session note]
    (when (aac/applications-access-authorized?
           organization-service
           tarjonta-service
           session
           [(:application-key note)]
           [:view-applications :edit-applications])
      (enrich-virkailija-organizations
       organization-service
       (application-store/add-review-note note session))))

  (get-application-version-changes
    [_ koodisto-cache session application-key]
    (when (aac/applications-access-authorized?
           organization-service
           tarjonta-service
           session
           [application-key]
           [:view-applications :edit-applications])
      (application-store/get-application-version-changes koodisto-cache
                                                         application-key)))

  (omatsivut-applications
    [_ session person-oid]
    (->> (get (person-service/linked-oids person-service [person-oid]) person-oid)
         :linked-oids
         (mapcat #(aac/omatsivut-applications organization-service session %))))

  (get-applications-for-valintalaskenta
    [_ session hakukohde-oid application-keys]
    (if-let [applications (aac/get-applications-for-valintalaskenta
                            organization-service
                            session
                            hakukohde-oid
                            application-keys)]
      (let [henkilot        (->> applications
                                 (map :personOid)
                                 distinct
                                 (person-service/get-persons person-service))
            yksiloimattomat (->> henkilot
                                 vals
                                 (remove #(or (:yksiloity %)
                                              (:yksiloityVTJ %)))
                                 (map :oidHenkilo)
                                 distinct
                                 seq)]
        {:yksiloimattomat yksiloimattomat
         :applications    (map (partial add-asiointikieli henkilot) applications)})
      {:unauthorized nil}))

  (siirto-applications
    [_ session hakukohde-oid application-keys]
    (if-let [applications (aac/siirto-applications
                           tarjonta-service
                           organization-service
                           session
                           hakukohde-oid
                           application-keys)]
      (let [henkilot        (->> applications
                                 (map :personOid)
                                 distinct
                                 (person-service/get-persons person-service))
            yksiloimattomat (->> henkilot
                                 (keep (fn [[oid h]]
                                         (when-not (or (:yksiloity h)
                                                       (:yksiloityVTJ h))
                                           oid)))
                                 distinct
                                 seq)]
        {:yksiloimattomat yksiloimattomat
         :applications    (map (partial add-henkilo henkilot) applications)})
      {:unauthorized nil}))

  (suoritusrekisteri-applications
    [_ haku-oid hakukohde-oids person-oids modified-after offset]
    (let [person-oids (when (seq person-oids)
                        (mapcat #(:linked-oids (second %)) (person-service/linked-oids person-service person-oids)))]
      (application-store/suoritusrekisteri-applications haku-oid hakukohde-oids person-oids modified-after offset))))

(defn remove-review-note [note-id]
  (application-store/remove-review-note note-id))

(defn- init-cipher
  [nonce mode]
  (let [cipher   (Cipher/getInstance "AES/GCM/NoPadding")
        key      (-> (:secret-key (:application-key-masking config))
                     (.getBytes "UTF-8")
                     ((fn [bs] (.decode (Base64/getDecoder) bs)))
                     (SecretKeySpec. "AES"))
        gcm-spec (new GCMParameterSpec 128 nonce 0 12)]
    (.init cipher mode key gcm-spec)
    cipher))

(defn mask-application-key
  [application-key]
  (let [nonce (let [ba (byte-array 12)]
                (.nextBytes (new SecureRandom) ba)
                ba)]
    (new String
         (->> (.doFinal (init-cipher nonce Cipher/ENCRYPT_MODE) (.getBytes application-key "UTF-8"))
              (concat nonce)
              byte-array
              (.encode (Base64/getUrlEncoder)))
         "UTF-8")))

(defn unmask-application-key
  [string]
  (try
    (let [input (.decode (Base64/getUrlDecoder) (.getBytes string "UTF-8"))]
      (new String
           (.doFinal (init-cipher input Cipher/DECRYPT_MODE) input 12 (- (alength input) 12))
           "UTF-8"))
    (catch Exception e
      (log/error e "Failed to unmask" string)
      nil)))

(defn new-application-service [] (->CommonApplicationService nil nil nil nil nil nil nil nil))
