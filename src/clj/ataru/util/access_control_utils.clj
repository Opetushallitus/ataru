(ns ataru.util.access-control-utils)

(defn org-oids [session] (map :oid (-> session :identity :organizations)))

(defn all-org-oids [organization-service organization-oids]
  (let [all-organizations (.get-all-organizations organization-service organization-oids)]
    (map :oid all-organizations)))
