(ns ataru.applications.application-store-db-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.component-data.component :as component :refer [harkinnanvaraisuus-question]]
            [ataru.forms.form-store :as form-store]
            [ataru.log.audit-log :as audit-log]
            [ataru.db.db :as db]
            [ataru.fixtures.application :as fixtures]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.util :as util]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [after around around-all context describe it should-be-nil should= should== tags]]))

(def ^:private form (atom nil))

(def ^:private test-application-id (atom nil))

(def audit-logger (audit-log/new-dummy-audit-logger))

(defn- find-application-key-by-id [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (->> (jdbc/query conn ["select key from applications where id = ?"
                                                   id])
                                 first
                                 :key)))

(defn- delete-form! [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (jdbc/delete! conn :forms ["id = ?" id])))

(defn- delete-application! [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (jdbc/delete! conn :application_hakukohde_attachment_reviews
                  ["application_key = (select key from applications where id = ?)" id])
    (jdbc/delete! conn :applications
                  ["id = ?" id])))

(defn- save-reviews-to-db! [reviews]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (store/store-reviews reviews {:connection conn})))

(defn- reset-database! []
  (let [form-with-attachment (update form-fixtures/person-info-form :content concat (:content form-fixtures/attachment-test-form))]
    (delete-application! @test-application-id)
    (reset! form (unit-test-db/init-db-fixture form-with-attachment))))

