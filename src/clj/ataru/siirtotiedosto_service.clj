(ns ataru.siirtotiedosto-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [clojure.java.io :refer [input-stream]]
            [ataru.config.core :refer [config]]
            [ataru.siirtotiedosto.siirtotiedosto-store :as siirtotiedosto-store])
  (:import (fi.vm.sade.valinta.dokumenttipalvelu SiirtotiedostoPalvelu)
           (java.util UUID)))

(defprotocol SiirtotiedostoService
  (siirtotiedosto-applications [this params])
  (siirtotiedosto-forms [this params])
  (siirtotiedosto-everything [this siirtotiedosto-data])
  (create-next-siirtotiedosto [this]))

(def applications-page-size (or (try (Integer/parseInt (-> config :siirtotiedostot :applications-page-size)) (catch Exception _ nil)) 10000))
(def forms-page-size (or (try (Integer/parseInt (-> config :siirtotiedostot :forms-page-size)) (catch Exception _ nil)) 500))
(defn create-new-siirtotiedosto-data [last-siirtotiedosto-data execution-id]
  (log/info "Creating new data, last data" last-siirtotiedosto-data ", exec id" execution-id)
  (if (:success last-siirtotiedosto-data)
    {:window-start (:window-end last-siirtotiedosto-data)
     :execution-uuid execution-id
     :info {}
     :success nil
     :error-message nil}
    (throw (RuntimeException. "Edellistä onnistunutta operaatiota ei löytynyt."))))

(defn update-siirtotiedosto-data [base-data operation-results]
  (if (:success operation-results)
    (merge base-data {:success true :info (:info operation-results)})
    (merge base-data {:success false :error-message (:error-message operation-results)})))

(defn- save-applications-to-s3 [^SiirtotiedostoPalvelu client applications execution-id file-count additional-info]
  (let [json (json/generate-string applications)
        stream (input-stream (.getBytes json))]
    (log/info execution-id "Saving" (count applications) "applications as json to s3 in siirtotiedosto, file count" file-count)
    (try (.saveSiirtotiedosto client "ataru" "hakemus" additional-info execution-id file-count stream 2)
         {:success true
          :error-message nil}
         (catch Exception e
           (log/error (str execution-id "Ei onnistuttu tallentamaan hakemuksia:" (.getMessage e)) e)
           {:success false
            :error-message (.getMessage e)}))))

(defn- save-forms-to-s3 [^SiirtotiedostoPalvelu client forms execution-id file-count additional-info]
  (let [json (json/generate-string forms)
        stream (input-stream (.getBytes json))]
    (log/info execution-id "Saving" (count forms) "forms as json to s3 in siirtotiedosto, file count" file-count)
    (try (.saveSiirtotiedosto client "ataru" "lomake" additional-info execution-id file-count stream 2)
         {:success true
          :error-message nil}
         (catch Exception e
           (log/error (str execution-id "Ei onnistuttu tallentamaan lomakkeita:" (.getMessage e)) e)
           {:success false
            :error-message (.getMessage e)}))))

(defn- combine-results [applications-result forms-result]
  (let [all-successful (and (:success applications-result) (:success forms-result))
        error (or
                (:error-message applications-result)
                (:error-message forms-result))]
    (-> applications-result
        (assoc :success all-successful)
        (assoc :error-message error)
        (assoc :info {:applications (:total-count applications-result)
                      :forms        (:total-count forms-result)}))))


