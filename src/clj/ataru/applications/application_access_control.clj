(ns ataru.applications.application-access-control
  (:require
    [ataru.log.audit-log :as audit-log]
    [taoensso.timbre :as log]
    [ataru.organization-service.session-organizations :as session-orgs]
    [ataru.user-rights :as user-rights]
    [ataru.applications.application-store :as application-store]
    [ataru.person-service.person-service :as person-service]
    [ataru.odw.odw-service :as odw-service]
    [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
    [ataru.tilastokeskus.tilastokeskus-service :as tilastokeskus-service]
    [ataru.valintapiste.valintapiste-service :as valintapiste-service]
    [ataru.util :as util]
    [clojure.set :as set]
    [ataru.suoritus.suoritus-service :as suoritus-service]
    [ataru.applications.suoritus-filter :as suoritus-filter]
    [clj-time.core :as time]
    [medley.core :refer [distinct-by]]))

(defn authorized-by-form?
  [authorized-organization-oids application]
  (boolean
    (when-let [organization-oid (:organization-oid application)]
      (authorized-organization-oids organization-oid))))

(defn- authorized-by-tarjoaja?
  [authorized-organization-oids hakukohde]
  (let [tarjoajat       (set (:tarjoaja-oids hakukohde))
        hakukohderyhmat (set (:ryhmaliitokset hakukohde))]
    (boolean
      (some authorized-organization-oids
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

(defn filter-authorized-by-form-or-hakukohde
  [tarjonta-service organization-oid-authorized? applications]
  (filter-authorized
    tarjonta-service
    (some-fn
      (partial authorized-by-form? organization-oid-authorized?)
      (partial authorized-by-tarjoajat? organization-oid-authorized?))
    applications))

(defn organization-oids-for-opinto-ohjaaja
  [organization-service session]
  (->> (session-orgs/select-organizations-for-rights
         organization-service
         session
         [:opinto-ohjaaja])
    (map :oid)
    set))

(defn linked-oids-for-person-oids [person-service oids]
  (let [onr-data (person-service/linked-oids person-service oids)]
    (mapcat vec (map :linked-oids (vals onr-data)))))

(defn- person-oids-for-oppilaitos
  [suoritus-service person-service oppilaitos-oid]
  (let [now (time/now)]
    (->>
      (suoritus-service/oppilaitoksen-opiskelijat
        suoritus-service
        oppilaitos-oid
        (suoritus-filter/year-for-suoritus-filter now)
        (suoritus-filter/luokkatasot-for-suoritus-filter))
      (map :person-oid)
      (linked-oids-for-person-oids person-service))))

(defn- authorized-by-person-oid?
  [authorized-person-oids application]
  (let [application-person-oid (:person-oid application)]
    (some #(= application-person-oid %) authorized-person-oids)))

(defn- filter-applications-by-lahtokoulu
  [suoritus-service person-service authorized-organization-oids applications]
  (let [authorized-person-oids (mapcat (partial person-oids-for-oppilaitos suoritus-service person-service) authorized-organization-oids)]
    (->> applications
      (filter (partial authorized-by-person-oid? authorized-person-oids))
      (map remove-organization-oid))))

(defn- filter-authorized-by-lahtokoulu
  [organization-service suoritus-service person-service session applications authorized-applications]
  (let [opinto-ohjaaja-authorized-organization-oids (organization-oids-for-opinto-ohjaaja organization-service session)]
    (if (and
          (some? opinto-ohjaaja-authorized-organization-oids)
          (not= (count applications) (count authorized-applications)))
      (let [authorized-application-oid? (set (map :oid authorized-applications))
            unauthorized-applications   (remove (comp authorized-application-oid? :oid) applications)]
        (filter-applications-by-lahtokoulu suoritus-service person-service opinto-ohjaaja-authorized-organization-oids unauthorized-applications))
      [])))

(defn- application-authorized-by-lahtokoulu?
  [organization-service suoritus-service person-service session application]
  (-> (filter-authorized-by-lahtokoulu organization-service suoritus-service person-service session [application] [])
    seq
    boolean))

(defn organization-oid-authorized-by-session-pred
  [organization-service session]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (fn [] (constantly false))
    (fn [oids] #(contains? oids %))
    (fn [] (constantly true))))

(defn filter-authorized-by-session
  [organization-service tarjonta-service suoritus-service person-service session applications]
  (let [organization-oid-authorized?     (organization-oid-authorized-by-session-pred organization-service session)
        normally-authorized-applications (filter-authorized-by-form-or-hakukohde tarjonta-service organization-oid-authorized? applications)
        opo-authorized-applications      (filter-authorized-by-lahtokoulu organization-service suoritus-service person-service session applications normally-authorized-applications)]
    (if (= 0 (count opo-authorized-applications))
      normally-authorized-applications
      (->> (concat normally-authorized-applications opo-authorized-applications)
           (distinct-by :key)))))

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

(defn- opinto-ohjaaja-access-authorized?
  [organization-service suoritus-service person-service session application-key]
  (let [[application-authorization-data] (application-store/applications-authorization-data [application-key])]
    (application-authorized-by-lahtokoulu? organization-service suoritus-service person-service session application-authorization-data)))

(defn- applications-opinto-ohjaaja-access-authorized?
  [organization-service suoritus-service person-service session application-keys]
  (let [applications-authorization-data (application-store/applications-authorization-data application-keys)
        authorized-applications (filter-authorized-by-lahtokoulu organization-service suoritus-service person-service session applications-authorization-data [])]
    (= (count authorized-applications) (count application-keys))))

(defn application-edit-authorized?
  [organization-service tarjonta-service suoritus-service person-service session application-key]
  (or
    (applications-access-authorized? organization-service tarjonta-service session [application-key] [:edit-applications])
    (opinto-ohjaaja-access-authorized? organization-service suoritus-service person-service session application-key)))

(defn application-view-authorized?
  [organization-service tarjonta-service suoritus-service person-service session application-key]
  (or
    (applications-access-authorized? organization-service tarjonta-service session [application-key] [:view-applications :edit-applications])
    (opinto-ohjaaja-access-authorized? organization-service suoritus-service person-service session application-key)))

(defn applications-access-authorized-including-opinto-ohjaaja?
  [organization-service tarjonta-service suoritus-service person-service session application-keys rights]
  (or
    (applications-access-authorized? organization-service tarjonta-service session application-keys rights)
    (applications-opinto-ohjaaja-access-authorized? organization-service suoritus-service person-service session application-keys)))

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
  [organization-service suoritus-service person-service session application rights-by-hakukohde]
  (or
    (->> rights-by-hakukohde
      (some #(contains? (val %) :edit-applications))
      (true?))
    (application-authorized-by-lahtokoulu? organization-service suoritus-service person-service session application)))

(defn get-latest-application-by-key
  [organization-service tarjonta-service suoritus-service person-service audit-logger session application-key]
  (let [application         (application-store/get-latest-application-by-key application-key)
        rights-by-hakukohde (some->> application
                                     vector
                                     (populate-applications-hakukohteet tarjonta-service)
                                     first
                                     (rights-by-hakukohde organization-service session))]
    (when (or (some #(not-empty
                       (set/intersection
                         #{:view-applications :edit-applications}
                         (val %)))
                rights-by-hakukohde)
              (seq
                (filter-applications-by-lahtokoulu
                  suoritus-service
                  person-service
                  (organization-oids-for-opinto-ohjaaja organization-service session)
                  [application])))
      (audit-log/log audit-logger
                     {:new       (dissoc application :answers)
                      :id        {:applicationOid application-key}
                      :session   session
                      :operation audit-log/operation-read})
      (-> application
          (assoc :can-edit? (can-edit-application? organization-service suoritus-service person-service session application rights-by-hakukohde))
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

(defn get-applications-for-odw [organization-service session person-service tarjonta-service valintalaskentakoostepalvelu-service suoritus-service from-date limit offset application-key]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(odw-service/get-applications-for-odw person-service tarjonta-service valintalaskentakoostepalvelu-service suoritus-service from-date limit offset application-key)))

(defn get-applications-for-tilastokeskus [organization-service session tarjonta-service haku-oid hakukohde-oid]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(tilastokeskus-service/get-application-info-for-tilastokeskus tarjonta-service haku-oid hakukohde-oid)))

(defn get-applications-for-valintapiste [organization-service session haku-oid hakukohde-oid]
  (session-orgs/run-org-authorized
    session
    organization-service
    [:view-applications :edit-applications]
    (constantly nil)
    (constantly nil)
    #(valintapiste-service/get-application-info-for-valintapiste haku-oid hakukohde-oid)))

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
