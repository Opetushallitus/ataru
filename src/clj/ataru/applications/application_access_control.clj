(ns ataru.applications.application-access-control
  (:require
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.forms.form-store :as form-store]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [ataru.applications.excel-export :as excel]))

(defn- check-form-access [form-key session organization-service]
  (when-not
      (form-access-control/form-allowed? form-key session organization-service)
    (throw (user-feedback-exception (str "Lomake " form-key " ei ole sallittu")))))

(defn- check-application-access [application-id session organization-service]
  (when-not
      (form-access-control/organization-allowed?
       session
       organization-service
       #(application-store/get-application-organization-oid application-id))
    (throw (user-feedback-exception (str "Hakemus " application-id " ei ole sallittu")))))

(defn get-application-list [form-key session organization-service]
  (check-form-access form-key session organization-service)
  {:applications (application-store/get-application-list form-key)})

(defn get-application [application-id session organization-service]
  (let [application (application-store/get-application application-id)
        form        (form-store/fetch-by-id (:form application))]
    (check-application-access application-id session organization-service)
    {:application application
     :form        form
     :events      (application-store/get-application-events application-id)
     :review      (application-store/get-application-review application-id)}))

(defn get-excel-report-of-applications [form-key session organization-service]
  (check-form-access form-key session organization-service)
  {:status  200
   :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
             "Content-Disposition" (str "attachment; filename=" (excel/filename form-key))}
   :body    (java.io.ByteArrayInputStream. (excel/export-all-applications form-key))})
