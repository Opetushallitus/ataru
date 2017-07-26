(ns ataru.fixtures.hakukohde)

(defn field-max-hakukohteet
  [n]
  {:params {:max-hakukohteet n}})

(def hakukohteet
  ; [hakukohteet form-descriptor valid?]
  [[nil nil true]
   [["abc"] (field-max-hakukohteet 2) true]
   [["abc" "def"] (field-max-hakukohteet 2) true]
   [["abc" "def" "ghi"] (field-max-hakukohteet 2) false]
   [[] (field-max-hakukohteet 2) false]
   [nil (field-max-hakukohteet 2) false]])
