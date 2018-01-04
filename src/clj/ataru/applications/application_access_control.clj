(ns ataru.applications.application-access-control
  (:require
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [ataru.odw.odw-service :as odw-service]))

(defn check-form-access [form-key session organization-service rights]
  (when-not
    (form-access-control/form-allowed-by-key? form-key session organization-service rights)
    (throw (user-feedback-exception (str "Lomake " form-key " ei ole sallittu")))))

(defn check-application-access [application-key session organization-service rights]
  (when-not
    (session-orgs/organization-allowed?
      session
      organization-service
      #(application-store/get-application-organization-oid application-key)
      rights)
    (throw (user-feedback-exception (str "Hakemuksen "
                                         application-key
                                         " käsittely ei ole sallittu")))))

(defn- empty-applications-result-fn [] {:applications []})

(defn get-application-list-by-hakukohde [hakukohde-oid session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   empty-applications-result-fn
   #(hash-map :applications (application-store/get-application-list-by-hakukohde hakukohde-oid %))
   #(hash-map :applications (application-store/get-full-application-list-by-hakukohde hakukohde-oid))))

(defn get-application-list-by-haku [haku-oid session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   empty-applications-result-fn
   #(hash-map :applications (application-store/get-application-list-by-haku haku-oid %))
   #(hash-map :applications (application-store/get-full-application-list-by-haku haku-oid))))

(defn get-application-list-by-ssn [ssn session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   empty-applications-result-fn
   #(hash-map :applications (application-store/get-application-list-by-ssn ssn %))
   #(hash-map :applications (application-store/get-full-application-list-by-ssn ssn))))

(defn get-application-list-by-dob [dob session organization-service]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    empty-applications-result-fn
    #(hash-map :applications (application-store/get-application-list-by-dob dob %))
    #(hash-map :applications (application-store/get-full-application-list-by-dob dob))))

(defn get-application-list-by-email [email session organization-service]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    empty-applications-result-fn
    #(hash-map :applications (application-store/get-application-list-by-email email %))
    #(hash-map :applications (application-store/get-full-application-list-by-email email))))

(defn get-application-list-by-name [name session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   empty-applications-result-fn
   #(hash-map :applications (application-store/get-application-list-by-name name %))
   #(hash-map :applications (application-store/get-full-application-list-by-name name))))

(defn get-latest-application-by-key [application-key session organization-service]
  (-> (session-orgs/run-org-authorized
        session
        organization-service
        [:view-applications :edit-applications]
        empty-applications-result-fn
        #(application-store/get-latest-application-by-key application-key %)
        #(application-store/get-latest-application-by-key-unrestricted application-key))
      (dissoc :secret)))

(defn external-applications [organization-service session haku-oid hakukohde-oid hakemus-oids]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    #(application-store/get-external-applications
       haku-oid
       hakukohde-oid
       hakemus-oids
       %)
    #(application-store/get-external-applications
       haku-oid
       hakukohde-oid
       hakemus-oids
       nil)))

(defn hakurekisteri-applications [organization-service session haku-oid hakukohde-oids person-oids modified-after]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(application-store/get-hakurekisteri-applications
       haku-oid
       hakukohde-oids
       person-oids
       modified-after)))

(defn application-key-to-person-oid [organization-service session haku-oid hakukohde-oids]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   (constantly nil)
   #(application-store/get-person-and-application-oids
     haku-oid
     hakukohde-oids)))

(defn omatsivut-applications [organization-service session person-oid]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   (constantly nil)
   #(application-store/get-full-application-list-by-person-oid-for-omatsivut
     person-oid)))

(defn onr-applications [organization-service session person-oid]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   #(application-store/onr-applications person-oid %)
   #(application-store/onr-applications person-oid nil)))

(defn get-applications-for-odw [organization-service session person-service from-date]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(odw-service/get-applications-for-odw person-service from-date)))

(defn get-applications-for-tilastokeskus [organization-service session haku-oid]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(application-store/get-application-info-for-tilastokeskus haku-oid)))
