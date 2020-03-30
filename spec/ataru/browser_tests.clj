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
            [ataru.email.application-email-confirmation :as application-email]
            [ataru.log.audit-log :as audit-log])
  (:import (java.util.concurrent TimeUnit)))

(defn- run-specs-with-virkailija-and-hakija-systems [specs]
  (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)
                application-email/start-email-edit-confirmation-job   (constantly nil)]
    (let [dummy-audit-logger (audit-log/new-dummy-audit-logger)
          virkailija-system  (atom (virkailija-system/new-system dummy-audit-logger))
          hakija-system      (atom (hakija-system/new-system dummy-audit-logger))]
      (try
        (ataru.fixtures.db.browser-test-db/reset-test-db true)
        (reset! virkailija-system (component/start-system @virkailija-system))
        (reset! hakija-system (component/start-system @hakija-system))
        (specs)
        (finally
          (component/stop-system @virkailija-system)
          (component/stop-system @hakija-system))))))

(defn- sh-timeout
  [timeout-secs & args]
  (println "run" timeout-secs args)
  (.get
   (future-call #(apply sh args))
   timeout-secs
   (TimeUnit/SECONDS)))

(defn run-karma-test
  [test-name & args]
  (let [results (apply sh-timeout 120 "node" "bin/karma-runner.js" test-name args)]
    (println (:out results))
    (.println System/err (:err results))
    (should= 0 (:exit results))))

(describe "Ataru UI tests /"
  (tags :ui)
  (around-all [specs]
    (run-specs-with-virkailija-and-hakija-systems specs))

  (describe "form creation /"
    (it "is created successfully"
      (run-karma-test "virkailija" (last (split (utils/login) #"="))))
    (it "is created with a question group successfully"
      (run-karma-test "virkailija-question-group" (last (split (utils/login) #"="))))
    (it "is created with a selection limit successfully"
        (run-karma-test "virkailija-selection-limit" (last (split (utils/login) #"="))))
    (it "is able to use lomake with hakukohde organization connection"
        (run-karma-test "virkailija-with-hakukohde-organization" (last (split (utils/login) #"=")))))

  (describe "applying using a form /"
    (it "is possible to apply using a plain form"
      (run-karma-test "hakija-form"))
    (it "is possible to apply using a form for haku with single hakukohde"
      (run-karma-test "hakija-haku"))
    (it "is possible to apply using a form for hakukohde"
      (run-karma-test "hakija-hakukohde"))
    (it "is possible to apply using a form successfully with non-finnish ssn"
      (run-karma-test "hakija-ssn"))
    (it "is possible to apply using a form with a question group"
      (run-karma-test "hakija-question-group-form"))
    (it "is possible to apply with selection limit"
        (run-karma-test "hakija-selection-limit"))
    (it "is possible to apply as virkailija"
      (run-karma-test "virkailija-haku")))

  (describe "editing a submitted application /"
    (it "is possible to edit a plain application successfully"
      (run-karma-test "hakija-edit"))
    (it "is possible to edit an application successfully as virkailija"
      (run-karma-test "virkailija-hakemus-edit"))
    (it "is taking hakuaika into account"
      (run-karma-test "hakija-hakukohteen-hakuaika")))

  (describe "application handling /"
    (it "is possible to handle application with a question group"
      (run-karma-test "virkailija-question-group-application-handling" (last (split (utils/login) #"="))))))

(run-specs)
