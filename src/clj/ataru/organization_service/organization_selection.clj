(ns ataru.organization-service.organization-selection
  (:require [clojure.string :as string]
            [ataru.organization-service.organization-service :as organization-service]
            [medley.core :refer [find-first]]))

(defn- organization-list
  [organization-service]
  (organization-service/get-all-organizations organization-service [{:oid ""}]))

(defn query-organization
  [organization-service session query]
  (let [superuser? (-> session :identity :superuser (boolean))
        organizations (if superuser?
                        (organization-list organization-service)
                        (-> session :organizations (vals)))]
    (take 11
          (if (or (string/blank? query)
                  (< (count query) 2))
            organizations
            (let [query-parts (map string/lower-case (string/split query #"\s+"))]
              (->> organizations
                   (filter
                     (fn [organization]
                       (let [haystack (string/lower-case
                                        (str (get-in organization [:name :fi] "")
                                             (get-in organization [:name :sv] "")
                                             (get-in organization [:name :en] "")))]
                         (every? #(string/includes? haystack %) query-parts))))))))))

(defn select-organization
  [organization-service session organization-oid]
  (if (-> session :identity :superuser)
    (find-first #(= (:oid %) organization-oid) (organization-list organization-service))
    (get (-> session :identity :organizations) (keyword organization-oid))))
