(ns ataru.applications.application-access-control
  (:require
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]))

(defn check-form-access [form-key session organization-service right]
  (when-not
    (form-access-control/form-allowed-by-key? form-key session organization-service right)
    (throw (user-feedback-exception (str "Lomake " form-key " ei ole sallittu")))))

(defn check-application-access [application-key session organization-service right]
  (when-not
    (session-orgs/organization-allowed?
      session
      organization-service
      [right]
      #(application-store/get-application-organization-oid application-key))
    (throw (user-feedback-exception (str "Hakemus " application-key " ei ole sallittu")))))

(defn- empty-applications-result-fn [] {:applications []})

(defn get-application-list-by-hakukohde [hakukohde-oid session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications]
   empty-applications-result-fn
   #(hash-map :applications (application-store/get-application-list-by-hakukohde hakukohde-oid %))
   #(hash-map :applications (application-store/get-full-application-list-by-hakukohde hakukohde-oid))))

(defn get-application-list-by-haku [haku-oid session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications]
   empty-applications-result-fn
   #(hash-map :applications (application-store/get-application-list-by-haku haku-oid %))
   #(hash-map :applications (application-store/get-full-application-list-by-haku haku-oid))))
