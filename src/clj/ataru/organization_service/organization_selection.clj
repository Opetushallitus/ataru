(ns ataru.organization-service.organization-selection
  (:require [clojure.string :as string]))

(defn query-organization
  [organizations query]
  ; TODO if oph organization, organizations = all
  (if (or (string/blank? query)
          (< (count query) 2))
    organizations
    (let [query-parts (map string/lower-case (string/split query #"\s+"))]
      (->> organizations
           (filter
             (fn [[_ organization]]
               (let [haystack (string/lower-case
                                (str (get-in organization [:name :fi] "")
                                     (get-in organization [:name :sv] "")
                                     (get-in organization [:name :en] "")))]
                 (every? #(string/includes? haystack %) query-parts))))
           (into {})))))

(defn select-organization
  [organizations organization-oid]
  (get organizations (keyword organization-oid)))
