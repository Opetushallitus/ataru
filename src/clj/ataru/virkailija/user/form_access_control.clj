(ns ataru.virkailija.user.form-access-control
  (:require
   [ataru.forms.form-store :as form-store]
   [ataru.virkailija.user.organization-client :refer [oph-organization]]
   [taoensso.timbre :refer [warn]]))

(defn- org-oids [session] (map :oid (-> session :identity :organizations)))

(defn post-form [form session organization-service]
  (let [user-name         (-> session :identity :username)
        organization-oids (org-oids session)
        org-count         (count organization-oids)]
    (if (not= 1 org-count)
      (do
        (warn (str "User "
                   user-name
                   " has the wrong amount of organizations: "
                   (count organization-oids)
                   " (required: exactly one).  can't attach form to an ambiguous organization: "
                   (vec organization-oids)))
        {:error
         (if (= 0 org-count)
           "no_organization_for_user"
           "multiple_organizations_for_user")})
      (form-store/create-form-or-increment-version!
       (assoc form :created-by (-> session :identity :username))
       (first organization-oids)))))

(defn get-forms [session organization-service]
  (let [organization-oids (org-oids session)]
    ;; OPH organization members can see everything when they're given the correct privilege
    (cond
      (some #{oph-organization} organization-oids)
      {:forms (form-store/get-all-forms)}

      ;; If the user has no organization connected with the required user right, we'll show nothing
      (empty? organization-oids)
      {:forms []}

      :else
      (let [all-organizations (.get-all-organizations organization-service organization-oids)
            all-oids          (map :oid all-organizations)] ; TODO figure out empty list case (gives sqlexception)
        {:forms (form-store/get-forms all-oids)}))))
