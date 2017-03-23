(ns ataru.virkailija.user.organization-client
  (:require
    [ataru.config.url-helper :refer [resolve-url]]
    [ataru.config.core :refer [config]]
    [cheshire.core :as json]
    [ataru.cas.client :as cas-client]))

(def oph-organization "1.2.246.562.10.00000000001")

(defn read-body
  [resp]
  (-> resp :body (json/parse-string true)))

(defn- org-node->map [org-node] {:name (:nimi org-node)
                                 :oid (:oid org-node)
                                 :type :organization})

(defn- group->map [group] {:name (:nimi group)
                                 :oid (:oid group)
                                 :type :group})

(defn get-all-organizations-as-seq
  "Flattens hierarchy and includes all suborganizations"
  [hierarchy]
  (letfn [(recur-orgs [org-node]
            (if (< 0 (count (:children org-node)))
              (into [(org-node->map org-node)]
                    (map #(recur-orgs %) (:children org-node)))
              (org-node->map org-node)))]
    (flatten (map #(recur-orgs %) (:organisaatiot hierarchy)))))

(defn get-organization-from-remote-service [cas-client organization-oid]
  (let [response (cas-client/cas-authenticated-get
                   cas-client
                   (resolve-url :organization-service.name organization-oid))]
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
  (if (= organization-oid oph-organization)
    ;; the remote organization service  (organisaatiopalvelu) doesn't support
    ;; fetching data about the root OPH organization, so we'll hard-code it here:
    {:oid oph-organization :name {:fi "OPH"} :type :organization}
    (get-organization-from-remote-service cas-client organization-oid)))

(defn get-organizations
  "Returns a sequence of {:name <org-name> :oid <org-oid>} maps containing all suborganizations
   The root organization is the first element"
  [cas-client root-organization-oid]
  (let [response (cas-client/cas-authenticated-get
                   cas-client
                   (resolve-url :organization-service.plain-hierarchy root-organization-oid))]
    (if (= 200 (:status response))
      (-> response read-body get-all-organizations-as-seq)
      (throw (Exception. (str "Got status code " (:status response) " While reading organizations"))))))

(defn get-groups
  "returns a sequence of {:name <group-name> :oid <group-oid>} maps containing all the
   groups within organization service"
  [cas-client]
  (let [response (cas-client/cas-authenticated-get cas-client (resolve-url :organization-service.groups))]
    (if (= 200 (:status response))
      (->> response read-body (map group->map))
      (throw (Exception. (str "Got status code " (:status response) " While reading groups"))))))
