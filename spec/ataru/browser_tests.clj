(ns ataru.browser-tests
  (:require [clojure.string :refer [split join]]
            [clojure.java.shell :refer [sh]]
            [environ.core :refer [env]]
            [speclj.core :refer :all]
            [ataru.db.db :as db]
            [ataru.config.core :refer [config]]
            [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.test-utils :refer [login]]
            [ataru.virkailija.virkailija-system :as virkailija-system]
            [ataru.hakija.hakija-system :as hakija-system]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [ataru.fixtures.db.browser-test-db :refer [init-db-fixture]])
  (:import (java.util.concurrent TimeUnit)))

(defn- run-specs-in-virkailija-system
  [specs]
  (let [system (atom (virkailija-system/new-system))]
    (try
      (migrations/migrate)
      (init-db-fixture)
      (reset! system (component/start-system @system))
      (specs)
      (finally
        (component/stop-system @system)))))

(defn- run-specs-in-hakija-system
  [specs]
  (let [system (atom (hakija-system/new-system))]
    (try
      (migrations/migrate)
      (reset! system (component/start-system @system))
      (specs)
      (finally
        (component/stop-system @system)))))

(defn sh-timeout
  [timeout-secs & args]
  (println "run" timeout-secs args)
  (.get
    (future-call #(apply sh args))
    timeout-secs
    (TimeUnit/SECONDS)))

(describe "Virkailija UI tests /"
          (tags :ui)
          (around-all [specs]
                      (db/clear-db! :db (-> config :db :schema))
                      (run-specs-in-virkailija-system specs))
          (it "are successful"
              (let [login-cookie-value (last (split (login) #"="))
                    results (sh-timeout 120 "node_modules/phantomjs-prebuilt/bin/phantomjs"
                                "--web-security" "false"
                                "bin/phantomjs-runner.js" "virkailija" login-cookie-value)]
                (println (:out results))
                (.println System/err (:err results))
                (should= 0 (:exit results)))))

(defn- get-latest-form
  []
  (first (form-store/get-all-forms)))

(describe "Hakija UI tests /"
          (tags :ui)
          (around-all [specs]
                      (run-specs-in-hakija-system specs))
          (it "can fill a form successfully"
              (let [results (sh-timeout
                              120
                              "node_modules/phantomjs-prebuilt/bin/phantomjs"
                              "--web-security" "false"
                              "bin/phantomjs-runner.js" "hakija" (:key (get-latest-form)))]
                (println (:out results))
                (.println System/err (:err results))
                (should= 0 (:exit results))))
          (it "can edit an application successfully"
              (let [latest-application (first (application-store/get-application-list-by-form (:key (get-latest-form))))
                    secret (-> latest-application
                               :id
                               (application-store/get-application)
                               :secret)
                    results            (sh-timeout
                                         120
                                         "node_modules/phantomjs-prebuilt/bin/phantomjs"
                                         "--web-security" "false"
                                         "bin/phantomjs-runner.js" "hakija-edit" secret)]
                (println (:out results))
                (.println System/err (:err results))
                (should= 0 (:exit results)))))

(run-specs)
