(ns ataru.applications.application-service
  (:require
    [ataru.applications.application-access-control :as aac]
    [ataru.applications.application-store :as application-store]
    [ataru.applications.excel-export :as excel]
    [ataru.email.application-email-confirmation :as email]
    [ataru.forms.form-access-control :as form-access-control]
    [ataru.forms.form-store :as form-store]
    [ataru.hakija.hakija-form-service :as hakija-form-service]
    [ataru.information-request.information-request-store :as information-request-store]
    [ataru.koodisto.koodisto :as koodisto]
    [ataru.middleware.user-feedback :refer [user-feedback-exception]]
    [ataru.person-service.birth-date-converter :as bd-converter]
    [ataru.person-service.person-service :as person-service]
    [ataru.tarjonta-service.hakukohde :refer [populate-hakukohde-answer-options]]
    [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
    [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
    [ataru.util :as util]
    [ataru.applications.filtering :as application-filtering]
    [clojure.data :refer [diff]]
    [ataru.virkailija.editor.form-utils :refer [visible?]]
    [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
    [medley.core :refer [find-first filter-vals]]
    [taoensso.timbre :refer [spy debug]]
    [ataru.application.review-states :as review-states]
    [ataru.application.application-states :as application-states]
    [ataru.schema.form-schema :as ataru-schema]
    [ataru.organization-service.session-organizations :as session-orgs]
    [schema.core :as s]
    [ataru.dob :as dob])
  (:import [java.io ByteArrayInputStream]))

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

(defn- person-info-from-application [application]
  (let [answers (util/answers-by-key (:answers application))]
    {:first-name     (-> answers :first-name :value)
     :preferred-name (-> answers :preferred-name :value)
     :last-name      (-> answers :last-name :value)
     :ssn            (-> answers :ssn :value)
     :birth-date     (-> answers :birth-date :value)
     :gender         (-> answers :gender :value)
     :nationality    (-> answers :nationality :value)
     :language       (-> answers :language :value)}))

(defn- person-info-from-onr-person [person]
  {:first-name     (:etunimet person)
   :preferred-name (:kutsumanimi person)
   :last-name      (:sukunimi person)
   :ssn            (:hetu person)
   :birth-date     (some-> person :syntymaaika bd-converter/convert-to-finnish-format)
   :gender         (-> person :sukupuoli)
   :nationality    (->> (-> person :kansalaisuus)
                        (mapv #(vector (get % :kansalaisuusKoodi "999"))))
   :language       (try
                     (-> person :aidinkieli :kieliKoodi clojure.string/upper-case)
                     (catch Exception e
                       (throw (new RuntimeException
                                   (str "Could not parse aidinkieli "
                                        (:aidinkieli person)
                                        "of person "
                                        (:oidHenkilo person))
                                   e))))})

(defn parse-person [application person-from-onr]
  (let [yksiloity       (or (-> person-from-onr :yksiloity)
                            (-> person-from-onr :yksiloityVTJ))
        person-info     (if yksiloity
                          (person-info-from-onr-person person-from-onr)
                          (person-info-from-application application))]
    (merge
      {:oid         (:person-oid application)
       :turvakielto (-> person-from-onr :turvakielto boolean)
       :yksiloity   (boolean yksiloity)}
      person-info)))

(defn get-person
  [application person-client]
  (let [person-from-onr (some->> (:person-oid application)
                                 (person-service/get-person person-client))]
    (parse-person application person-from-onr)))

(defn- populate-form-fields
  [form koodisto-cache tarjonta-info]
  (-> (koodisto/populate-form-koodisto-fields koodisto-cache form)
      (populate-hakukohde-answer-options tarjonta-info)
      (hakija-form-service/populate-can-submit-multiple-applications tarjonta-info)))

(defn fields-equal? [[new-in-left new-in-right shared]]
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
                                       (filter #(visible? % field-by-id answers hakutoiveet
                                                          (-> tarjonta-info :tarjonta :hakukohteet)))
                                       (map remove-irrelevant-changes))))
             fields-left    (sort-by :id (visible-fields form-left))
             fields-right   (sort-by :id (visible-fields form-right))]
         (not (fields-equal? (diff fields-left fields-right))))))

