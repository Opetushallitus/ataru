(ns ataru.siirtotiedosto-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [clojure.java.io :refer [input-stream]]
            [ataru.config.core :refer [config]])
  (:import (fi.vm.sade.valinta.dokumenttipalvelu SiirtotiedostoPalvelu)))

(defprotocol SiirtotiedostoService
  (siirtotiedosto-applications [this params])
  (siirtotiedosto-forms [this params]))

(def applications-page-size (or (try (Integer/parseInt (-> config :siirtotiedostot :applications-page-size)) (catch Exception _ nil)) 10000))
(def forms-page-size (or (try (Integer/parseInt (-> config :siirtotiedostot :forms-page-size)) (catch Exception _ nil)) 500))

(defn- save-applications-to-s3 [^SiirtotiedostoPalvelu client applications execution-id file-count additional-info]
  (let [json (json/generate-string applications)
        stream (input-stream (.getBytes json))]
    (log/info "Saving" (count applications) "applications as json to s3 in siirtotiedosto! Execution id " execution-id)
    (try (.saveSiirtotiedosto client "ataru" "hakemus" additional-info execution-id file-count stream 2)
         true
         (catch Exception e
           (log/error (str "Ei onnistuttu tallentamaan hakemuksia:" e))
           false))))

(defn- save-forms-to-s3 [^SiirtotiedostoPalvelu client forms execution-id file-count additional-info]
  (let [json (json/generate-string forms)
        stream (input-stream (.getBytes json))]
    (log/info "Saving" (count forms) "forms as json to s3 in siirtotiedosto! Execution id " execution-id)
    (try (.saveSiirtotiedosto client "ataru" "lomake" additional-info execution-id file-count stream 2)
         true
         (catch Exception e
           (log/error (str "Ei onnistuttu tallentamaan lomakkeita:" (.getMessage e)))
           false))))

(defrecord CommonSiirtotiedostoService [siirtotiedosto-client]
  SiirtotiedostoService
  (siirtotiedosto-applications
    [_ params]
    (let [done (atom 0)
          changed-ids (->> (application-store/siirtotiedosto-application-ids params)
                           (map :id))
          partitions (partition applications-page-size applications-page-size nil changed-ids)]
      (log/info "Changed application ids in total: " (count changed-ids) ", partition size " applications-page-size ", partitions:" (count partitions))
      (let [chunk-results (doall (for [application-ids partitions]
                                          (let [start (System/currentTimeMillis)
                                                applications-chunk (application-store/siirtotiedosto-applications-for-ids application-ids)
                                                {:keys [execution-id haku-oid]} params
                                                success? (save-applications-to-s3 siirtotiedosto-client applications-chunk execution-id (inc @done) (or haku-oid ""))]
                                            (log/info "Applications-chunk" (str (swap! done inc) "/" (count partitions)) "complete, took" (- (System/currentTimeMillis) start) ", success " success?)
                                            success?)))]
        (log/info "application-chunk results" chunk-results)
        {:success (every? boolean chunk-results)
         :modified-before (:modified-before params)})))

  (siirtotiedosto-forms
    [_ params]
    (let [done (atom 0)
          changed-ids (->> (form-store/siirtotiedosto-form-ids params)
                           (map :id))
          partitions (partition forms-page-size forms-page-size nil changed-ids)]
      (log/info "Changed form ids in total: " (count changed-ids) ", partition size " forms-page-size ", partitions:" (count partitions))
      (let [chunk-results (doall (for [form-ids partitions]
                                    (let [start (System/currentTimeMillis)
                                          forms-chunk (form-store/fetch-forms-by-ids form-ids)
                                          {:keys [execution-id haku-oid]} params
                                          success? (save-forms-to-s3 siirtotiedosto-client forms-chunk execution-id (inc @done) (or haku-oid ""))]
                                      (log/info "Forms-chunk" (str (swap! done inc) "/" (count partitions)) "complete, took" (- (System/currentTimeMillis) start) ", success " success?)
                                      success?)))]
        (log/info "form-chunk results" chunk-results)
        {:success (every? boolean chunk-results)
         :modified-before (:modified-before params)})))

  )

(defn new-siirtotiedosto-service [] (->CommonSiirtotiedostoService nil))
