(ns ataru.virkailija.user.organization-client
  (:require
   [oph.soresu.common.config :refer [config]]
   [cheshire.core :as json]
   [ataru.cas.client :as cas-client]))

(def
  plain-org-hierarchy-path
  "/hierarkia/hae/nimi?aktiiviset=true&suunnitellut=true&lakkautetut=false&skipParents=true&oid=")

(defn read-body
  [resp]
  (-> resp :body slurp (json/parse-string true)))

(defn- org-node->map [org-node] {:name (:nimi org-node) :oid (:oid org-node)})

(defn get-all-organizations-as-seq
  "Flattens hierarchy and includes all suborganizations"
  [hierarchy]
  (letfn [(recur-orgs [org-node]
            (if (< 0 (count (:children org-node)))
              (into [(org-node->map org-node)]
                    (map #(recur-orgs %) (:children org-node)))
              (org-node->map org-node)))]
    (flatten (map #(recur-orgs %) (:organisaatiot hierarchy)))))

(defn create []
  {:base-address (get-in config [:organization-service :base-address])})

(defn get-organizations
  "Returns a sequence of {:name <org-name> :oid <org-oid>} maps containing all suborganizations
   The root organization is the first element"
  [this root-organization-oid]
  {:pre [(some? (:base-address this))]}
  (let [cas-client (:cas-client this)
        response (cas-client/cas-authenticated-get cas-client
                                                   (str (:base-address this)
                                                        plain-org-hierarchy-path
                                                        root-organization-oid) )]
    (if (= 200 (:status response))
      (-> response read-body get-all-organizations-as-seq)
      (throw (Exception. (str "Got status code " (:status response) " While reading organizations"))))))