(defn get-application-with-human-readable-koodis
  "Get application that has human-readable koodisto values populated
   onto raw koodi values."
  [koodisto-cache application-key session organization-service tarjonta-service ohjausparametrit-service person-client with-newest-form?]
  (when-let [application (aac/get-latest-application-by-key
                           organization-service
                           tarjonta-service
                           session
                           application-key)]
    (let [tarjonta-info        (tarjonta-parser/parse-tarjonta-info-by-haku
                                 koodisto-cache
                                 tarjonta-service
                                 organization-service
                                 ohjausparametrit-service
                                 (:haku application)
                                 (:hakukohde application))
          form-in-application  (form-store/fetch-by-id (:form application))
          newest-form          (form-store/fetch-by-key (:key form-in-application))
          form                 (populate-form-fields (if with-newest-form?
                                                       newest-form
                                                       form-in-application) koodisto-cache tarjonta-info)
          forms-differ?        (and (not with-newest-form?)
                                    (forms-differ? application tarjonta-info form
                                                   (populate-form-fields newest-form koodisto-cache tarjonta-info)))
          alternative-form     (some-> (when forms-differ?
                                             newest-form)
                                       (assoc :content [])
                                       (dissoc :organization-oid))
          hakukohde-reviews    (future (parse-application-hakukohde-reviews application-key))
          attachment-reviews   (future (parse-application-attachment-reviews application-key))
          events               (future (application-store/get-application-events application-key))
          review               (future (application-store/get-application-review application-key))
          review-notes         (future (application-store/get-application-review-notes application-key))
          information-requests (future (information-request-store/get-information-requests application-key))]
      (util/remove-nil-values {:application          (-> application
                                                         (dissoc :person-oid)
                                                         (assoc :person (get-person application person-client))
                                                         (merge tarjonta-info))
                               :form                 form
                               :latest-form          alternative-form
                               :hakukohde-reviews    @hakukohde-reviews
                               :attachment-reviews   @attachment-reviews
                               :events               @events
                               :review               @review
                               :review-notes         @review-notes
                               :information-requests @information-requests}))))

(defn ->form-query
  [key]
  {:form key})

(defn ->hakukohde-query
  [tarjonta-service hakukohde-oid ensisijaisesti]
  (let [hakukohde (tarjonta-service/get-hakukohde tarjonta-service hakukohde-oid)]
    (merge {:haku (:hakuOid hakukohde)}
           (if ensisijaisesti
             {:ensisijainen-hakukohde [hakukohde-oid]}
             {:hakukohde [hakukohde-oid]}))))

(defn ->hakukohderyhma-query
  [ryhman-hakukohteet
   authorized-organization-oids
   haku-oid
   hakukohderyhma-oid
   ensisijaisesti
   rajaus-hakukohteella]
  (let [kayttajan-hakukohteet (filter #(some authorized-organization-oids (:tarjoaja-oids %))
                                      ryhman-hakukohteet)]
    (merge {:haku      haku-oid
            :hakukohde (map :oid ryhman-hakukohteet)}
           (when ensisijaisesti
             (if (some? rajaus-hakukohteella)
               {:hakukohde                    [rajaus-hakukohteella]
                :ensisijainen-hakukohde       [rajaus-hakukohteella]
                :ensisijaisesti-hakukohteissa (map :oid ryhman-hakukohteet)}
               {:ensisijainen-hakukohde       (map :oid kayttajan-hakukohteet)
                :ensisijaisesti-hakukohteissa (map :oid ryhman-hakukohteet)})))))

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
  {:name (application-store/->name-query-value name)})