(def application (-> (filter #(= "attachments" (:key %)) fixtures/applications)
                     (first)
                     (assoc :form_id (:id form))))

(defn- create-new-reviews [application application-key]
  (let [flat-form-content (util/flatten-form-fields (:content form-fixtures/attachment-test-form))
        answers-by-key    (-> application :content :answers util/answers-by-key)
        fields-by-id      (util/form-fields-by-id form-fixtures/attachment-test-form)]
    (store/create-application-attachment-reviews
      application-key
      (store/filter-visible-attachments answers-by-key
                                        flat-form-content
                                        fields-by-id)
      answers-by-key
      {:att__1 {:value ["56756"]}
       :att__2 {:value ["32131"]}}
      []
      true
      fields-by-id
      #{})))

(def ^:dynamic *painike-application-id* (atom nil))
(def ^:dynamic *nonselected-painike-application-id* (atom nil))
(def ^:dynamic *valikko-application-id* (atom nil))
(def ^:dynamic *lista-application-id* (atom nil))
(def ^:dynamic *group-painike-application-id* (atom nil))
(def ^:dynamic *nonselected-group-painike-application-id* (atom nil))
(def ^:dynamic *group-valikko-application-id* (atom nil))
(def ^:dynamic *group-lista-application-id* (atom nil))
(def ^:dynamic *per-hakukohde-application-id* (atom nil))
(def ^:dynamic *per-hakukohde-followup-application-id* (atom nil))

(describe "listing applications"
  (tags :unit :database)

  (after
   (reset-database!))

  (it "should find application with attachments query"
    (let [existing-attachment-id "att__1"
          application-id         (-> (store/add-application
                                      (dissoc application :key)
                                      []
                                      form-fixtures/attachment-test-form
                                      {}
                                      audit-logger
                                      nil)
                                     :id)
          application-key        (find-application-key-by-id application-id)
          new-reviews            (create-new-reviews application application-key)
          query                  {:attachment-review-states {existing-attachment-id ["not-checked"]}}
          sort                   {:order-by "applicant-name" :order "asc"}]
      (reset! test-application-id application-id)
      (should== [{:application_key application-key
                  :attachment_key  "att__1"
                  :state           "not-checked"
                  :updated?        true
                  :hakukohde       "form"}
                 {:application_key application-key
                  :attachment_key  "att__2"
                  :state           "attachment-missing"
                  :updated?        true
                  :hakukohde       "form"}]
                new-reviews)
      (save-reviews-to-db! new-reviews)
      (let [queried-applications (vec (store/get-application-heading-list query sort))
            found-application    (first queried-applications)]
        (should== 1 (count queried-applications))
        (should= application-key (:key found-application)))))

  (context "with option answers query"

    (around [it]
      (let [audit-logger                             (audit-log/new-dummy-audit-logger)
            form                                     {:name             {:fi "Lomake"}
                                                      :content          [(assoc (component/single-choice-button nil)
                                                                                :key "painike"
                                                                                :options [{:value "0"
                                                                                           :label {:fi "0"}}
                                                                                          {:value "1"
                                                                                           :label {:fi "1"}}])
                                                                         (assoc (component/dropdown nil)
                                                                                :key "valikko"
                                                                                :options [{:value "0"
                                                                                           :label {:fi "0"}}
                                                                                          {:value "1"
                                                                                           :label {:fi "1"}}])
                                                                         (assoc (component/multiple-choice nil)
                                                                                :key "lista"
                                                                                :options [{:value "0"
                                                                                           :label {:fi "0"}}
                                                                                          {:value "1"
                                                                                           :label {:fi "1"}}])
                                                                         (harkinnanvaraisuus-question nil)
                                                                         (assoc (component/question-group nil)
                                                                                :children [(assoc (component/single-choice-button nil)
                                                                                                  :key "group-painike"
                                                                                                  :options [{:value "0"
                                                                                                             :label {:fi "0"}}
                                                                                                            {:value "1"
                                                                                                             :label {:fi "1"}}])
                                                                                           (assoc (component/dropdown nil)
                                                                                                  :key "group-valikko"
                                                                                                  :options [{:value "0"
                                                                                                             :label {:fi "0"}}
                                                                                                            {:value "1"
                                                                                                             :label {:fi "1"}}])
                                                                                           (assoc (component/multiple-choice nil)
                                                                                                  :key "group-lista"
                                                                                                  :options [{:value "0"
                                                                                                             :label {:fi "0"}}
                                                                                                            {:value "1"
                                                                                                             :label {:fi "1"}}])])]
                                                      :created-by       "Testaaja"
                                                      :languages        ["fi"]
                                                      :organization-oid "1.2.246.562.10.00000000001"
                                                      :deleted          nil
                                                      :locked           nil
                                                      :locked-by        nil}
            form-id                          (-> form form-store/create-new-form! :key form-store/latest-id-by-key)
            painike-application-id           (-> {:lang    "fi"
                                                  :form    form-id
                                                  :answers [{:key       "painike"
                                                             :value     "0"
                                                             :fieldType "singleChoice"}]}
                                                  (store/add-application [] form {} audit-logger nil)
                                                 :id)
            nonselected-painike-application-id       (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "painike"
                                                                     :value     nil
                                                                     :fieldType "singleChoice"}]}
                                                         (store/add-application [] form {} audit-logger nil)
                                                         :id)
            valikko-application-id                   (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "valikko"
                                                                     :value     "0"
                                                                     :fieldType "dropdown"}]}
                                             (store/add-application [] form {} audit-logger nil)
                                                         :id)
            lista-application-id                     (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "lista"
                                                                     :value     ["0"]
                                                                     :fieldType "multipleChoice"}]}
                                                          (store/add-application [] form {} audit-logger nil)
                                                         :id)
            group-painike-application-id             (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "group-painike"
                                                                     :value     [["0"]]
                                                                     :fieldType "singleChoice"}]}
                                                          (store/add-application [] form {} audit-logger nil)
                                                         :id)
            nonselected-group-painike-application-id (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "group-painike"
                                                                     :value     [[nil]]
                                                                     :fieldType "singleChoice"}]}
                                                         (store/add-application [] form {} audit-logger nil)
                                                         :id)
            group-valikko-application-id             (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "group-valikko"
                                                                     :value     [["0"]]
                                                                     :fieldType "dropdown"}]}
                                                          (store/add-application [] form {} audit-logger nil)
                                                         :id)
            group-lista-application-id               (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "group-lista"
                                                                     :value     [["0"]]
                                                                     :fieldType "multipleChoice"}]}
                                                          (store/add-application [] form {} audit-logger nil)
                                                         :id)
            per-hakukohde-application-id  (-> {
                                               :lang    "fi"
                                               :form    form-id
                                               :answers [{:key "harkinnanvaraisuus_1.2.246.562.29.123454321"
                                                          :original-question "harkinnanvaraisuus"
                                                          :value "1"
                                                          :fieldType "singleChoice"
                                                          :duplikoitu-kysymys-hakukohde-oid "1.2.246.562.29.123454321"}]}
                                              (store/add-application [] form {} audit-logger nil)
                                              :id)
            per-hakukohde-followup-application-id (-> {
                                                       :lang    "fi"
                                                       :form    form-id
                                                       :answers [{:key "harkinnanvaraisuus_1.2.246.562.29.123454321"
                                                                  :original-question "harkinnanvaraisuus"
                                                                  :value "1"
                                                                  :fieldType "singleChoice"
                                                                  :duplikoitu-kysymys-hakukohde-oid "1.2.246.562.29.123454321"}
                                                                 {:key "harkinnanvaraisuus-reason_1.2.246.562.29.123454321"
                                                                  :original-followup "harkinnanvaraisuus-reason"
                                                                  :fieldType "singleChoice"
                                                                  :value "2"
                                                                  :duplikoitu-followup-hakukohde-oid "1.2.246.562.29.123454321"}]}
                                                        (store/add-application [] form {} audit-logger nil)
                                                      :id)]
        (binding [*painike-application-id*                   painike-application-id
                  *nonselected-painike-application-id*       nonselected-painike-application-id
                  *valikko-application-id*                   valikko-application-id
                  *lista-application-id*                     lista-application-id
                  *group-painike-application-id*             group-painike-application-id
                  *nonselected-group-painike-application-id* nonselected-group-painike-application-id
                  *group-valikko-application-id*             group-valikko-application-id
                  *group-lista-application-id*               group-lista-application-id
                  *per-hakukohde-application-id*             per-hakukohde-application-id
                  *per-hakukohde-followup-application-id*    per-hakukohde-followup-application-id]
          (try
            (it)
            (finally
              (delete-application! painike-application-id)
              (delete-application! nonselected-painike-application-id)
              (delete-application! valikko-application-id)
              (delete-application! lista-application-id)
              (delete-application! group-painike-application-id)
              (delete-application! nonselected-group-painike-application-id)
              (delete-application! group-valikko-application-id)
              (delete-application! group-lista-application-id)
              (delete-application! per-hakukohde-application-id)
              (delete-application! per-hakukohde-followup-application-id)
              (delete-form! form-id))))))

    (it "should find application with Painike, yksi valittavissa"
      (should== [*painike-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "painike" :options ["0"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with nonselected Painike, yksi valittavissa"
      (should== [*nonselected-painike-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "painike" :options []}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with Valikko"
      (should== [*valikko-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "valikko" :options ["0"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with Lista, monta valittavissa"
      (should== [*lista-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "lista" :options ["0"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find applications with per-hakukohde question"
      (should= [*per-hakukohde-application-id* *per-hakukohde-followup-application-id*]
               (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "harkinnanvaraisuus" :use-original-question true :options ["1"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with per-hakukohde question using followup"
      (should= [*per-hakukohde-followup-application-id*]
               (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "harkinnanvaraisuus-reason" :use-original-followup true :options ["2"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with per-hakukohde question"
       (should= []
                (mapv :id (store/get-application-heading-list
                            {:option-answers [{:key "harkinnanvaraisuus" :use-original-question true :options ["0"]}]}
                            {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with per-hakukohde question using followup"
       (should= []
                (mapv :id (store/get-application-heading-list
                            {:option-answers [{:key "harkinnanvaraisuus-reason" :use-original-followup true :options ["1"]}]}
                            {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Painike, yksi valittavissa with nonselected answer"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "painike" :options ["1"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Valikko with nonselected answer"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "valikko" :options ["1"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Lista, monta valittavissa with nonselected answer"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "lista" :options ["1"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with Painike, yksi valittavissa in a question group"
      (should== [*group-painike-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "group-painike" :options ["0"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with nonselected Painike, yksi valittavissa in a question group"
      (should== [*nonselected-group-painike-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "group-painike" :options[]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with Valikko in a question group"
      (should== [*group-valikko-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "group-valikko" :options ["0"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with Lista, monta valittavissa in a question group"
      (should== [*group-lista-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "group-lista" :options ["0"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Painike, yksi valittavissa with nonselected answer in a question group"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "group-painike" :options ["1"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Valikko with nonselected answer in a question group"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "group-valikko" :options ["1"]}]}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Lista, monta valittavissa with nonselected answer in a question group"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers [{:key "group-lista" :options ["1"]}]}
                           {:order-by "applicant-name" :order "asc"}))))))

(describe "creating application attachment reviews in db"
  (tags :unit :attachments :attachments-db)

  (after
    (reset-database!))

  (it "should delete orphans and preserve reviews"
      (let [new-reviews (create-new-reviews application (:key application))]
        (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                                  (let [connection {:connection connection}]
                                    (store/store-reviews new-reviews connection)
                                    (should== 0 (store/delete-orphan-attachment-reviews (:key application)
                                                                                        new-reviews
                                                                                        connection))
                                    (should== 1 (store/delete-orphan-attachment-reviews (:key application)
                                                                                        [(first new-reviews)]
                                                                                        connection)))))))


(def ^:dynamic *tutu-application-key-1* (atom nil))
(def ^:dynamic *tutu-application-key-2* (atom nil))
(def ^:dynamic *tutu-application-key-3* (atom nil))
(describe "Services for TUTU"
  (tags :tutu :integration)

  (around-all [context]
    (let [
      audit-logger (audit-log/new-dummy-audit-logger)
      form {
        :name             {:fi "Lomake"}
        :content          []
        :created-by       "Testaaja"
        :languages        ["fi"]
        :organization-oid "1.2.246.562.10.00000000001"
        :deleted          nil
        :locked           nil
        :locked-by        nil
      }
      form-id (-> form form-store/create-new-form! :key form-store/latest-id-by-key)
      app-id-1 (->
        (store/add-application
          {
            :lang    "fi"
            :form    form-id
            :answers []
            :person-oid "1.2.3.4.5.303"
          }
          []
          nil
          {}
          audit-logger
          nil
        )
        :id
      )
      app-id-2 (->
        (store/add-application
          {
            :lang    "fi"
            :form    form-id
            :answers []
            :person-oid "2.2.2"
          }
          []
          form
          {}
          audit-logger
          nil
        )
        :id
      )
      app-id-3 (->
        (store/add-application
          {
            :lang    "fi"
            :form    form-id
            :answers []
            :person-oid nil
          }
          []
          form
          {}
          audit-logger
          nil
        )
        :id
      )
      app-key-1 (find-application-key-by-id app-id-1)
      app-key-2 (find-application-key-by-id app-id-2)
      app-key-3 (find-application-key-by-id app-id-3)
    ] (binding [
        *tutu-application-key-1*  app-key-1
        *tutu-application-key-2*  app-key-2
        *tutu-application-key-3*  app-key-3
      ] (try
          (context)
          (finally
            (delete-application! app-id-1)
            (delete-application! app-id-2)
            (delete-application! app-id-3)
            (delete-form! form-id)
          )
        )
      )
    )
  )

  (describe "get-tutu-application"

    (it "returns an entry when application with person-oid is found"
      (should= *tutu-application-key-1* (-> (store/get-tutu-application *tutu-application-key-1*) :key))
    )

    (it "returns nil when application is not found"
      (should-be-nil (store/get-tutu-application "not-found-key"))
    )

    (it "returns nil when application has person-oid of nil"
      (should-be-nil (store/get-tutu-application *tutu-application-key-3*))
    )
  )

  (describe "get-tutu-applications"

    (it "returns a list of entries matching input oids"
      (should= 2 (count (store/get-tutu-applications [*tutu-application-key-1* *tutu-application-key-2*])))
    )

    (it "omits entries whose person-oid is nil"
      (should= 2 (count (store/get-tutu-applications [*tutu-application-key-1* *tutu-application-key-2* *tutu-application-key-3*])))
    )
  )
)
