(ns ataru.virkailija.user.organization-client
  (:require
   [oph.soresu.common.config :refer [config]]
   [cheshire.core :as json]
   [ataru.cas.client :as cas-client]))

(def oph-organization "1.2.246.562.10.00000000001")

(def
  plain-org-hierarchy-path
  "/hierarkia/hae/nimi?aktiiviset=true&suunnitellut=true&lakkautetut=false&skipParents=true&oid=")

(def org-name-path "/hae/nimi?aktiiviset=true&suunnitellut=true&lakkautetut=false&oid=")

(defn read-body
  [resp]
  (-> resp :body (json/parse-string true)))

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

(defn base-address [] (get-in config [:organization-service :base-address]))

(defn get-organization-from-remote-service [cas-client organization-oid]
  {:pre [(some? (base-address))]}
  (let [response (cas-client/cas-authenticated-get cas-client
                                                   (str (base-address)
                                                        org-name-path
                                                        organization-oid))]
    (if (= 200 (:status response))
      (let [parsed-response (read-body response)
            org-count (:numHits parsed-response)]
        (cond
          (< 1 org-count)
          ; Should not happen ever, but let's make the failure very explicit and do some moves if it actually occurs:
          (throw (Exception. (str "Got wrong number of organizations for unique oid query " org-count)))

          (= 0 org-count)
          nil

          :else
          (org-node->map (first (:organisaatiot parsed-response)))))
      (throw (Exception. (str "Got status code " (:status response) " While reading single organization"))))))

(defn get-organization [cas-client organization-oid]
  {:pre [(some? (base-address))]}
  (if (= organization-oid oph-organization)
    ;; the remote organization service  (organisaatiopalvelu) doesn't support
    ;; fetching data about the root OPH organization, so we'll hard-code it here:
    {:oid oph-organization :name {:fi "OPH"}}
    (get-organization-from-remote-service cas-client organization-oid)))

(defn get-organizations
  "Returns a sequence of {:name <org-name> :oid <org-oid>} maps containing all suborganizations
   The root organization is the first element"
  [cas-client root-organization-oid]
  {:pre [(some? (base-address))]}
  (let [response (cas-client/cas-authenticated-get cas-client
                                                   (str (base-address)
                                                        plain-org-hierarchy-path
                                                        root-organization-oid) )]
    (if (= 200 (:status response))
      (-> response read-body get-all-organizations-as-seq)
      (throw (Exception. (str "Got status code " (:status response) " While reading organizations"))))))
