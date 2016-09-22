(ns ataru.virkailija.user.form-access-control
  (:require
   [ataru.forms.form-store :as form-store]
   [ataru.virkailija.user.organization-client :refer [oph-organization]]))

(defn- org-oids [session] (map :oid (-> session :identity :organizations)))

(defn post-form [form session organization-service]
  (let [user-name         (-> session :identity :username)
        organization-oids (org-oids session)]
    (if (not= 1 (count organization-oids))
      (throw (Exception. (str "User "
                              user-name
                              " has the wrong amount of organizations: "
                              (count organization-oids)
                              " (required: exactly one).  can't attach form to an ambiguous organization: "
                              organization-oids))))
    (form-store/create-form-or-increment-version!
     (assoc form :created-by (-> session :identity :username))
     (first organization-oids))))

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
