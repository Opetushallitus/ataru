(ns ataru.organization-service.organization-selection
  (:require [clojure.string :as string]
            [ataru.organization-service.organization-service :as organization-service]
            [medley.core :refer [find-first]]))

(defn- all-organizations
  [organization-service]
  (organization-service/get-all-organizations
    organization-service
    (into [{:oid ""}] (organization-service/get-hakukohde-groups organization-service))))

(defn- filter-org-by-type
  [include-organizations? include-hakukohde-groups? organization]
  (cond
    (and include-organizations? include-hakukohde-groups?)
    true
    (and (not include-organizations?) (not include-hakukohde-groups?))
    false
    :else
    (= (-> organization :hakukohderyhma? boolean) include-hakukohde-groups?)))

(defn query-organization
  [organization-service session query include-organizations? include-hakukohde-groups? page-num]
  (let [page-size 20
        superuser?    (-> session :identity :superuser (boolean))
        organizations (->> (if superuser?
                             (all-organizations organization-service)
                             (-> session :identity :organizations (vals)))
                           (filter (partial filter-org-by-type include-organizations? include-hakukohde-groups?))
                           (sort-by #(some (fn [lang] (ataru.util/not-blank? (-> % :name lang))) [:fi :sv :en])))]
    (take (inc (* page-size (inc page-num)))
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
