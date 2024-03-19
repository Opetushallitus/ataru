(ns ataru.db.migration-implementations
  (:require [ataru.application.review-states :as review-states]
            [ataru.applications.application-store :as application-store]
            [ataru.background-job.job-store :as job-store]
            [ataru.cas.client :as cas]
            [ataru.component-data.person-info-module :as person-info-module]
            [ataru.component-data.value-transformers :as t]
            [ataru.config.core :refer [config]]
            [ataru.db.db :refer [get-datasource]]
            [ataru.db.migrations.application-migration-store :as migration-app-store]
            [ataru.db.migrations.form-migration-store :as migration-form-store]
            [ataru.forms.form-store :as store]
            [ataru.hakija.background-jobs.attachment-finalizer-job :as attachment-finalizer-job]
            [ataru.kayttooikeus-service.kayttooikeus-service :as kayttooikeus-service]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.koodisto.koodisto-db-cache :as koodisto-cache]
            [ataru.person-service.person-service :as person-service]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.util :as util]
            [ataru.util.random :as c]
            [camel-snake-kebab.core :refer [->camelCaseKeyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.core.match :refer [match]]
            [clojure.java.jdbc :as jdbc :refer [with-db-transaction]]
            [clojure.set]
            [clojure.string]
            [clojure.walk]
            [taoensso.timbre :as log]
            [ataru.component-data.component :as component]
            [ataru.translations.texts :refer [email-default-texts]]
            [medley.core :refer [find-first]]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-job :as harkinnanvaraisuus-job])
  (:import (java.time ZonedDateTime ZoneId)))

(defonce migration-session {:user-agent "migration"})

(def default-fetch-size 50)

(def audit-logger (atom nil))

(defn- with-query-results-cursor [conn [sql & params] func]
  (with-open [stmt (.prepareStatement (jdbc/get-connection conn) sql)]
    (doseq [[index value] (map vector (iterate inc 1) params)]
      (.setObject stmt index value))
    (.setFetchSize stmt default-fetch-size)
    (with-open [rset (.executeQuery stmt)]
      (func (jdbc/result-set-seq rset)))))

(defn- with-update-cursor [conn sql]
  (with-open [stmt (.prepareStatement (jdbc/get-connection conn) sql)]
    (let [rset (.execute stmt)]
      rset)))

(defn- update-person-info-module
  [new-person-info-module form]
  (clojure.walk/prewalk
    (fn [expr]
      (match expr
        {:module (:or :person-info "person-info")}
        new-person-info-module
        :else expr))
    form))

