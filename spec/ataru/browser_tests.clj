(ns ataru.browser-tests
  (:require [clojure.string :refer [split join]]
            [clojure.java.shell :refer [sh]]
            [environ.core :refer [env]]
            [speclj.core :refer :all]
            [oph.soresu.common.db :as db]
            [oph.soresu.common.config :refer [config]]
            [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.test-utils :refer [login]]
            [ataru.virkailija.virkailija-system :as virkailija-system]
            [ataru.hakija.hakija-system :as hakija-system]
            [ataru.fixtures.db.browser-test-db :refer [init-db-fixture]])
  (:import (java.util.concurrent TimeUnit)))

(defn- run-specs-in-virkailija-system
  [specs]
  (let [system (virkailija-system/new-system)]
    (try
      (migrations/migrate)
      (init-db-fixture)
      (component/start-system system)
      (println "* go")
      (specs)
      (println "* done")
      (finally
        (component/stop-system system)))))

(defn- run-specs-in-hakija-system
  [specs]
  (let [system (hakija-system/new-system)]
    (try
      (migrations/migrate)
      (component/start-system system)
      (specs)
      (finally
        (component/stop-system system)))))

(defn sh-timeout [timeout-secs & args]
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

(describe "Hakija UI tests /"
          (tags :ui)
          (around-all [specs]
                      (run-specs-in-hakija-system specs))
          (it "are successful"
              (let [results (sh-timeout 120 "node_modules/phantomjs-prebuilt/bin/phantomjs"
                                        "--web-security" "false"
                                        "bin/phantomjs-runner.js" "hakija")]
                (println (:out results))
                (.println System/err (:err results))
                (should= 0 (:exit results)))))


(run-specs)
