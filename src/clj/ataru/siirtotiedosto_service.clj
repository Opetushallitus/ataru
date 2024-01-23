(ns ataru.siirtotiedosto-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [schema-tools.core :as st]
            [clojure.java.io :refer [input-stream]])
  (:import (fi.vm.sade.valinta.dokumenttipalvelu SiirtotiedostoPalvelu)
           (java.util Optional)))

(defprotocol SiirtotiedostoService
  (siirtotiedosto-applications [this params])
  (siirtotiedosto-forms [this params]))

(def applications-page-size 20000);todo, nämä konffattaviksi
(def forms-page-size 200)

(s/defschema SiirtotiedostoFormSchema {:properties        s/Any
                                        :deleted          (s/maybe s/Bool)
                                        :key              s/Str
                                        :content          [{:fieldClass s/Str
                                                            :id         s/Str
                                                            :fieldType  s/Str
                                                            s/Any       s/Any}]
                                        :name             {s/Any s/Str}
                                        :organization-oid s/Str
                                        :created-by       s/Str
                                        :created-time     org.joda.time.DateTime
                                        :languages        [s/Str]})

;Todo fixme Hakemuksen henkilöoid-kentän kanssa voi olla ongelma sikäli, että henkilöOidin lisääminen hakemuksen tallentamisen jälkeen ei päivitä hakemuksen muokkaushetkeä
;eli jälkikäteen lisätty person_oid ei välttämättä päädy mihinkään siirtotiedostoon
(s/defschema SiirtotiedostoApplicationSchema {:hakemusOid s/Str
                                              :state s/Any
                                              (s/optional-key :form_key) s/Str
                                              (s/optional-key :keyValues) {s/Any s/Any}
                                              (s/optional-key :attachments) {s/Any s/Any}
                                              (s/optional-key :created_time) org.joda.time.DateTime
                                              (s/optional-key :eligibility-set-automatically) s/Any
                                              (s/optional-key :submitted) org.joda.time.DateTime
                                              (s/optional-key :lang) s/Str
                                              (s/optional-key :application_hakukohde_reviews) s/Any
                                              (s/optional-key :hakuOid) (s/maybe s/Str)
                                              (s/optional-key :form) s/Num
                                              (s/optional-key :person_oid) (s/maybe s/Str)})
(s/defschema SiirtotiedostoInactivatedApplicationSchema {:hakemusOid s/Str
                                                         :state "inactivated"})
(defn- save-applications-to-s3 [^SiirtotiedostoPalvelu client applications start-time]
  (let [schema-compliant-applications (map #(st/select-schema % SiirtotiedostoApplicationSchema) applications)
        json (json/generate-string schema-compliant-applications)
        stream (input-stream (.getBytes json))]
    (log/info "Saving" (count json) "of applications json to s3 in siirtotiedosto! Start " start-time)
    (try (.saveSiirtotiedosto client (Optional/empty) (Optional/empty) "ataru" (Optional/of "applications") stream) (catch Exception e
                                                                                                                                  (log/error (str "Ei onnistuttu tallentamaan hakemuksia:" e))))))

(defn- save-forms-to-s3 [^SiirtotiedostoPalvelu client forms start-time]
  (let [schema-compliant-forms (map #(st/select-schema % SiirtotiedostoFormSchema) forms)
        json (json/generate-string schema-compliant-forms)
        stream (input-stream (.getBytes json))]
    (log/info "Saving" (count json) "of forms json to s3 in siirtotiedosto! Start " start-time)
    (try
      (.saveSiirtotiedosto client (Optional/empty) (Optional/empty) "ataru" (Optional/of "forms") stream)
      (catch Exception e
        (log/error (str "Ei onnistuttu tallentamaan lomakkeita:" (.getMessage e)))))))

(defrecord CommonSiirtotiedostoService [siirtotiedosto-client]
  SiirtotiedostoService
  (siirtotiedosto-applications
    [_ params]
    (let [done (atom 0)
          changed-ids (->> (application-store/siirtotiedosto-application-ids params)
                           (map :id))
          partitions (partition applications-page-size applications-page-size nil changed-ids)]
      (log/info "Changed application ids in total: " (count changed-ids) ", partitions:" (count partitions))
      (let [first-application-per-chunk (doall (for [application-ids partitions]
                                          (let [start (System/currentTimeMillis)
                                                applications-chunk (application-store/siirtotiedosto-applications-for-ids application-ids)]
                                            (save-applications-to-s3 siirtotiedosto-client applications-chunk (:modified_after params))
                                            (log/info "Applications-chunk" (str (swap! done inc) "/" (count partitions)) "complete, took" (- (System/currentTimeMillis) start))
                                            (first applications-chunk))))]
        ;(log/info "first applications" (flatten first-application-per-chunk))
        {:applications (flatten first-application-per-chunk)
         :success true
         :modified-before (:modified-before params)})))

  (siirtotiedosto-forms
    [_ params]
    (let [done (atom 0)
          changed-ids (->> (form-store/siirtotiedosto-form-ids params)
                           (map :id))
          partitions (partition forms-page-size forms-page-size nil changed-ids)]
      (log/info "Changed form ids in total: " (count changed-ids) ", partitions:" (count partitions))
      (let [first-forms-per-chunk (doall (for [form-ids partitions]
                                    (let [start (System/currentTimeMillis)
                                          forms-chunk (form-store/fetch-forms-by-ids form-ids)]
                                      (save-forms-to-s3 siirtotiedosto-client forms-chunk (:modified_after params))
                                      (log/info "Forms-chunk" (str (swap! done inc) "/" (count partitions)) "complete, took" (- (System/currentTimeMillis) start))
                                      (first forms-chunk))))]
        ;(log/info "first forms" (flatten first-forms-per-chunk))
        {:forms   (flatten first-forms-per-chunk)
         :success true
         :modified-before (:modified-before params)})))

  )

(defn new-siirtotiedosto-service [] (->CommonSiirtotiedostoService nil))
