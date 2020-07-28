(ns ataru.applications.application-store-db-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.component-data.component :as component]
            [ataru.forms.form-store :as form-store]
            [ataru.log.audit-log :as audit-log]
            [ataru.db.db :as db]
            [ataru.fixtures.application :as fixtures]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.util :as util]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [after around context describe it should= should== tags]]))

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
  (let [form-with-attachment (update form-fixtures/person-info-form :content concat form-fixtures/attachment-test-form)]
    (println "Tyhjennetään hakemuksia tietokannasta.")
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

(def ^:dynamic *painike-application-id*)
(def ^:dynamic *nonselected-painike-application-id*)
(def ^:dynamic *valikko-application-id*)
(def ^:dynamic *lista-application-id*)
(def ^:dynamic *group-painike-application-id*)
(def ^:dynamic *nonselected-group-painike-application-id*)
(def ^:dynamic *group-valikko-application-id*)
(def ^:dynamic *group-lista-application-id*)

(describe "listing applications"
  (tags :unit :database)

  (after
   (reset-database!))

  (it "should find application with attachments query"
    (let [existing-attachment-id "att__1"
          application-id         (store/add-application
                                  (dissoc application :key)
                                  []
                                  form-fixtures/attachment-test-form
                                  {}
                                  audit-logger)
          application-key        (find-application-key-by-id application-id)
          new-reviews            (create-new-reviews application application-key)
          query                  {:attachment-review-states [existing-attachment-id '("not-checked")]}
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
            form-id                                  (-> form
                                             form-store/create-new-form!
                                             :key
                                             form-store/latest-id-by-key)
            painike-application-id                   (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "painike"
                                                                     :value     "0"
                                                                     :fieldType "singleChoice"}]}
                                             (store/add-application
                                              []
                                              form
                                              {}
                                              audit-logger))
            nonselected-painike-application-id       (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "painike"
                                                                     :value     nil
                                                                     :fieldType "singleChoice"}]}
                                                         (store/add-application
                                                          []
                                                          form
                                                          {}
                                                          audit-logger))
            valikko-application-id                   (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "valikko"
                                                                     :value     "0"
                                                                     :fieldType "dropdown"}]}
                                             (store/add-application
                                              []
                                              form
                                              {}
                                              audit-logger))
            lista-application-id                     (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "lista"
                                                                     :value     ["0"]
                                                                     :fieldType "multipleChoice"}]}
                                             (store/add-application
                                              []
                                              form
                                              {}
                                              audit-logger))
            group-painike-application-id             (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "group-painike"
                                                                     :value     [["0"]]
                                                                     :fieldType "singleChoice"}]}
                                             (store/add-application
                                              []
                                              form
                                              {}
                                              audit-logger))
            nonselected-group-painike-application-id (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "group-painike"
                                                                     :value     [[nil]]
                                                                     :fieldType "singleChoice"}]}
                                                         (store/add-application
                                                          []
                                                          form
                                                          {}
                                                          audit-logger))
            group-valikko-application-id             (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "group-valikko"
                                                                     :value     [["0"]]
                                                                     :fieldType "dropdown"}]}
                                             (store/add-application
                                              []
                                              form
                                              {}
                                              audit-logger))
            group-lista-application-id               (-> {:lang    "fi"
                                                          :form    form-id
                                                          :answers [{:key       "group-lista"
                                                                     :value     [["0"]]
                                                                     :fieldType "multipleChoice"}]}
                                             (store/add-application
                                              []
                                              form
                                              {}
                                              audit-logger))]
        (binding [*painike-application-id*                   painike-application-id
                  *nonselected-painike-application-id*       nonselected-painike-application-id
                  *valikko-application-id*                   valikko-application-id
                  *lista-application-id*                     lista-application-id
                  *group-painike-application-id*             group-painike-application-id
                  *nonselected-group-painike-application-id* nonselected-group-painike-application-id
                  *group-valikko-application-id*             group-valikko-application-id
                  *group-lista-application-id*               group-lista-application-id]
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
              (delete-form! form-id))))))

    (it "should find application with Painike, yksi valittavissa"
      (should== [*painike-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"painike" ["0"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with nonselected Painike, yksi valittavissa"
      (should== [*nonselected-painike-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"painike" []}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with Valikko"
      (should== [*valikko-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"valikko" ["0"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with Lista, monta valittavissa"
      (should== [*lista-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"lista" ["0"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Painike, yksi valittavissa with nonselected answer"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"painike" ["1"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Valikko with nonselected answer"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"valikko" ["1"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Lista, monta valittavissa with nonselected answer"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"lista" ["1"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with Painike, yksi valittavissa in a question group"
      (should== [*group-painike-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"group-painike" ["0"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with nonselected Painike, yksi valittavissa in a question group"
      (should== [*nonselected-group-painike-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"group-painike" []}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with Valikko in a question group"
      (should== [*group-valikko-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"group-valikko" ["0"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should find application with Lista, monta valittavissa in a question group"
      (should== [*group-lista-application-id*]
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"group-lista" ["0"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Painike, yksi valittavissa with nonselected answer in a question group"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"group-painike" ["1"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Valikko with nonselected answer in a question group"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"group-valikko" ["1"]}}
                           {:order-by "applicant-name" :order "asc"}))))

    (it "should not find application with Lista, monta valittavissa with nonselected answer in a question group"
      (should== []
                (mapv :id (store/get-application-heading-list
                           {:option-answers {"group-lista" ["1"]}}
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