(defn ->person-oid-query
  [person-oid]
  {:person-oid person-oid})

(defn ->application-oid-query
  [application-oid]
  {:application-oid application-oid})

(defn ->application-oids-query
  [application-oids]
  {:application-oids application-oids})

(defn ->empty-query
  []
  {})

(defn ->and-query [& queries] (apply merge queries))

(defn- processing-state-counts-for-application
  [{:keys [application-hakukohde-reviews]} included-hakukohde-oid-set]
  (->> (or
         (->> application-hakukohde-reviews
              (filter (fn [review]
                        (and
                          (= "processing-state" (:requirement review))
                          (or (nil? included-hakukohde-oid-set)
                              (contains? included-hakukohde-oid-set (:hakukohde review))))))
              (not-empty))
         [{:requirement "processing-state" :state review-states/initial-application-hakukohde-processing-state}])
       (map :state)
       (frequencies)))

(defn review-state-counts
  [applications included-hakukohde-oid-set]
  (reduce
    (fn [acc application]
      (merge-with + acc (processing-state-counts-for-application application included-hakukohde-oid-set)))
    {}
    applications))

(defn- map-vals-to-zero [m]
  (into {} (for [[k v] m] [k 0])))

(defn attachment-state-counts
  [applications included-hakukohde-oid-set]
  (reduce
    (fn [acc application]
      (merge-with (fn [prev new] (+ prev (if (not-empty new) 1 0)))
                  acc
                  (group-by :state (cond->> (application-states/attachment-reviews-with-no-requirements application)
                                            (some? included-hakukohde-oid-set)
                                            (filter #(contains? included-hakukohde-oid-set (:hakukohde %)))))))
    (map-vals-to-zero review-states/attachment-hakukohde-review-types-with-no-requirements)
    applications))

(defn- populate-applications-with-person-data
  [person-service applications]
  (let [persons (person-service/get-persons
                 person-service
                 (distinct (keep :person-oid applications)))]
    (map (fn [application]
           (let [onr-person (get persons (:person-oid application))
                 person     (if (or (:yksiloity onr-person)
                                    (:yksiloityVTJ onr-person))
                              {:oid            (:oidHenkilo onr-person)
                               :preferred-name (:kutsumanimi onr-person)
                               :last-name      (:sukunimi onr-person)
                               :yksiloity      true
                               :ssn            (boolean (:hetu onr-person))}
                              {:oid            (:person-oid application)
                               :preferred-name (:preferred-name application)
                               :last-name      (:last-name application)
                               :yksiloity      false
                               :ssn            (boolean (:ssn application))})]
             (-> application
                 (assoc :person person)
                 (dissoc :ssn :person-oid :preferred-name :last-name))))
         applications)))

(defn get-application-list-by-query
  [person-service
   tarjonta-service
   authorized-organization-oids
   query
   sort
   states-and-filters]
  (let [applications            (application-store/get-application-heading-list query sort)
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

(defn get-excel-report-of-applications-by-key
  [application-keys selected-hakukohde selected-hakukohderyhma user-wants-to-skip-answers? included-ids session organization-service tarjonta-service koodisto-cache ohjausparametrit-service person-service]
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
                                                                        (parse-person application))))
                                                applications)
          skip-answers-to-preserve-memory? (if included-ids
                                             (<= 200000 (count applications))
                                             (<= 4500 (count applications)))
          skip-answers?                    (or user-wants-to-skip-answers?
                                               skip-answers-to-preserve-memory?)
          included-ids                     (or included-ids
                                               (constantly true))
          lang                             (keyword (or (-> session :identity :lang) :fi))]
      (ByteArrayInputStream. (excel/export-applications applications-with-persons
                                                        application-reviews
                                                        application-review-notes
                                                        selected-hakukohde
                                                        selected-hakukohderyhma
                                                        skip-answers?
                                                        included-ids
                                                        lang
                                                        tarjonta-service
                                                        koodisto-cache
                                                        organization-service
                                                        ohjausparametrit-service)))))

(defn- save-application-hakukohde-reviews
  [application-key hakukohde-reviews session]
  (doseq [[hakukohde review] hakukohde-reviews]
    (doseq [[review-requirement review-state] review]
      (application-store/save-application-hakukohde-review
        application-key
        (name hakukohde)
        (name review-requirement)
        (name review-state)
        session))))

(defn- save-attachment-hakukohde-reviews
  [application-key attachment-reviews session]
  (doseq [[hakukohde review] attachment-reviews
          [attachment-key review-state] review]
    (application-store/save-attachment-hakukohde-review
      application-key
      (name hakukohde)
      (name attachment-key)
      review-state
      session)))

(defn save-application-review
  [organization-service tarjonta-service session review]
  (let [application-key (:application-key review)]
    (when (aac/applications-access-authorized?
           organization-service
           tarjonta-service
           session
           [application-key]
           [:edit-applications])
      (application-store/save-application-review review session)
      (save-application-hakukohde-reviews application-key (:hakukohde-reviews review) session)
      (save-attachment-hakukohde-reviews application-key (:attachment-reviews review) session)
      {:events (application-store/get-application-events application-key)})))

(defn mass-update-application-states
  [organization-service tarjonta-service session application-keys hakukohde-oid from-state to-state]
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
     to-state)))

