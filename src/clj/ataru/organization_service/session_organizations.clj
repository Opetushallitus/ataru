(ns ataru.organization-service.session-organizations
  (:require
    [schema.core :as s]
    [ataru.user-rights :refer [Right]]
    [clojure.set :refer [intersection]]
    [ataru.organization-service.organization-service :as organization-service]))

(defn- right-organizations
  [session]
  (-> session :identity :user-right-organizations))

(defn- filter-orgs-for-rights
  [session rights organizations]
  (if (-> session :identity :superuser)
    organizations
    (filter
      (fn [org]
        (-> (-> session :identity :organizations)
            (get (keyword (:oid org)))
            :rights
            (set)
            (intersection (set (map name rights)))
            (not-empty)))
      organizations)))

(defn right-seq? [val] (s/validate [Right] val))

(defn select-organizations-for-rights [organization-service session rights]
  {:pre [(right-seq? rights)]}
  (if-let [selected-organization (:selected-organization session)]
    (when-not (and (-> session :identity :superuser)
                   (empty? (intersection (set rights)
                                         (->> selected-organization :rights (map keyword) set))))
      (filter-orgs-for-rights
        session
        rights
        (organization-service/get-all-organizations organization-service [selected-organization])))
    (let [right-orgs (right-organizations session)]
      (->> rights
           (map #(get right-orgs %))
           (remove nil?)
           (flatten)
           (distinct)
           (organization-service/get-all-organizations organization-service)))))

(defn run-org-authorized [session
                          organization-service
                          rights
                          when-no-orgs-fn
                          when-ordinary-user-fn
                          when-superuser-fn]
  {:pre [(right-seq? rights)]}
  (let [organizations         (select-organizations-for-rights organization-service session rights)
        superuser?            (-> session :identity :superuser)
        organization-oids     (set (map :oid organizations))
        selected-organization (:selected-organization session)]
    (cond
      (and
        (or (not superuser?)
            (some? selected-organization))
        (empty? organizations))
      (when-no-orgs-fn)

      (or
        (some? selected-organization)
        (not superuser?))
      (when-ordinary-user-fn organization-oids)

      superuser?
      (when-superuser-fn))))

(defn organization-allowed?
  "Parameter organization-oid-handle can be either the oid value or a function which returns the oid"
  [session organization-service organization-oid-fn rights]
  {:pre [(right-seq? rights)]}
  (run-org-authorized
    session
    organization-service
    rights
    (fn [] false)
    (fn [organizations] (contains? organizations (organization-oid-fn)))
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
