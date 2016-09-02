(ns ataru.virkailija.user.organization-service
  (:require
   [oph.soresu.common.config :refer [config]]
   [cheshire.core :as json]
   [ataru.cas.client :as cas-client]))

(defn read-body
  [resp]
  (-> resp :body slurp (json/parse-string true)))

(defn get-all-organizations-as-seq
  "Flattens hierarchy and includes all suborganizations"
  [hierarchy]
  (letfn [(recursively-get-organizations [org-node]
            (if (< 0 (count (:children org-node)))
              (map #(recursively-get-organizations %) (:children org-node))
              {:name (:nimi org-node) :oid (:oid org-node)}))]
    (flatten (map #(recursively-get-organizations %) (:organisaatiot hierarchy)))))

(defn create []
  {:base-address (get-in config [:organization-service :base-address])})

(defn get-organizations
  "Returns a sequence of {:name <org-name> :oid <org-oid>} maps containing all suborganizations"
  [this root-organization-oid]
  {:pre [(some? (:base-address this))]}
  (let [cas-client (:cas-client this)
        response (cas-client/cas-authenticated-get cas-client
                                                   (str (:base-address this)
                                                        "/hierarkia/hae/nimi?aktiiviset=true&suunnitellut=true&lakkautetut=false&skipParents=true&oid="
                                                        root-organization-oid) )]
    (if (= 200 (:status response))
      (-> response read-body get-all-organizations-as-seq)
      (throw (Exception. (str "Got status code " (:status response) " While reading organizations"))))))
