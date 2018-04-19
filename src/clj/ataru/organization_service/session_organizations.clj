(ns ataru.organization-service.session-organizations
  (:require
    [schema.core :as s]
    [ataru.organization-service.organization-client :as organization-client]
    [ataru.organization-service.user-rights :refer [Right]]
    [ataru.organization-service.organization-service :as organization-service]))

(defn- right-organizations
  [session]
  (-> session :identity :user-right-organizations))

(defn- all-org-oids [organization-service organizations]
  (let [all-organizations (.get-all-organizations organization-service organizations)]
    (set (map :oid all-organizations))))

(defn- filter-orgs-for-rights
  [user-organizations rights organization-oids]
  (filter
    (fn [oid]
      (-> user-organizations
          (get oid)
          :rights
          (clojure.set/intersection (set rights))
          (not-empty)))
    organization-oids))

(defn right-seq? [val] (s/validate [Right] val))

(defn select-organizations-for-rights [session rights]
  {:pre [(right-seq? rights)]}
  (let [right-orgs (right-organizations session)]
    (->> rights
         (map #(get right-orgs %))
         (remove nil?)
         flatten
         distinct)))

(defn run-org-authorized [session
                          organization-service
                          rights
                          when-no-orgs-fn
                          when-ordinary-user-fn
                          when-superuser-fn]
  {:pre [(right-seq? rights)]}
  (let [organizations         (select-organizations-for-rights session rights)
        organization-oids     (set (map :oid organizations))
        selected-organization (:selected-organization session)]
    (cond
      (empty? organizations)
      (when-no-orgs-fn)

      (some? selected-organization)
      (when-ordinary-user-fn
        (filter-orgs-for-rights
          (:organizations session)
          rights
          (all-org-oids organization-service selected-organization)))

      (contains? organization-oids organization-client/oph-organization)
      (when-superuser-fn)

      :else
      (when-ordinary-user-fn (all-org-oids organization-service organizations)))))

(defn organization-allowed?
  "Parameter organization-oid-handle can be either the oid value or a function which returns the oid"
  [session organization-service organization-oid-handle rights]
  {:pre [(right-seq? rights)]}
  (run-org-authorized
   session
   organization-service
   rights
   (fn [] false)
   #(let [organization-oid (if (instance? clojure.lang.IFn organization-oid-handle)
                             (organization-oid-handle)
                             organization-oid-handle)]
      (contains? % organization-oid))
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
