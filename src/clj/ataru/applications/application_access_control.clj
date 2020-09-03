(ns ataru.applications.application-access-control
  (:require
   [ataru.log.audit-log :as audit-log]
   [ataru.organization-service.session-organizations :as session-orgs]
   [ataru.user-rights :as user-rights]
   [ataru.applications.application-store :as application-store]
   [ataru.odw.odw-service :as odw-service]
   [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
   [ataru.tilastokeskus.tilastokeskus-service :as tilastokeskus-service]
   [ataru.util :as util]
   [clojure.set :as set]))

(defn authorized-by-form?
  [authorized-organization-oids application]
  (boolean (authorized-organization-oids (:organization-oid application))))

(defn- authorized-by-tarjoaja?
  [authorized-organization-oids hakukohde]
  (let [tarjoajat       (set (:tarjoaja-oids hakukohde))
        hakukohderyhmat (set (:ryhmaliitokset hakukohde))]
    (boolean (some authorized-organization-oids
                   (concat tarjoajat hakukohderyhmat)))))

(defn authorized-by-tarjoajat?
  [authorized-organization-oids application]
  (boolean
   (some #(authorized-by-tarjoaja? authorized-organization-oids %)
         (:hakukohde application))))

(defn- populate-applications-hakukohteet
  [tarjonta-service applications]
  (let [hakukohteet (->> applications
                         (mapcat :hakukohde)
                         distinct
                         (tarjonta-service/get-hakukohteet tarjonta-service)
                         (reduce #(assoc %1 (:oid %2) %2) {}))]
    (map #(update % :hakukohde (partial mapv (fn [oid] (get hakukohteet oid {:oid oid}))))
         applications)))

(defn- depopulate-application-hakukohteet
  [application]
  (update application :hakukohde (partial mapv :oid)))

(defn- remove-organization-oid [application]
  (dissoc application :organization-oid))

(defn filter-authorized
  [tarjonta-service predicate applications]
  (->> applications
       (populate-applications-hakukohteet tarjonta-service)
       (filter predicate)
       (map depopulate-application-hakukohteet)
       (map remove-organization-oid)))

(defn applications-access-authorized?
  [organization-service tarjonta-service session application-keys rights]
  (session-orgs/run-org-authorized
   session
   organization-service
   rights
   (constantly false)
   #(->> (application-store/applications-authorization-data application-keys)
         (populate-applications-hakukohteet tarjonta-service)
         (every? (some-fn (partial authorized-by-form? %)
                          (partial authorized-by-tarjoajat? %))))
   (constantly true)))

(defn- rights-by-hakukohde
  [organization-service session application]
  (let [authorized? (memoize
                     (fn [right]
                       (session-orgs/run-org-authorized
                        session
                        organization-service
                        [right]
                        (fn [] (fn [_] false))
                        (fn [orgs]
                          (fn [hakukohde]
                            (or (authorized-by-form? orgs application)
                                (authorized-by-tarjoaja? orgs hakukohde))))
                        (fn [] (fn [_] true)))))]
    (reduce (fn [acc hakukohde]
              (->> user-rights/right-names
                   (filter #((authorized? %) hakukohde))
                   set
                   (assoc acc (:oid hakukohde))))
            {}
            (or (seq (:hakukohde application))
                [{:oid "form"}]))))

(defn- can-edit-application?
  [rights-by-hakukohde]
  (->> rights-by-hakukohde
       (some #(contains? (val %) :edit-applications))
       (true?)))

(defn get-latest-application-by-key
  [organization-service tarjonta-service audit-logger session application-key]
  (let [application         (application-store/get-latest-application-by-key application-key)
        rights-by-hakukohde (some->> application
                                     vector
                                     (populate-applications-hakukohteet tarjonta-service)
                                     first
                                     (rights-by-hakukohde organization-service session))]
    (when (some #(not-empty
                  (set/intersection
                   #{:view-applications :edit-applications}
                   (val %)))
                rights-by-hakukohde)
      (audit-log/log audit-logger
                     {:new       (dissoc application :answers)
                      :id        {:applicationOid application-key}
                      :session   session
                      :operation audit-log/operation-read})
      (-> application
          (assoc :can-edit? (can-edit-application? rights-by-hakukohde))
          (assoc :rights-by-hakukohde (util/map-kv rights-by-hakukohde vec))
          remove-organization-oid))))

(defn- populate-hakukohde
  [external-application]
  (assoc external-application
         :hakukohde (map :hakukohdeOid (:hakutoiveet external-application))))

(defn- remove-hakukohde
  [external-application]
  (dissoc external-application :hakukohde))

(defn external-applications
  [organization-service tarjonta-service session haku-oid hakukohde-oid hakemus-oids]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    #(->> (application-store/get-external-applications haku-oid
                                                       hakukohde-oid
                                                       hakemus-oids)
          (map populate-hakukohde)
          (filter-authorized tarjonta-service
                             (some-fn (partial authorized-by-form? %)
                                      (partial authorized-by-tarjoajat? %)))
          (map remove-hakukohde))
    #(map remove-organization-oid (application-store/get-external-applications
                                   haku-oid
                                   hakukohde-oid
                                   hakemus-oids))))

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
   #(application-store/get-full-application-list-by-person-oid-for-omatsivut-and-refresh-old-secrets
     person-oid)))

(defn onr-applications [organization-service session person-oid]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   (constantly nil)
   #(application-store/onr-applications person-oid)))

(defn get-applications-for-odw [organization-service session person-service tarjonta-service from-date limit offset application-key]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(odw-service/get-applications-for-odw person-service tarjonta-service from-date limit offset application-key)))

(defn get-applications-for-tilastokeskus [organization-service session tarjonta-service haku-oid hakukohde-oid]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(tilastokeskus-service/get-application-info-for-tilastokeskus tarjonta-service haku-oid hakukohde-oid)))

(defn get-applications-for-valintalaskenta [organization-service session hakukohde-oid application-keys]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(application-store/get-applications-for-valintalaskenta hakukohde-oid application-keys)))

(defn siirto-applications
  [tarjonta-service organization-service session hakukohde-oid application-keys]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   #(->> (application-store/siirto-applications hakukohde-oid application-keys)
         (map (fn [a] (assoc a :hakukohde (:hakutoiveet a))))
         (filter-authorized tarjonta-service
                            (some-fn (partial authorized-by-form? %)
                                     (partial authorized-by-tarjoajat? %)))
         (map (fn [a] (dissoc a :hakukohde)))
         (map remove-organization-oid))
   #(->> (application-store/siirto-applications hakukohde-oid application-keys)
         (map remove-organization-oid))))

(defn valinta-ui-applications
  [organization-service tarjonta-service person-service session query]
  (session-orgs/run-org-authorized
   session
   organization-service
   [:view-applications :edit-applications]
   (constantly nil)
   #(filter-authorized tarjonta-service
                       (partial authorized-by-tarjoajat? %)
                       (application-store/valinta-ui-applications query person-service))
   #(filter-authorized tarjonta-service
                       (constantly true)
                       (application-store/valinta-ui-applications query person-service))))
