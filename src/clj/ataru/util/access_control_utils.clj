(ns ataru.util.access-control-utils
  (:import [clojure.lang IFn]))

(defn org-oids [session] (map :oid (-> session :identity :organizations)))

(defn all-org-oids [organization-service organization-oids]
  (let [all-organizations (.get-all-organizations organization-service organization-oids)]
    (map :oid all-organizations)))

(defn organization-allowed?
  "Parameter organization-oid-handle can be either the oid value or a function which returns the oid"
  [session organization-service organization-oid-handle]
  (let [organization-oids (org-oids session)]
    (cond
      (some #{oph-organization} organization-oids)
      true

      (empty? organization-oids)
      false

      :else
      (let [organization-oid (if (instance? IFn organization-oid-handle)
                               (organization-oid-handle)
                               organization-oid-handle)]
        (-> #{organization-oid}
            (some (all-org-oids organization-service organization-oids))
            boolean)))))
