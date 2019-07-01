(ns ataru.forms.form-access-control
  (:require
   [ataru.applications.application-store :as application-store]
   [ataru.forms.form-store :as form-store]
   [ataru.schema.form-schema :as form-schema]
   [ataru.virkailija.editor.form-diff :as form-diff]
   [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
   [ataru.tarjonta-service.tarjonta-service :refer [yhteishaku?]]
   [ataru.organization-service.session-organizations :as session-orgs]
   [ataru.organization-service.organization-client :refer [oph-organization]]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [taoensso.timbre :refer [warn]]))

(defn form-allowed-by-id?
  "id identifies a version of the form"
  [form-id session organization-service right]
  (session-orgs/organization-allowed?
   session
   organization-service
   (fn [] (form-store/get-organization-oid-by-id form-id))
   [right]))

(defn form-allowed-by-haku?
  "True when user has organization in haku or in some hakukohde"
  [form-key session virkailija-tarjonta-service]
  (let [form-keys-from-tarjonta (set (keys (tarjonta-protocol/get-forms-in-use virkailija-tarjonta-service session)))]
    (contains? form-keys-from-tarjonta form-key)))

(defn get-organizations-with-edit-rights
  [session]
  (if (:selected-organization session)
    (if (or (-> session :identity :superuser)
            (contains? (-> session :selected-organization :rights) :form-edit))
      [(:selected-organization session)]
      [])
    (-> session
        :identity
        :user-right-organizations
        :form-edit)))

(defn- check-lock-authorization [{:keys [key id]} session tarjonta-service virkailija-tarjonta-service]
  (when-not (-> session :identity :superuser)
    (let [haut        (keys (get (tarjonta-protocol/get-forms-in-use virkailija-tarjonta-service session) key))
          yhteishaut? (->> haut
                           (map #(tarjonta-protocol/get-haku tarjonta-service %))
                           (filter yhteishaku?))]
      (when (first yhteishaut?)
            (throw (user-feedback-exception
                     (format "Lukitseminen ja avaaminen yhteishaussa vain rekisterinpitäjän oikeuksilla!")))))))

(defn- check-edit-authorization [form session virkailija-tarjonta-service organization-service do-fn]
  (let [organizations (get-organizations-with-edit-rights session)]
    (when (not (:organization-oid form))
      (throw (user-feedback-exception (str "Lomaketta ei ole kytketty organisaatioon"
                                        (when-not (empty? organizations)
                                          (str " " (vec organizations)))))))
    (cond
      (and
       (:id form) ; Updating, since form already has id
       (not (or (form-allowed-by-id? (:id form) session organization-service :form-edit)
                (form-allowed-by-haku? (:key form) session virkailija-tarjonta-service))))
      (throw (user-feedback-exception
               (str "Ei oikeutta lomakkeeseen "
                    (:id form)
                    " organisaatioilla "
                    (vec organizations))))

      ;The potentially new organization for form is not allowed for user
      (and (not (:id form))
           (not (session-orgs/organization-allowed?
                  session
                  organization-service
                  (constantly (:organization-oid form))
                  [:form-edit])))
      (throw (user-feedback-exception
              (str "Ei oikeutta organisaatioon "
                   (:organization-oid form)
                   " käyttäjän organisaatioilla "
                   (vec organizations))))

      :else
      (do-fn))))

(defn- check-form-field-id-duplicates
  [form]
  (let [form-element-ids (atom [])]
    (clojure.walk/prewalk
      (fn [x]
        (when (and (map-entry? x)
                   (= :id (key x)))
          (swap! form-element-ids conj (val x)))
        x)
      (:content form))
    (when (and
            (not-empty @form-element-ids)
            (not (apply distinct? @form-element-ids)))
      (throw (Exception. (str "Duplicate element id in form: " (pr-str (keep #(when (< 1 (second %)) (first %))
                                                                             (frequencies @form-element-ids)))))))))

(defn post-form [form session virkailija-tarjonta-service organization-service]
  (let [organization-oids (map :oid (get-organizations-with-edit-rights session))
        first-org-oid     (first organization-oids)
        form-with-org     (assoc form :organization-oid (or (:organization-oid form) first-org-oid))]
    (check-form-field-id-duplicates form)
    (check-edit-authorization
     form-with-org
     session
     virkailija-tarjonta-service
     organization-service
     (fn []
       (form-store/create-form-or-increment-version!
        (assoc
         form-with-org
         :created-by (-> session :identity :username))
         session)))))

(defn edit-form-with-operations
  [id operations session virkailija-tarjonta-service organization-service]
  (let [latest-version (form-store/fetch-form id)]
    (if (:locked latest-version)
      (throw (user-feedback-exception "Lomakkeen muokkaus on estetty."))
      (let [coerced-form (form-schema/form-coercer latest-version)
            updated-form (form-diff/apply-operations coerced-form operations)]
        (post-form updated-form session virkailija-tarjonta-service organization-service)))))

(defn delete-form [form-id session virkailija-tarjonta-service organization-service]
  (let [form (form-store/fetch-latest-version form-id)]
    (check-edit-authorization form session virkailija-tarjonta-service organization-service
      (fn []
        (form-store/create-form-or-increment-version!
         (assoc form :deleted true)
          session)))))

(defn- get-forms-as-ordinary-user [session virkailija-tarjonta-service organization-oids]
  (let [forms-with-organization-oids                              (form-store/get-forms organization-oids)
        forms-using-tarjonta-keys                                 (set (keys (tarjonta-protocol/get-forms-in-use virkailija-tarjonta-service session)))
        missing-tarjonta-form-keys                                (clojure.set/difference forms-using-tarjonta-keys (set (map :key forms-with-organization-oids)))
        tarjonta-forms-with-some-hakukohde-from-user-organization (form-store/get-forms-by-keys (vec missing-tarjonta-form-keys))]
    (concat forms-with-organization-oids tarjonta-forms-with-some-hakukohde-from-user-organization)))

(defn get-forms-for-editor [session virkailija-tarjonta-service organization-service]
  {:forms (session-orgs/run-org-authorized
           session
           organization-service
           [:form-edit]
           vector
           #(get-forms-as-ordinary-user session virkailija-tarjonta-service (vec %))
           #(form-store/get-all-forms))})

(defn update-form-lock [form-id operation session tarjonta-service virkailija-tarjonta-service organization-service]
  (let [latest-version  (form-store/fetch-form form-id)
        previous-locked (:locked latest-version)
        lock?           (= "close" operation)
        updated-form    (merge latest-version
                               (if lock?
                                 {:locked "now()" :locked-by (-> session :identity :oid)}
                                 {:locked nil :locked-by nil}))]
    (check-lock-authorization latest-version session tarjonta-service virkailija-tarjonta-service)
    (if (or (and lock? (some? previous-locked))
            (and (not lock?) (nil? previous-locked)))
      (throw (user-feedback-exception "Lomakkeen sisältö on muuttunut. Lataa sivu uudelleen."))
      (select-keys (post-form updated-form session virkailija-tarjonta-service organization-service)
                   [:locked :id]))))
