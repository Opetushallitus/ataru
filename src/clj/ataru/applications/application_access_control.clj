(ns ataru.applications.application-access-control
  (:require
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]))

(defn check-form-access [form-key session organization-service]
  (when-not
      (form-access-control/form-allowed? form-key session organization-service)
    (throw (user-feedback-exception (str "Lomake " form-key " ei ole sallittu")))))

(defn get-application-list [form-key session organization-service]
  (check-form-access form-key session organization-service)
  {:applications (application-store/get-application-list form-key)})