(defrecord CommonSiirtotiedostoService [siirtotiedosto-client]
  SiirtotiedostoService
  (siirtotiedosto-applications
    [_ params]
    (let [execution-id (:execution-uuid params)
          chunks-done (atom 0)
          total-count (atom 0)
          changed-ids (->> (application-store/siirtotiedosto-application-ids params)
                           (map :id))
          partitions (partition applications-page-size applications-page-size nil changed-ids)]
      (log/info execution-id "Changed application ids in total: " (count changed-ids) ", partition size " applications-page-size ", partitions:" (count partitions))
      (let [chunk-results (doall (for [application-ids partitions]
                                          (let [start (System/currentTimeMillis)
                                                applications-chunk (application-store/siirtotiedosto-applications-for-ids application-ids)
                                                {:keys [success error-message]} (save-applications-to-s3 siirtotiedosto-client applications-chunk execution-id (inc @chunks-done) (or (:haku-oid params) ""))]
                                            (log/info execution-id "Applications-chunk" (str (swap! chunks-done inc) "/" (count partitions))
                                                      "complete, took" (- (System/currentTimeMillis) start) ", success " success)
                                            (when success (swap! total-count (fn [acc amt] (+ acc amt)) (count application-ids)))
                                            {:success success
                                              :error-message error-message})))]
        (log/info execution-id "application-chunk results" chunk-results)
        (-> params
            (assoc :success (every? boolean (map :success chunk-results)))
            (assoc :error-message (first (filter #(some? %) (map :error-message chunk-results))))
            (assoc :total-count @total-count)))))

  (siirtotiedosto-forms
    [_ params]
    (let [execution-id (:execution-uuid params)
          chunks-done (atom 0)
          total-count (atom 0)
          changed-ids (->> (form-store/siirtotiedosto-form-ids params)
                           (map :id))
          partitions (partition forms-page-size forms-page-size nil changed-ids)]
      (log/info execution-id "Changed form ids in total: " (count changed-ids) ", partition size " forms-page-size ", partitions:" (count partitions))
      (let [chunk-results (doall (for [form-ids partitions]
                                    (let [start (System/currentTimeMillis)
                                          forms-chunk (form-store/fetch-forms-by-ids form-ids)
                                          haku-oid (:haku-oid params)
                                          {:keys [success error-message]} (save-forms-to-s3 siirtotiedosto-client forms-chunk execution-id (inc @chunks-done) (or haku-oid ""))]
                                      (log/info execution-id "Forms-chunk" (str (swap! chunks-done inc) "/" (count partitions))
                                                "complete, took" (- (System/currentTimeMillis) start) ", success " success)
                                      (when success (swap! total-count (fn [acc amt] (+ acc amt)) (count form-ids)))
                                      {:success success
                                       :error-message error-message})))]
        (log/info execution-id "form-chunk results" chunk-results)
        (-> params
            (assoc :success (every? boolean (map :success chunk-results)))
            (assoc :error-message (first (filter #(some? %) (map :error-message chunk-results))))
            (assoc :total-count @total-count)))))

  (siirtotiedosto-everything
    [this params]
    (let [applications-result (siirtotiedosto-applications this params)
          forms-result        (siirtotiedosto-forms this params)]
      (combine-results applications-result forms-result)))

  (create-next-siirtotiedosto
    [this]
    (let [execution-id            (str (UUID/randomUUID))
          previous-success        (siirtotiedosto-store/get-latest-successful-data)
          new-siirtotiedosto-data (siirtotiedosto-store/insert-new-siirtotiedost-operation (create-new-siirtotiedosto-data previous-success execution-id))]
      (log/info (str execution-id "Launching siirtotiedosto operation. Previous data: " previous-success ", new data " new-siirtotiedosto-data))
      (try
        (let [siirtotiedosto-data-after-operation (->> (siirtotiedosto-everything this new-siirtotiedosto-data)
                                                       (update-siirtotiedosto-data new-siirtotiedosto-data))]
          (if (:success siirtotiedosto-data-after-operation)
            (log/info execution-id "Created siirtotiedostot" siirtotiedosto-data-after-operation)
            (log/error execution-id "Siirtotiedosto operation failed:" siirtotiedosto-data-after-operation))
          (siirtotiedosto-store/update-siirtotiedosto-operation siirtotiedosto-data-after-operation)
          siirtotiedosto-data-after-operation)
        (catch Exception e
          (let [failed-data (-> new-siirtotiedosto-data
                                (assoc :success false)
                                (assoc :error-message (.getMessage e)))]
            (log/error (str execution-id "Siirtotiedosto operation failed: ") e)
            (siirtotiedosto-store/update-siirtotiedosto-operation failed-data)
            failed-data))))))

(defn new-siirtotiedosto-service [] (->CommonSiirtotiedostoService nil))
