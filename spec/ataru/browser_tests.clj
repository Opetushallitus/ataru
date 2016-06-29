(ns ataru.browser-tests
  (:require [clojure.string :refer [split join]]
            [clojure.java.shell :refer [sh]]
            [environ.core :refer [env]]
            [speclj.core :refer :all]
            [oph.soresu.common.db :as db]
            [oph.soresu.common.config :refer [config]]
            [ataru.test-utils :refer [login]]
            [com.stuartsierra.component :as component]
            [ataru.virkailija.virkailija-system :as virkailija-system]))

(defn- run-specs-in-system
  [specs]
  (let [system (virkailija-system/new-system)]
    (try
      (component/start-system system)
      (with-redefs [ataru.virkailija.authentication.auth/logged-in? (constantly true)]
        (specs))
      (finally
        (component/stop-system system)))))

(describe "UI tests /"
          (tags :ui)
          (around-all [specs]
                      (db/clear-db! :db (-> config :db :schema))
                      (run-specs-in-system specs))
          (it "browser tests"
              (let [results (sh "node_modules/phantomjs-prebuilt/bin/phantomjs"
                                "--web-security" "false"
                                "bin/phantomjs-runner.js" "fakecookie")
                    output (:out results)
                    test-report-path "target/tests-output.txt"]
                (println "Tests exit code" (:exit results))
                (spit test-report-path output)
                (println output)
                (.println System/err (:err results))
                (should= 0 (:exit results)))))

(run-specs)