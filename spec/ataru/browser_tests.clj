(ns ataru.browser-tests
  (:require [clojure.string :refer [split join]]
            [clojure.java.shell :refer [sh]]
            [environ.core :refer [env]]
            [speclj.core :refer :all]
            [ataru.db.db :as db]
            [ataru.config.core :refer [config]]
            [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.test-utils :as utils]
            [ataru.virkailija.virkailija-system :as virkailija-system]
            [ataru.hakija.hakija-system :as hakija-system]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store])
  (:import (java.util.concurrent TimeUnit)))

(defn- run-specs-in-virkailija-system
  [specs]
  (let [system (atom (virkailija-system/new-system))]
    (try
      ;; Reset here in order to keep login cookie from fake login
      (ataru.fixtures.db.browser-test-db/reset-test-db true)
      (reset! system (component/start-system @system))
      (specs)
      (finally
        (component/stop-system @system)))))

(defn- run-specs-in-hakija-system
  [specs]
  (let [system (atom (hakija-system/new-system))]
    (try
      (reset! system (component/start-system @system))
      (specs)
      (finally
        (component/stop-system @system)))))

(defn- sh-timeout
  [timeout-secs & args]
  (println "run" timeout-secs args)
  (.get
   (future-call #(apply sh args))
   timeout-secs
   (TimeUnit/SECONDS)))

(describe "Virkailija UI tests /"
          (tags :ui :ui-virkailija)
          (around-all [specs]
                      (run-specs-in-virkailija-system specs))
          (it "are successful"
              (let [login-cookie-value (last (split (utils/login) #"="))
                    results            (sh-timeout
                                        120
                                        "node_modules/phantomjs-prebuilt/bin/phantomjs"
                                        "--web-security" "false"
                                        "bin/phantomjs-runner.js" "virkailija" login-cookie-value)]
                (println (:out results))
                (.println System/err (:err results))
                (should= 0 (:exit results)))))

(describe "Hakija UI tests /"
          (tags :ui :ui-hakija)
          (around-all [specs]
                      (run-specs-in-hakija-system specs))

          (it "can fill a form successfully"
              (let [results (sh-timeout
                             120
                             "node_modules/phantomjs-prebuilt/bin/phantomjs"
                             "--web-security" "false"
                             "bin/phantomjs-runner.js" "hakija-form")]
                (println (:out results))
                (.println System/err (:err results))
                (should= 0 (:exit results))))

          (it "can fill a form for haku with single hakukohde successfully"
              (let [results (sh-timeout
                             120
                             "node_modules/phantomjs-prebuilt/bin/phantomjs"
                             "--web-security" "false"
                             "bin/phantomjs-runner.js" "hakija-haku")]
                (println (:out results))
                (.println System/err (:err results))
                (should= 0 (:exit results))))

          (it "can fill a form for hakukohde successfully"
              (let [results (sh-timeout
                             120
                             "node_modules/phantomjs-prebuilt/bin/phantomjs"
                             "--web-security" "false"
                             "bin/phantomjs-runner.js" "hakija-hakukohde")]
                (println (:out results))
                (.println System/err (:err results))
                (should= 0 (:exit results))))

          (it "can fill a form successfully with non-finnish ssn"
              (let [results (sh-timeout
                             120
                             "node_modules/phantomjs-prebuilt/bin/phantomjs"
                             "--web-security" "false"
                             "bin/phantomjs-runner.js" "hakija-ssn")]
                (println (:out results))
                (.println System/err (:err results))
                (should= 0 (:exit results))))

          (it "can edit an application successfully"
              (let [results (sh-timeout
                             120
                             "node_modules/phantomjs-prebuilt/bin/phantomjs"
                             "--web-security" "false"
                             "bin/phantomjs-runner.js" "hakija-edit")]
                (println (:out results))
                (.println System/err (:err results))
                (should= 0 (:exit results)))))

(run-specs)
