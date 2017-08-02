(ns ataru.fixtures.hakukohde)

(defn field-max-hakukohteet
  [num-max-hakukohteet num-options]
  {:params {:max-hakukohteet num-max-hakukohteet} :options (repeat num-options {:value "foo"})})

(def hakukohteet
  ; [hakukohteet form-descriptor valid?]
  [[nil nil true]
   [["abc"] (field-max-hakukohteet 2 3) true]
   [["abc" "def"] (field-max-hakukohteet 2 3) true]
   [["abc" "def" "ghi"] (field-max-hakukohteet 2 3) false]
   [[] (field-max-hakukohteet 2 3) false]
   [nil (field-max-hakukohteet 2 3) false]])
