(ns ataru.organization-service.organization-selection
  (:require [clojure.string :as string]
            [ataru.organization-service.organization-service :as organization-service]
            [medley.core :refer [find-first]]))

(defn- all-organizations
  [organization-service]
  (organization-service/get-all-organizations
   organization-service
   (organization-service/get-hakukohde-groups organization-service)))

(defn query-organization
  [organization-service session query]
  (let [superuser?    (-> session :identity :superuser (boolean))
        organizations (sort-by
                        #(some (fn [lang] (-> % :name lang)) [:fi :sv :en])
                        (if superuser?
                          (all-organizations organization-service)
                          (-> session :identity :organizations (vals))))]
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
  [organization-service session organization-oid rights]
  (if (-> session :identity :superuser)
    (some-> (find-first #(= (:oid %) organization-oid) (all-organizations organization-service))
            (assoc :rights rights))
    (get (-> session :identity :organizations) (keyword organization-oid))))
