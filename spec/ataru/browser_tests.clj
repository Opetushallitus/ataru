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

(defn- run-specs-with-virkailija-and-hakija-systems [specs]
  (with-redefs [application-email/start-email-submit-confirmation-job (fn [_ _])
                application-email/start-email-edit-confirmation-job   (fn [_ _])]
    (let [virkailija-system (atom (virkailija-system/new-system))
          hakija-system     (atom (hakija-system/new-system))]
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

(describe "Ataru UI tests /"
  (tags :ui)
  (around-all [specs]
    (run-specs-with-virkailija-and-hakija-systems specs))

  (describe "form creation /"
    (it "is created successfully"
      (run-phantom-test "virkailija" (last (split (utils/login) #"="))))
    (it "is created with a question group successfully"
      (run-phantom-test "virkailija-question-group" (last (split (utils/login) #"=")))))

  (describe "applying using a form /"
    (it "is possible to apply using a plain form"
      (run-phantom-test "hakija-form"))
    (it "is possible to apply using a form for haku with single hakukohde"
      (run-phantom-test "hakija-haku"))
    (it "is possible to apply using a form for hakukohde"
      (run-phantom-test "hakija-hakukohde"))
    (it "is possible to apply using a form successfully with non-finnish ssn"
      (run-phantom-test "hakija-ssn"))
    (it "is possible to apply using a form with a question group"
      (run-phantom-test "hakija-question-group-form")))

  (describe "editing a submitted application /"
    (it "is possible to edit a plain application successfully"
      (run-phantom-test "hakija-edit"))
    (it "is possbile to edit an application successfully as virkailija"
      (run-phantom-test "virkailija-hakemus-edit")))

  (describe "application handling /"
    (it "is possbile to handle application with a question group"
      (run-phantom-test "virkailija-question-group-application-handling" (last (split (utils/login) #"="))))))

(run-specs)
