(ns ataru.applications.automatic-eligibility-spec
  (:require [ataru.applications.automatic-eligibility :as ae]
            [ataru.db.db :as db]
            [ataru.dob :as dob]
            [ataru.log.audit-log :as audit-log]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clojure.java.jdbc :as jdbc]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [speclj.core :refer :all]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/form-queries.sql")
(defqueries "sql/application-queries.sql")

(defn- ->ohjausparametrit-gen
  [haku]
  (if (nil? haku)
    (gen/return nil)
    (gen/one-of [(gen/return {})
                 (gen/return {:PH_AHP nil})
                 (gen/return {:PH_AHP {:date nil}})
                 (gen/fmap #(hash-map :PH_AHP (hash-map :date %))
                           (gen/choose 1 3))])))

(def input-gen
  (gen/let [haku (gen/one-of [(gen/return nil)
                              (gen/hash-map
                               :oid (gen/return "haku-oid")
                               :ylioppilastutkinto-antaa-hakukelpoisuuden? gen/boolean)])
            hakukohde-oids (if (nil? haku)
                             (gen/return [])
                             (gen/fmap (fn [i]
                                         (map #(str "hakukohde-" % "-oid")
                                              (range i)))
                                       (gen/choose 1 6)))
            yah?           (gen/vector gen/boolean (count hakukohde-oids))]
    (gen/hash-map
     :application      (gen/hash-map :key (gen/return "application-key")
                                     :person-oid (gen/return "person-oid")
                                     :haku-oid (gen/return (:oid haku))
                                     :hakukohde-oids (gen/return hakukohde-oids))
     :haku             (gen/return haku)
     :ohjausparametrit (->ohjausparametrit-gen haku)
     :now              (gen/fmap coerce/from-long (gen/choose 1 3))
     :hakukohteet      (gen/return (map #(hash-map :oid %1
                                                   :ylioppilastutkinto-antaa-hakukelpoisuuden? %2)
                                        hakukohde-oids
                                        yah?))
     :suoritus?        gen/boolean)))

(def audit-logger (audit-log/new-dummy-audit-logger))

(defn- call-ae
  [inputs]
  (ae/automatic-eligibility-if-ylioppilas
   (:application inputs)
   (:haku inputs)
   (:ohjausparametrit inputs)
   (:now inputs)
   (:hakukohteet inputs)
   (:suoritus? inputs)))

(defn- check [times prop]
  (let [result (tc/quick-check times prop)]
    (when-not (:result result)
      (let [input (-> result :shrunk :smallest first)]
        (-fail (str (with-out-str (clojure.pprint/pprint (list 'call-ae input))) "\n"
                    (with-out-str (clojure.pprint/pprint (call-ae input)))))))))

(describe "Automatic eligibility"
  (tags :unit)

  (it "returns no updates when application has no haku-oid"
    (check 100 (prop/for-all [inputs input-gen]
                 (if (nil? (:haku inputs))
                   (empty? (call-ae inputs))
                   true))))

  (it "returns no updates when automatic eligibility if ylioppilas not in use"
    (check 100 (prop/for-all [inputs input-gen]
                 (if (false? (get-in inputs [:haku :ylioppilastutkinto-antaa-hakukelpoisuuden?]))
                   (empty? (call-ae inputs))
                   true))))

  (it "returns updates only for hakukohteet where automatic eligibility if ylioppilas is in use"
    (check 100 (prop/for-all [inputs input-gen]
                 (every? #(:ylioppilastutkinto-antaa-hakukelpoisuuden? (:hakukohde %))
                         (call-ae inputs)))))

  (it "returns no updates when now is pass PH_AHP"
    (check 100 (prop/for-all [inputs input-gen]
                 (if (some->> (get-in inputs [:ohjausparametrit :PH_AHP :date])
                              coerce/from-long
                              (time/after? (:now inputs)))
                   (empty? (call-ae inputs))
                   true))))

  (it "returns updates from unreviewed to eligible when ylioppilas or ammatillinen"
    (check 100 (prop/for-all [inputs input-gen]
                 (if (:suoritus? inputs)
                   (every? #(and (= "unreviewed" (:from %))
                                 (= "eligible" (:to %)))
                           (call-ae inputs))
                   true))))

  (it "returns updates from eligible to unreviewed when not ylioppilas or ammatillinen"
    (check 100 (prop/for-all [inputs input-gen]
                 (if (not (:suoritus? inputs))
                   (every? #(and (= "eligible" (:from %))
                                 (= "unreviewed" (:to %)))
                           (call-ae inputs))
                   true))))

  (it "returns application as given in updates"
    (check 100 (prop/for-all [inputs input-gen]
                 (every? #(= (:application inputs) (:application %))
                         (call-ae inputs))))))

(def ^:dynamic *form-id*)
(def ^:dynamic *application-id*)
(def ^:dynamic *application-key*)

(defn- get-reviews []
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (jdbc/query connection
                ["SELECT state, requirement
                  FROM application_hakukohde_reviews
                  WHERE application_key = ? AND
                        hakukohde = ?"
                 *application-key*
                 "1.2.246.562.20.00000000001"])))

