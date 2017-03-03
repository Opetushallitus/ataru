(ns ataru.virkailija.user.session-organizations
  (:require
   [schema.core :as s]
   [ataru.virkailija.user.organization-client :as organization-client]
   [ataru.virkailija.user.user-rights :refer [Right]]))

(defn- right-organizations [session] (-> session :identity :user-right-organizations))

(defn- all-org-oids [organization-service organizations]
  (let [all-organizations (.get-all-organizations organization-service organizations)]
        (map :oid all-organizations)))

(defn run-org-authorized [session
                          organization-service
                          right
                          when-no-orgs-fn
                          when-ordinary-user-fn
                          when-superuser-fn]
  {:pre [(s/validate Right right)]}
  (let [organizations     (right (right-organizations session))
        organization-oids (map :oid organizations)]
    (cond
      (empty? organizations)
      (when-no-orgs-fn)

      (some #{organization-client/oph-organization} organization-oids)
      (when-superuser-fn)

      :else
      (when-ordinary-user-fn (all-org-oids organization-service organizations)))))

(defn organization-allowed?
  "Parameter organization-oid-handle can be either the oid value or a function which returns the oid"
  [session organization-service organization-oid-handle right]
  {:pre [(s/validate Right right)]}
  (run-org-authorized
   session
   organization-service
   right
   (fn [] false)
   #(let [organization-oid (if (instance? clojure.lang.IFn organization-oid-handle)
                               (organization-oid-handle)
                               organization-oid-handle)]
     (-> #{organization-oid}
         (some %)
         boolean))
   (fn [] true)))

(defn organization-list
  "Returns a flattened list of organizations with the user rights attached to the orgs"
  [session]
  (vals
   (reduce
    (fn [acc [k vs]]
      (reduce
       (fn [acc' v]
         (let [oid    (:oid v)
               oid-kw (keyword oid)]
           (if (oid-kw acc)
             (update-in acc' [oid-kw :rights] conj k)
             (assoc acc' oid-kw (merge v {:rights [k]})))))
       acc vs))
    {} (right-organizations session))))