(defn update-birth-date-place-holder []
  (doseq [form (->> (migration-form-store/get-all-forms)
                    (map #(store/fetch-by-id (:id %)))
                    (sort-by :created-time))]
    (store/create-form-or-increment-version!
     (clojure.walk/prewalk
      (fn [expr]
        (match expr
          {:id "birth-date"}
          (assoc-in expr [:params :placeholder]
                    {:fi "pp.kk.vvvv"
                     :sv "dd.mm.åååå"
                     :en "dd.mm.yyyy"})
          :else expr))
      form)
      migration-session
      @audit-logger)))

(defn refresh-person-info-modules []
  (let [new-person-module (person-info-module/person-info-module)]
    (doseq [form (->> (migration-form-store/get-all-forms)
                      (map #(store/fetch-by-id (:id %)))
                      (sort-by :created-time))]
      (store/create-form-or-increment-version!
       (update-person-info-module new-person-module form)
        migration-session
        @audit-logger))))

(defn inject-hakukohde-component-if-missing
  "Add hakukohde component to legacy forms (new ones have one added on creation)"
  [form]
  (let [has-hakukohde-component? (-> (filter #(= (keyword (:id %)) :hakukohteet) (get-in form [:content :content]))
                                     (first)
                                     (not-empty))]
    (if has-hakukohde-component?
      nil
      (update-in form [:content :content] #(into [(component/hakukohteet)] %)))))

(defn migrate-legacy-form-content-to-contain-hakukohteet-module [connection]
  (with-db-transaction [conn {:connection connection}]
    (let [update (fn [form conn]
                     (log/info "Updating followups of form-id:" (:id form))
                     (jdbc/execute! conn ["update forms set content = ? where id = ?" (:content form) (:id form)]))]
      (doseq [form (->> (migration-form-store/get-all-forms)
                        (map #(migration-app-store/fetch-by-id (:id %) conn)))]
        (some->
          form
          inject-hakukohde-component-if-missing
          (update conn))))))

(defn application-id->application-key
  "Make application_events to refer to applications using
   applications.key instead of applications.id"
  []
  (let [applications (migration-app-store/get-all-applications)]
    (doseq [application applications
            :let [application-id  (:id application)
                  application-key (:key application)]]
      (doseq [application-event (migration-app-store/get-application-events-by-application-id application-id)
              :when (nil? (:application-key application-event))
              :let [event-id (:id application-event)]]
        (migration-app-store/set-application-key-to-application-event event-id application-key))
      (doseq [confirmation-email (migration-app-store/get-application-confirmation-emails application-id)
              :when (nil? (:application-key confirmation-email))
              :let [confirmation-id (:id confirmation-email)]]
        (migration-app-store/set-application-key-to-application-confirmation-email confirmation-id application-key))
      (let [application-review (migration-app-store/get-application-review-by-application-id application-id)]
        (when (nil? (:application-key application-review))
          (let [review-id (:id application-review)]
            (migration-app-store/set-application-key-to-application-review review-id application-key)))))))

(defn- secrets->keys
  [secrets {:keys [key]}]
  (if-not (contains? secrets key)
    (let [secret (c/url-part 34)]
      (assoc secrets key secret))
    secrets))

(defn- secret->application [application]
  (let [secret (migration-app-store/get-application-secret application)]
    (assoc application :secret secret)))

(defn populate-application-secrets
  []
  (let [applications (->> (migration-app-store/get-all-applications)
                          (map secret->application))
        secrets      (reduce secrets->keys {} applications)]
    (doseq [{:keys [key] :as application} applications
            :let [secret (get secrets key)]]
      (migration-app-store/set-application-secret application secret))))

(defn- get-haku-for-hakukohde
  [hakukohde-oid]
  (log/info "Loading hakukohde" hakukohde-oid)
  (when-let [haku-oid (:haku-oid (tarjonta-client/get-hakukohde hakukohde-oid))]
    (tarjonta-client/get-haku haku-oid)))

(def memo-get-haku-for-hakukohde (memoize get-haku-for-hakukohde))

(defn add-haku-details-for-applications
  []
  (doseq [{:keys [id hakukohde]} (migration-app-store/get-applications-without-haku)]
    (if-let [haku (memo-get-haku-for-hakukohde hakukohde)]
      (do
        (migration-app-store/update-application-add-haku id haku)
        (log/info "Updated haku details for application" id))
      (log/error "Could not update haku for application" id "with hakukohde" hakukohde))))

(defn- wrap-followups [form]
  (let [fw           (atom nil)
        wrapped-form (clojure.walk/prewalk
                       (fn [expr]
                         (match expr
                           {:followup followup}
                           (do
                             (reset! fw followup)
                             (-> (dissoc expr :followup)
                                 (assoc :followups [followup])))

                           :else expr))
                       form)]
    (when @fw
      wrapped-form)))

(defn followups-to-vectored-followups
  []
  (let [existing-forms (try
                         (map #(store/fetch-by-id (:id %)) (migration-form-store/get-all-forms))
                         (catch Exception _ []))
        wrap (fn [w]
                 (store/create-form-or-increment-version! w migration-session @audit-logger))]
    (doseq [form existing-forms]
      (some-> form
              wrap-followups
              (wrap)))))

(defn followups-to-vectored-followups-like-all-of-them
  []
  (let [update (fn [form conn]
                 (log/info "Updating followups of form-id:" (:id form))
                 (jdbc/execute! conn ["update forms set content = ? where id = ?" (:content form) (:id form)]))]
    (with-db-transaction [conn {:datasource (get-datasource :db)}]
      (with-query-results-cursor conn ["select id, content from forms"]
        (fn [forms]
          (doseq [form forms]
            (some->
                form
                wrap-followups
              (update conn))))))))

; oph organization
(def fake-session {:identity
                   {:username      "Admin"
                    :organizations [{:oid "1.2.246.562.10.00000000001"}]}})

(defn- create-new-review-state
  [application]
  (let [application-key (:key application)
        old-review      (application-store/get-application-review application-key)
        old-state       (:state old-review)
        hakukohteet     (if (pos? (count (:hakukohde application)))
                          (:hakukohde application)
                          ["form"])
        [application-state selection-state] (case (keyword old-state)
                                              :unprocessed ["unprocessed" "incomplete"]
                                              :processing ["processing" "incomplete"]
                                              :invited-to-interview ["invited-to-interview" "incomplete"]
                                              :invited-to-exam ["invited-to-exam" "incomplete"]
                                              :evaluating ["evaluating" "incomplete"]
                                              :valintaesitys ["valintaesitys" "incomplete"]
                                              :processed ["processed" "incomplete"]
                                              :inactivated ["inactivated" "incomplete"]
                                              :not-selected ["processed" "reserve"]
                                              :selection-proposal ["processed" "selection-proposal"]
                                              :selected ["processed" "selected"]
                                              :applicant-has-accepted ["processed" "selected"]
                                              :rejected ["processed" "rejected"]
                                              :canceled ["inactivated" "incomplete"])]
    (log/info "Creating new review state for application" application-key "in state" old-state)
    (when (not= old-state application-state)
      (log/info "Updating application state:" old-state "->" application-state)
      (application-store/save-application-review (merge old-review {:state application-state}) fake-session @audit-logger))
    (when (= 1 (count hakukohteet))
      (log/info "Updating hakukohde" (first hakukohteet) "to state" selection-state)
      (application-store/save-application-hakukohde-review
        (:key application)
        (first hakukohteet)
        "selection-state"
        selection-state
        fake-session
        @audit-logger))))

(defn application-reviews->new-model
  []
  (doseq [application (migration-app-store/get-all-applications)]
    (create-new-review-state application)))

(defn dob->dd-mm-yyyy-format [connection]
  (with-db-transaction [conn {:connection connection}]
    (letfn [(invalid-dob-format? [[day month _]]
              (and (some? day)
                   (some? month)
                   (or (< (count day) 2)
                       (< (count month) 2))))
            (application-with-invalid-dob-format? [application]
              (->> application
                   :content
                   :answers
                   (filter (fn [answer]
                             (and (= (:key answer) "birth-date")
                                  (not (clojure.string/blank? (:value answer))))))
                   (eduction (map :value)
                             (map #(clojure.string/split % #"\.")))
                   (first)
                   (invalid-dob-format?)))
            (->dd-mm-yyyy-format [application]
              (update-in application [:content :answers] (partial map (fn [answer]
                                                                        (cond-> answer
                                                                          (= (:key answer) "birth-date")
                                                                          (update :value t/birth-date))))))
            (->applications [applications application]
              (if-let [application-key (:key application)]
                (-> applications
                    (update application-key (fnil identity []))
                    (update application-key conj application))
                applications))
            (latest-application-id [applications]
              (->> applications
                   (sort-by :created-time)
                   (last)
                   :id))]
      (let [applications (->> (migration-app-store/get-all-applications)
                              (filter application-with-invalid-dob-format?)
                              (map ->dd-mm-yyyy-format)
                              (reduce ->applications {}))]
        (doseq [[application-key applications] applications]
          (doseq [application applications]
            (log/info (str "Updating date of birth answer of application " (:id application)))
            (migration-app-store/update-application-content (:id application) (:content application)))
          (when-let [application-id (latest-application-id applications)]
            (log/info (str "Starting new person service job for application " application-id " (key: " application-key ")"))
            (job-store/store-new conn
                                 (:type person-integration/job-definition)
                                 {:application-id application-id}))))))

  )

(defn camel-case-content-keys []
  (doseq [application (migration-app-store/get-all-applications)]
    (let [camel-cased-content (transform-keys ->camelCaseKeyword
                                                (:content application))]
      (when (not= camel-cased-content (:content application))
        (log/info "Camel casing keywords of application" (:id application))
        (migration-app-store/update-application-content
         (:id application)
         camel-cased-content)))))

(defn review-notes->own-table []
  (doseq [review-note (->> (migration-app-store/get-all-application-reviews)
                           (filter (comp not clojure.string/blank? :notes)))]
    (let [application-key (:application-key review-note)]
      (log/info (str "Migrating review notes of application " application-key))
      (migration-app-store/create-application-review-note review-note))))

(defn application-states-to-hakukohteet
  []
  (let [states->set            #(->> % (map first) (set))
        new-application-states (states->set review-states/application-review-states)
        new-hakukohde-states   (states->set review-states/application-hakukohde-processing-states)]
    (doseq [{:keys [hakukohde key] :as application} (migration-app-store/get-latest-versions-of-all-applications)]
      (let [review (application-store/get-application-review key)
            state  (:state review)]
        (when (and
                (not (contains? new-application-states state))
                (contains? new-hakukohde-states state))
          (doseq [hakukohde-oid-or-form (or (not-empty hakukohde) ["form"])]
            (println "Creating new hakukohde-review" (:key application) (:id application) "->" hakukohde-oid-or-form state)
            (application-store/save-application-hakukohde-review
              key
              hakukohde-oid-or-form
              "processing-state"
              state
              fake-session
              @audit-logger)))
        (let [new-application-state (if (= state "inactivated")
                                      "inactivated"
                                      "active")]
          (when (not= new-application-state state)
            (println "Updating application review state" key (:id application) state "->" new-application-state)
            (migration-app-store/set-application-state key new-application-state)))))))

(defn start-attachment-finalizer-job-for-all-applications
  [connection]
  (with-db-transaction [conn {:connection connection}]
    (doseq [application-id (migration-app-store/get-ids-of-latest-applications)]
      (job-store/store-new conn
                           (:type attachment-finalizer-job/job-definition)
                           {:application-id application-id}))))

(defn migrate-add-harkinnanvaraisuus-checks
  [connection]
  (with-db-transaction [conn {:connection connection}]
    (job-store/store-new conn
                         (:type harkinnanvaraisuus-job/job-definition)
                         {})))

(defn migrate-add-harkinnanvaraisuus-rechecks
  [connection]
  (with-db-transaction [conn {:connection connection}]
    (job-store/store-new conn
                         (:type harkinnanvaraisuus-job/recheck-job-definition)
                         {})))

(defn- update-home-town
  [new-home-town-component form]
  (clojure.walk/prewalk
   (fn [e]
     (if (= "home-town" (:id e))
       new-home-town-component
       e))
   form))

(defn- update-kotikunta-answer
  [kunnat application]
  (update-in application [:content :answers]
             (partial map (fn [a]
                            (if (and (= "home-town" (:key a))
                                     (not (clojure.string/blank? (:value a))))
                              (if-let [match (kunnat (clojure.string/lower-case (:value a)))]
                                (assoc a :value match)
                                a)
                              a)))))

(defn migrate-kotikunta-from-text-to-code
  [connection]
  (with-db-transaction [conn {:connection connection}]
    (let [new-home-town {:fieldClass                     "formField"
                         :fieldType                      "dropdown"
                         :id                             "home-town"
                         :label                          {:fi "Kotikunta" :sv "Hemkommun" :en "Home town"}
                         :params                         {}
                         :options                        [{:value "" :label {:fi "" :sv "" :en ""}}]
                         :validators                     ["home-town"]
                         :koodisto-source                {:uri "kunta" :version 1}
                         :exclude-from-answers-if-hidden true}
          kunnat        (reduce #(assoc %1
                                        (clojure.string/lower-case (:fi (:label %2)))
                                        (:value %2)
                                        (clojure.string/lower-case (:sv (:label %2)))
                                        (:value %2))
                                {}
                                (koodisto-cache/get-koodi-options (koodisto/encode-koodisto-key {:uri "kunta" :version 1})))]
      (doseq [form (migration-app-store/get-1.86-forms conn)
              :let [new-form (update-home-town new-home-town form)]]
        (if (= (:content new-form) (:content form))
          (log/info "Not updating form" (:key form))
          (let [{:keys [id key]} (migration-app-store/insert-1.86-form conn new-form)]
            (log/info "Updating form" (:key form))
            (doseq [application (migration-app-store/get-1.86-applications conn key)
                    :let [new-application (update-kotikunta-answer kunnat application)]]
              (if (or (= (:content new-application) (:content application))
                      (not= (:form_id application) (:id form)))
                (log/info "Not updating application" (:key application))
                (do (log/info "Updating application" (:key application))
                    (migration-app-store/insert-1.86-application
                     conn
                     (assoc new-application :form_id id)))))))))))

(def system-metadata
  {:created-by  {:name "system"
                 :oid  "system"
                 :date "1970-01-01T00:00:00Z"}
   :modified-by {:name "system"
                 :oid  "system"
                 :date "1970-01-01T00:00:00Z"}})

(defn- get-field-metadata
  [virkailija]
  {:created-by  {:name (format "%s %s" (:kutsumanimi virkailija) (:sukunimi virkailija))
                 :oid  (:oidHenkilo virkailija)
                 :date (ZonedDateTime/now (ZoneId/of "Europe/Helsinki"))}
   :modified-by {:name (format "%s %s" (:kutsumanimi virkailija) (:sukunimi virkailija))
                 :oid  (:oidHenkilo virkailija)
                 :date (ZonedDateTime/now (ZoneId/of "Europe/Helsinki"))}})

(defn migrate-element-metadata-to-forms
  [connection]
  (with-db-transaction [conn {:connection connection}]
    (let [kayttooikeus-service (if (-> config :dev :fake-dependencies)
                                 (kayttooikeus-service/->FakeKayttooikeusService)
                                 (kayttooikeus-service/->HttpKayttooikeusService
                                  (cas/new-client "/kayttooikeus-service" "j_spring_cas_security_check"
                                                  "JSESSIONID" (-> config :public-config :virkailija-caller-id))))
          person-service       (person-service/new-person-service)
          get-virkailija       (memoize (fn [username]
                                          (->> username
                                               (kayttooikeus-service/virkailija-by-username kayttooikeus-service)
                                               :oid
                                               (person-service/get-person person-service))))]
      (doseq [id   (migration-app-store/get-1.88-form-ids conn)
              :let [form           (migration-app-store/get-1.88-form conn id)
                    virkailija     (get-virkailija (:created_by form))
                    field-metadata (get-field-metadata virkailija)]]
        (-> (:content form)
            (update :content
                    (fn [content]
                      (for [field content
                            :let  [metadata (if (or (= "hakukohteet" (:id field))
                                                    (= "person-info" (:module field)))
                                              system-metadata
                                              field-metadata)]]
                        (clojure.walk/prewalk (fn [x]
                                                (if (and (map? x) (contains? x :fieldType))
                                                  (assoc x :metadata metadata)
                                                  x))
                                              field))))
            (migration-app-store/update-1.88-form-content (:id form) conn))))))

(defn- create-attachment-reviews
  [attachment-field application-key hakutoiveet]
  (let [review-base                        {:application_key application-key
                                            :attachment_key  (:id attachment-field)
                                            :state           "not-checked"}
        relevant-field-hakukohde-oids      (clojure.set/intersection (set (map :oid hakutoiveet))
                                                                     (-> attachment-field :belongs-to-hakukohteet set))
        relevant-field-hakukohderyhma-oids (->> hakutoiveet
                                                (filter #(not-empty (clojure.set/intersection (-> attachment-field :belongs-to-hakukohderyhma set)
                                                                                              (-> % :ryhmaliitokset set))))
                                                (map :oid))
        hakukohde-oids                     (concat relevant-field-hakukohde-oids relevant-field-hakukohderyhma-oids)]
    (map #(assoc review-base :hakukohde %)
         (cond
           (or (-> attachment-field :belongs-to-hakukohderyhma not-empty)
               (-> attachment-field :belongs-to-hakukohteet not-empty))
           hakukohde-oids

           (not-empty hakutoiveet)
           (map :oid hakutoiveet)

           :else ["form"]))))

(defn- followup-option-selected?
  [field answers]
  (let [parent-answer-key (-> field :followup-of keyword)
        answers    (-> answers
                       parent-answer-key
                       :value
                       vector ; Make sure we won't flatten a string answer to ()
                       flatten
                       set)]
    (contains? answers (:option-value field))))

(defn- filter-relevant-attachments
  [answers fields]
  (filter (fn [field]
            (and (= "attachment" (:fieldType field))
                 (or (not (contains? field :followup-of))
                     (followup-option-selected? field answers))))
          fields))

(defn migrate-add-subject-and-content-finish []
  (let [submit-email-subjects (get-in email-default-texts [:email-submit-confirmation-template :submit-email-subjects])
        email-content-ending  (get-in email-default-texts [:email-submit-confirmation-template :without-application-period])]
    (with-db-transaction [connection {:datasource (get-datasource :db)}]
      (with-update-cursor connection "alter table email_templates add column subject TEXT not null default ''")
      (with-update-cursor connection "alter table email_templates add column content_ending TEXT not null default ''")
      (doseq [lang [:fi :sv :en]]
        (migration-app-store/set-1_96-content-ending! connection (name lang) (get email-content-ending lang))
        (migration-app-store/set-1_96-subject! connection (name lang) (get submit-email-subjects lang))))))

(defn migrate-attachment-states-to-applications
  [connection]
  (with-db-transaction [conn {:connection connection}]
    (let [get-cached-hakukohde-and-ryhmaliitokset (memoize tarjonta-client/get-hakukohde)]
      (doseq [key    (migration-app-store/get-1.92-latest-application-keys conn)
              :let [application (migration-app-store/get-1.92-application conn key)
                    hakutoiveet (map get-cached-hakukohde-and-ryhmaliitokset (:hakukohde application))
                    form        (migration-app-store/get-1.92-form-by-id conn (:form_id application))]
              review (->> form
                          :content
                          :content
                          util/flatten-form-fields
                          (filter-relevant-attachments (-> application :content :answers util/answers-by-key))
                          (mapcat #(create-attachment-reviews % (:key application) hakutoiveet)))]
        (migration-app-store/insert-1.92-attachment-review conn review)))))

(defn migrate-nationality-to-question-group
  [connection]
  (with-db-transaction [conn {:connection connection}]
    (letfn [(nationality-answer? [answer]
              (= (:key answer) "nationality"))
            (nationality-answer->question-group [application]
              (let [answers                     (-> application :content :answers)
                    old-nationality-answer      (find-first nationality-answer? answers)
                    new-nationality-answer      (assoc old-nationality-answer :value [[(:value old-nationality-answer)]])
                    answers-without-nationality (remove nationality-answer? answers)]
                (when old-nationality-answer
                  (assoc-in application [:content :answers] (conj answers-without-nationality new-nationality-answer)))))]
      (let [new-person-info-module (person-info-module/person-info-module)]
        (doseq [form-id (migration-app-store/get-1.100-form-ids conn)
                :let [form     (migration-app-store/get-1.100-form conn form-id)
                      new-form (update-person-info-module new-person-info-module form)]]
          (if (= (:content new-form) (:content form))
            (log/info "1.100: Not updating form" (:key form) form-id)
            (let [{:keys [id]} (migration-app-store/insert-1.100-form conn new-form)]
              (log/info "1.100: Updating form" (:key form) form-id)
              (doseq [application (migration-app-store/get-1.100-applications conn form-id)
                      :let [new-application (nationality-answer->question-group application)]]
                (if (or (not new-application)
                        (= (:content new-application) (:content application))
                        (not= (:form_id application) (:id form)))
                  (log/info "1.100: Not updating application" (:key application) (:id application))
                  (do (log/info "1.100: Updating application" (:key application) (:id application))
                      (migration-app-store/insert-1.100-application
                        conn
                        (assoc new-application :form_id id))))))))))))