(defn- get-events []
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (jdbc/query connection
                ["SELECT new_review_state, event_type, review_key
                  FROM application_events
                  WHERE application_key = ? AND
                        hakukohde = ?"
                 *application-key*
                 "1.2.246.562.20.00000000001"])))

(describe "Update application hakukohde review based on automatic eligibility"
  (tags :unit)

  (around [it]
    (let [form-id          (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                             (:id (yesql-add-form<! {:name                 {:fi "Lomake"}
                                                     :content              {:content []}
                                                     :created_by           "testi"
                                                     :key                  "d3fb73dd-b097-42b9-bf35-a873735440e2"
                                                     :languages            {:languages ["fi"]}
                                                     :organization_oid     "1.2.246.562.10.00000000001"
                                                     :deleted              false
                                                     :locked               nil
                                                     :locked_by            nil
                                                     :used_hakukohderyhmas []}
                                                    {:connection connection})))
          {:keys [id key]} (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                             (yesql-add-application<! {:form_id        form-id
                                                       :content        {:answers []}
                                                       :lang           "fi"
                                                       :preferred_name "Testi"
                                                       :last_name      "Testi"
                                                       :hakukohde      ["1.2.246.562.20.00000000001"]
                                                       :haku           "1.2.246.562.29.00000000001"
                                                       :person_oid     "1.2.246.562.24.00000000001"
                                                       :ssn            nil
                                                       :dob            (dob/str->dob "24.09.1989")
                                                       :email          "test@example.com"}
                                                      {:connection connection}))]
      (binding [*form-id*         form-id
                *application-id*  id
                *application-key* key]
        (try
          (it)
          (finally
            (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
              (jdbc/execute! connection
                             ["DELETE FROM application_events
                               WHERE application_key = ?"
                              key])
              (jdbc/execute! connection
                             ["DELETE FROM application_hakukohde_reviews
                               WHERE application_key = ?"
                              key])
              (jdbc/execute! connection
                             ["DELETE FROM applications
                               WHERE id = ?"
                              id])
              (jdbc/execute! connection
                             ["DELETE FROM forms
                               WHERE id = ?"
                              form-id])))))))

  (it "should set eligible if no state before"
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (ae/update-application-hakukohde-review
       connection
       audit-logger
       {:from        "unreviewed"
        :to          "eligible"
        :application {:key *application-key*}
        :hakukohde   {:oid "1.2.246.562.20.00000000001"}}))
    (let [r (get-reviews)
          e (get-events)]
      (should= 1 (count r))
      (should-contain {:state       "eligible"
                       :requirement "eligibility-state"}
                      r)
      (should= 1 (count e))
      (should-contain {:new_review_state "eligible"
                       :event_type       "eligibility-state-automatically-changed"
                       :review_key       "eligibility-state"}
                      e)))

  (it "should set eligible if unreviewed before"
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (jdbc/execute! connection
                     ["INSERT INTO application_hakukohde_reviews
                       (application_key, requirement, state, hakukohde)
                       VALUES
                       (?, 'eligibility-state', 'unreviewed', ?)"
                      *application-key*
                      "1.2.246.562.20.00000000001"]))
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (ae/update-application-hakukohde-review
       connection
       audit-logger
       {:from        "unreviewed"
        :to          "eligible"
        :application {:key *application-key*}
        :hakukohde   {:oid "1.2.246.562.20.00000000001"}}))
    (let [r (get-reviews)
          e (get-events)]
      (should= 1 (count r))
      (should-contain {:state       "eligible"
                       :requirement "eligibility-state"}
                      r)
      (should= 1 (count e))
      (should-contain {:new_review_state "eligible"
                       :event_type       "eligibility-state-automatically-changed"
                       :review_key       "eligibility-state"}
                      e)))

  (it "should set unreviewed if automatically set eligible before"
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (ae/update-application-hakukohde-review
       connection
       audit-logger
       {:from        "unreviewed"
        :to          "eligible"
        :application {:key *application-key*}
        :hakukohde   {:oid "1.2.246.562.20.00000000001"}}))
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (ae/update-application-hakukohde-review
       connection
       audit-logger
       {:from        "eligible"
        :to          "unreviewed"
        :application {:key *application-key*}
        :hakukohde   {:oid "1.2.246.562.20.00000000001"}}))
    (let [r (get-reviews)
          e (get-events)]
      (should= 1 (count r))
      (should-contain {:state       "unreviewed"
                       :requirement "eligibility-state"}
                      r)
      (should= 2 (count e))
      (should-contain {:new_review_state "eligible"
                       :event_type       "eligibility-state-automatically-changed"
                       :review_key       "eligibility-state"}
                      e)
      (should-contain {:new_review_state "unreviewed"
                       :event_type       "eligibility-state-automatically-changed"
                       :review_key       "eligibility-state"}
                      e)))

  (it "should not set eligible if not unreviewed before"
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (jdbc/execute! connection
                     ["INSERT INTO application_hakukohde_reviews
                       (application_key, requirement, state, hakukohde)
                       VALUES
                       (?, 'eligibility-state', 'uneligible', ?)"
                      *application-key*
                      "1.2.246.562.20.00000000001"]))
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (ae/update-application-hakukohde-review
       connection
       audit-logger
       {:from        "unreviewed"
        :to          "eligible"
        :application {:key *application-key*}
        :hakukohde   {:oid "1.2.246.562.20.00000000001"}}))
    (let [r (get-reviews)
          e (get-events)]
      (should= 1 (count r))
      (should-contain {:state       "uneligible"
                       :requirement "eligibility-state"}
                      r)
      (should= 0 (count e))))

  (it "should not set unreviewed if no state before"
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (ae/update-application-hakukohde-review
       connection
       audit-logger
       {:from        "eligible"
        :to          "unreviewed"
        :application {:key *application-key*}
        :hakukohde   {:oid "1.2.246.562.20.00000000001"}}))
    (let [r (get-reviews)
          e (get-events)]
      (should= 0 (count r))
      (should= 0 (count e))))

  (it "should not set unreviewed if not automatically set eligible before"
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (jdbc/execute! connection
                     ["INSERT INTO application_hakukohde_reviews
                       (application_key, requirement, state, hakukohde)
                       VALUES
                       (?, 'eligibility-state', 'eligible', ?)"
                      *application-key*
                      "1.2.246.562.20.00000000001"]))
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (ae/update-application-hakukohde-review
       connection
       audit-logger
       {:from        "eligible"
        :to          "unreviewed"
        :application {:key *application-key*}
        :hakukohde   {:oid "1.2.246.562.20.00000000001"}}))
    (let [r (get-reviews)
          e (get-events)]
      (should= 1 (count r))
      (should-contain {:state       "eligible"
                       :requirement "eligibility-state"}
                      r)
      (should= 0 (count e))))

  (it "should not set unreviewed if automatically set eligible before and then set by user"
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (ae/update-application-hakukohde-review
       connection
       audit-logger
       {:from        "unreviewed"
        :to          "eligible"
        :application {:key *application-key*}
        :hakukohde   {:oid "1.2.246.562.20.00000000001"}}))
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (jdbc/execute! connection
                     ["UPDATE application_hakukohde_reviews
                       SET state = 'eligible',
                           modified_time = DEFAULT
                       WHERE application_key = ? AND
                             hakukohde = ? AND
                             requirement = 'eligibility-state'"
                      *application-key*
                      "1.2.246.562.20.00000000001"])
      (jdbc/execute! connection
                     ["INSERT INTO application_events
                       (new_review_state, event_type, application_key, hakukohde, review_key)
                       VALUES
                       ('eligible', 'hakukohde-review-state-change', ?, ?, 'eligibility-state')"
                      *application-key*
                      "1.2.246.562.20.00000000001"]))
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (ae/update-application-hakukohde-review
       connection
       audit-logger
       {:from        "eligible"
        :to          "unreviewed"
        :application {:key *application-key*}
        :hakukohde   {:oid "1.2.246.562.20.00000000001"}}))
    (let [r (get-reviews)
          e (get-events)]
      (should= 1 (count r))
      (should-contain {:state       "eligible"
                       :requirement "eligibility-state"}
                      r)
      (should= 2 (count e))
      (should-contain {:new_review_state "eligible"
                       :event_type       "eligibility-state-automatically-changed"
                       :review_key       "eligibility-state"}
                      e)
      (should-contain {:new_review_state "eligible"
                       :event_type       "hakukohde-review-state-change"
                       :review_key       "eligibility-state"}
                      e))))
