(ns ataru.browser-tests
  (:require [clojure.string :refer [split join]]
            [clojure.java.shell :refer [sh]]
            [environ.core :refer [env]]
            [speclj.core :refer :all]
            [ataru.config.core :refer [config]]
            [com.stuartsierra.component :as component]
            [ataru.test-utils :as utils]
            [ataru.virkailija.virkailija-system :as virkailija-system]
            [ataru.hakija.hakija-system :as hakija-system]
            [ataru.forms.form-store :as form-store]
            [ataru.hakija.application-email-confirmation :as application-email])
  (:import (java.util.concurrent TimeUnit)))

(defn- run-specs-in-virkailija-system
  [specs clear-db?]
  (let [system (atom (virkailija-system/new-system))]
    (try
      (when clear-db?
        (ataru.fixtures.db.browser-test-db/reset-test-db true))
      (reset! system (component/start-system @system))
      (specs)
      (finally
        (component/stop-system @system)))))

(defn- run-specs-in-hakija-system
  [specs]
  (with-redefs [application-email/start-email-submit-confirmation-job (fn [_])
                application-email/start-email-edit-confirmation-job   (fn [_])]
    (let [system (atom (hakija-system/new-system))]
      (try
        (reset! system (component/start-system @system))
        (specs)
        (finally
          (component/stop-system @system))))))

(defn- sh-timeout
  [timeout-secs & args]
  (println "run" timeout-secs args)
  (.get
   (future-call #(apply sh args))
   timeout-secs
   (TimeUnit/SECONDS)))

(defn run-phantom-test
  [test-name & args]
  (let [results (apply sh-timeout
                       120
                       "node_modules/phantomjs-prebuilt/bin/phantomjs"
                       "--web-security" "false"
                       "bin/phantomjs-runner.js" test-name args)]
    (println (:out results))
    (.println System/err (:err results))
    (should= 0 (:exit results))))

(defn- get-latest-form
  [form-name]
  (->> (form-store/get-all-forms)
       (filter #(= (:name %) form-name))
       (first)))


(describe "Virkailija UI tests /"
          (tags :ui :ui-virkailija)
          (around-all [specs]
                      (run-specs-in-virkailija-system specs true))
          (it "are successful"
              (run-phantom-test "virkailija" (last (split (utils/login) #"="))))
          (it "creates a form with question groups"
            (run-phantom-test "virkailija-question-group" (last (split (utils/login) #"=")))))

(describe "Hakija UI tests /"
          (tags :ui :ui-hakija)
          (around-all [specs]
                      (run-specs-in-hakija-system specs))

          (it "can fill a form successfully"
              (run-phantom-test "hakija-form"))

          (it "can fill a form for haku with single hakukohde successfully"
              (run-phantom-test "hakija-haku"))

          (it "can fill a form for hakukohde successfully"
              (run-phantom-test "hakija-hakukohde"))

          (it "can fill a form successfully with non-finnish ssn"
              (run-phantom-test "hakija-ssn"))

          (it "can edit an application successfully"
              (run-phantom-test "hakija-edit"))

          (it "can edit an application successfully as virkailija"
              (run-phantom-test "virkailija-hakemus-edit"))

          (it "can fill a form with a question group successfully"
              (run-phantom-test "hakija-question-group-form")))

(run-specs)
