(ns ataru.virkailija.user.session-organizations
  [ataru.virkailija.user.organization-client :as organization-client])

(defn organizations [session] (-> session :identity :organizations))

;(defn org-oids [session] (map :oid (organizations session)))

(defn all-org-oids [organization-service organizations]
  (let [all-organizations (.get-all-organizations organization-service organizations)]
        (map :oid all-organizations)))

(defn get-all-organization-oids
  "Gives all the organization oids allowed for user's session
   (including subhierarchy)"
  [session organization-service]
  (all-org-oids organization-service (organizations session)))

(defn run-org-authorized [session
                          organization-service
                          when-no-orgs-fn
                          when-ordinary-user-fn
                          when-superuser-fn]
  (let [organizations     (organizations session)
        organization-oids (map :oid organizations)]
    (cond
      (empty? organizations)
      (when-no-orgs-fn)

      (some #{organization-client/oph-organization} organization-oids)
      (when-superuser-fn)

      :else
      (when-ordinary-user-fn (all-org-oids organization-service organizations)))))
