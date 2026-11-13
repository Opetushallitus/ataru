(ns ataru.applications.application-feedback-store-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.db.db :as db]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.fixtures.form :as form-fixtures]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [after-all before-all describe it should-be-nil should= tags]]))

(def ^:private feedback-test-form
  (merge (form-fixtures/create-form)
         {:organization-oid "1.2.246.562.10.0439845"
          :id               2147483600}))

(def ^:private form (atom nil))

(defn- delete-feedback-of-form! [form-key]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (jdbc/delete! conn :application_feedback ["form_key = ?" form-key])))

(defn- feedback-for-form [{:keys [key id]}]
  {:form-key   key
   :form-id    id
   :form-name  "Testilomake"
   :user-agent "Mozilla/5.0 (test)"
   :rating     4
   :haku-oid   nil
   :feedback   "Hyvä lomake"})

(describe "application feedback store"
  (tags :unit :application-feedback)

  (before-all
    ;; init-db-fixture palauttaa lomakkeen sen alkuperäisellä juoksevalla id:llä,
    ;; vaikka kantaan tallennettu id vaihdetaan fixturen id:ksi
    (reset! form (assoc (unit-test-db/init-db-fixture feedback-test-form)
                        :id (:id feedback-test-form)))
    (delete-feedback-of-form! (:key @form)))

  (after-all
    (delete-feedback-of-form! (:key @form)))

  (it "stores feedback given through a haku with the haku oid"
    (store/add-application-feedback (assoc (feedback-for-form @form)
                                           :haku-oid "1.2.246.562.29.00000000001"
                                           :feedback "Palaute haun kautta"))
    (let [[feedback] (filter #(= "Palaute haun kautta" (:feedback %))
                             (store/get-application-feedback-by-form-key (:key @form)))]
      (should= "1.2.246.562.29.00000000001" (:haku-oid feedback))
      (should= 4 (:stars feedback))
      (should= "Mozilla/5.0 (test)" (:user-agent feedback))
      (should= (:key @form) (:form-key feedback))))

  (it "stores feedback given on a standalone form without a haku oid"
    (store/add-application-feedback (assoc (feedback-for-form @form)
                                           :feedback "Palaute ilman hakua"))
    (let [[feedback] (filter #(= "Palaute ilman hakua" (:feedback %))
                             (store/get-application-feedback-by-form-key (:key @form)))]
      (should-be-nil (:haku-oid feedback))))

  (it "stores feedback even when the haku oid key is missing from the payload"
    (store/add-application-feedback (-> (feedback-for-form @form)
                                        (dissoc :haku-oid)
                                        (assoc :feedback "Palaute ilman haku-oid-avainta")))
    (let [[feedback] (filter #(= "Palaute ilman haku-oid-avainta" (:feedback %))
                             (store/get-application-feedback-by-form-key (:key @form)))]
      (should-be-nil (:haku-oid feedback))))

  (it "returns newest feedback first"
    (delete-feedback-of-form! (:key @form))
    (doseq [text ["Ensimmäinen" "Toinen" "Kolmas"]]
      (store/add-application-feedback (assoc (feedback-for-form @form) :feedback text)))
    (should= ["Kolmas" "Toinen" "Ensimmäinen"]
             (map :feedback (store/get-application-feedback-by-form-key (:key @form))))))
