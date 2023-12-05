(ns ataru.siirtotiedosto-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [clj-time.core :as time]))

(defprotocol SiirtotiedostoService
  (siirtotiedosto-applications [this params])
  (siirtotiedosto-forms [this params]))

(def applications-page-size 10000)

(def forms-page-size 300)

(defn- mock-save-to-s3 [applications]
  (log/info "Saving" (count applications) "applications to s3 in siirtotiedosto!");todo write data to s3 if not empty
  )

(defn- mock-save-forms-to-s3 [forms last-timestamp]
  (let [file-created (time/now)
        json (json/generate-string forms)
        filename (str "ataru-forms__" last-timestamp "__" file-created)]
    (log/info "Saving" (count json) "of json to s3 in siirtotiedosto! Filename" filename)))

(defrecord CommonSiirtotiedostoService [organization-service
                                        tarjonta-service
                                        ohjausparametrit-service
                                        audit-logger
                                        person-service
                                        valinta-tulos-service
                                        koodisto-cache
                                        job-runner
                                        liiteri-cas-client
                                        suoritus-service
                                        form-by-id-cache
                                        valintalaskentakoostepalvelu-service]
  SiirtotiedostoService
  (siirtotiedosto-applications
    [_ params]
    (loop [pages-fetched 0
           offset 0
           statistics []
           last-successful-timestamp nil]
      (log/info "Start collecting applications for page" (inc pages-fetched) {:pages-fetched pages-fetched
                                                                              :offset        offset} ". Last timestamp successfully fetched: " last-successful-timestamp)
      (let [start (System/currentTimeMillis)
            applications (application-store/siirtotiedosto-applications-paged (merge params {:page-size applications-page-size
                                                                                             :offset offset}))
            returned-count (count applications)
            took (- (System/currentTimeMillis) start)
            first-timestamp (:created_time (first applications))
            last-timestamp (:created_time (last applications))
            stat (str "Collecting " returned-count " applications took " took "ms. First created " first-timestamp ", last created " last-timestamp)]
        (log/info "Got page" (inc pages-fetched) "for params" params ":" returned-count stat)
        (mock-save-to-s3 applications)
        (if (= returned-count applications-page-size)
          (recur
            (inc pages-fetched)
            (+ offset returned-count)
            (conj statistics stat)
            last-timestamp) ;todo add some kind of simple error tracking
          (do
            (log/info "Siirtotiedosto ready! params" params ", stats" (conj statistics stat))
            {:success (boolean last-successful-timestamp)
             :last-successful-timestamp last-successful-timestamp})))))

  (siirtotiedosto-forms
    [_ params]
    (let [changed-ids (->> (form-store/siirtotiedosto-form-ids params)
                           (map :id))
          partitions (partition forms-page-size forms-page-size nil changed-ids)]
      (log/info "Changed ids in total: " (count changed-ids) ", partitions:" (count partitions))
      (let [first-forms-per-chunk (for [form-ids partitions]
                                    (let [forms-chunk (form-store/fetch-by-ids form-ids)
                                          last-timestamp (:created-time (last forms-chunk))]
                                      ;(log/info "last form" (last forms-chunk))
                                      (mock-save-forms-to-s3 forms-chunk last-timestamp)
                                      (first forms-chunk)))]
        (log/info "first forms" (flatten first-forms-per-chunk))
        {:forms   (json/generate-string (flatten first-forms-per-chunk))
         :success true})))

  )

(defn new-siirtotiedosto-service [] (->CommonSiirtotiedostoService nil nil nil nil nil nil nil nil nil nil nil nil))
