(ns ataru.util.access-control-utils)

(defn org-oids [session] (map :oid (-> session :identity :organizations)))
