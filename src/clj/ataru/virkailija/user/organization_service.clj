(ns ataru.virkailija.user.organization-service)

(defn get-all-organizations-as-seq
  "Flattens hierarchy and includes all suborganizations"
  [hierarchy]
        (letfn [(recursively-get-organizations [org-node]
                  (if (< 0 (count (get org-node "children")))
                    (map #(recursively-get-organizations %) (get org-node "children"))
                    {:name (get org-node "nimi") :oid (get org-node "oid")}))]
          (flatten (map #(recursively-get-organizations %) (get hierarchy "organisaatiot")))))