(defn send-modify-application-link-email
  [koodisto-cache application-key session organization-service ohjausparametrit-service tarjonta-service job-runner]
  (when-let [application-id (:id (aac/get-latest-application-by-key
                                  organization-service
                                  tarjonta-service
                                  session
                                  application-key))]
    (application-store/add-new-secret-to-application application-key)
    (email/start-email-submit-confirmation-job koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id)
    (application-store/add-application-event {:application-key application-key
                                              :event-type      "modification-link-sent"}
                                             session)))

(defn add-review-note [organization-service tarjonta-service session note]
  (when (aac/applications-access-authorized?
         organization-service
         tarjonta-service
         session
         [(:application-key note)]
         [:view-applications :edit-applications])
    (application-store/add-review-note note session)))

(defn remove-review-note [note-id]
  (application-store/remove-review-note note-id))

(defn get-application-version-changes
  [koodisto-cache organization-service tarjonta-service session application-key]
  (when (aac/applications-access-authorized?
         organization-service
         tarjonta-service
         session
         [application-key]
         [:view-applications :edit-applications])
    (application-store/get-application-version-changes koodisto-cache
                                                       application-key)))

(defn omatsivut-applications
  [organization-service person-service session person-oid]
  (->> (get (person-service/linked-oids person-service [person-oid]) person-oid)
       :linked-oids
       (mapcat #(aac/omatsivut-applications organization-service session %))))

(defn add-asiointikieli [henkilot application]
  (let [asiointikieli (or (get {"1" "fi"
                                "2" "sv"
                                "3" "en"}
                            ((:keyValues application) "asiointikieli"))
                          (get-in (get henkilot (:personOid application))
                            [:asiointiKieli :kieliKoodi])
                          (:asiointikieli application))]
    (assoc application :asiointikieli asiointikieli)))

(defn get-applications-for-valintalaskenta
  [organization-service person-service session hakukohde-oid application-keys]
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

(defn siirto-applications
  [tarjonta-service organization-service person-service session hakukohde-oid application-keys]
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
                             hakukohderyhma-oid
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
                            (->application-oid-query application-oid)))]
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

(defn suoritusrekisteri-applications
  [person-service haku-oid hakukohde-oids person-oids modified-after offset]
  (let [person-oids (when (seq person-oids)
                      (mapcat #(:linked-oids (second %)) (person-service/linked-oids person-service person-oids)))]
    (application-store/suoritusrekisteri-applications haku-oid hakukohde-oids person-oids modified-after offset)))
