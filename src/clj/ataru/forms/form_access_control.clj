(ns ataru.forms.form-access-control
  (:require
   [ataru.forms.form-store :as form-store]
   [ataru.virkailija.user.organization-client :refer [oph-organization]]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [taoensso.timbre :refer [warn]]))

(defn- organizations [session] (-> session :identity :organizations))
(defn- org-oids [session] (map :oid (organizations session)))

(defn- all-org-oids [organization-service organizations]
  (let [all-organizations (.get-all-organizations organization-service organizations)]
        (map :oid all-organizations)))

(defn organization-allowed?
  "Parameter organization-oid-handle can be either the oid value or a function which returns the oid"
  [session organization-service organization-oid-handle]
  (let [organizations (organizations session)]
    (cond
      (some #{oph-organization} (map :oid organizations))
      true

      (empty? organizations)
      false

      :else
      (let [organization-oid (if (instance? clojure.lang.IFn organization-oid-handle)
                               (organization-oid-handle)
                               organization-oid-handle)]
        (-> #{organization-oid}
            (some (all-org-oids organization-service organizations))
            boolean)))))

(defn form-allowed-by-key? [form-key session organization-service]
  (organization-allowed?
   session
   organization-service
   (fn [] (form-store/get-organization-oid-by-key form-key))))

(defn form-allowed-by-id?
  "id identifies a version of the form"
  [form-id session organization-service]
  (organization-allowed?
   session
   organization-service
   (fn [] (form-store/get-organization-oid-by-id form-id))))

(defn- check-authenticated [form session organization-service do-fn]
  (let [user-name     (-> session :identity :username)
        organizations (organizations session)
        org-count     (count organizations)]
    (if (not= 1 org-count)
      (do
        (warn (str "User "
                   user-name
                   " has the wrong amount of organizations: "
                   (count organizations)
                   " (required: exactly one).  can't attach form to an ambiguous organization: "
                   (vec organizations)))
        (throw (user-feedback-exception
                 (if (= 0 org-count)
                   "Käyttäjätunnukseen ei ole liitetty organisaatota"
                   "Käyttäjätunnukselle löytyi monta organisaatiota")))))
    (if (and
          (:id form) ; Updating, since form already has id
          (not (form-allowed-by-id? (:id form) session organization-service)))
      (throw (user-feedback-exception
               (str "Ei oikeutta lomakkeeseen "
                    (:id form)
                    " organisaatioilla "
                    (vec organizations)))))
    (do-fn)))

(defn post-form [form session organization-service]
  (check-authenticated form session organization-service
    (fn []
      (let [organization-oids (org-oids session)]
        (form-store/create-form-or-increment-version!
          (assoc form :created-by (-> session :identity :username))
          (first organization-oids))))))

(defn delete-form [form-id session organization-service]
  (let [form (form-store/fetch-latest-version form-id)]
    (check-authenticated form session organization-service
      (fn []
        (let [organization-oids (org-oids session)]
          (form-store/create-form-or-increment-version!
            (assoc form :deleted true)
            (first organization-oids)))))))

(defn get-forms [include-deleted? session organization-service]
  (let [organizations     (organizations session)
        organization-oids (map :oid organizations)]
    ;; OPH organization members can see everything when they're given the correct privilege
    (cond
      (some #{oph-organization} organization-oids)
      {:forms (form-store/get-all-forms include-deleted?)}

      ;; If the user has no organization connected with the required user right, we'll show nothing
      (empty? organization-oids)
      {:forms []}

      :else
      (let [all-oids (all-org-oids organization-service organizations)]
        {:forms (form-store/get-forms include-deleted? all-oids)}))))
