(ns ataru.util.access-control-utils
  (:require [ataru.virkailija.user.organization-client :as organization-client])
  (:import [clojure.lang IFn]))

(defn organizations [session] (-> session :identity :organizations))
(defn org-oids [session] (map :oid (organizations session)))

(defn all-org-oids [organization-service organizations]
  (let [all-organizations (.get-all-organizations organization-service organizations)]
        (map :oid all-organizations)))

(defn organization-allowed?
  "Parameter organization-oid-handle can be either the oid value or a function which returns the oid"
  [session organization-service organization-oid-handle]
  (let [organizations (organizations session)]
    (cond
      (some #{organization-client/oph-organization} (map :oid organizations))
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
