(ns ataru.fixtures.hakukohde)

(defn field-max-hakukohteet
  [num-max-hakukohteet values]
  {:params {:max-hakukohteet num-max-hakukohteet} :options (map (fn [v] {:value v}) values)})

(def hakukohteet
  ; [hakukohteet form-descriptor valid?]
  [[nil nil true]
   [["abc"] (field-max-hakukohteet 2 ["abc" "def" "ghi"]) true]
   [["abc" "def"] (field-max-hakukohteet 2 ["abc" "def" "ghi"]) true]
   [["abc" "def" "ghi"] (field-max-hakukohteet 2 ["abc" "def" "ghi"]) false]
   [[] (field-max-hakukohteet 2 ["abc" "def" "ghi"]) false]
   [nil (field-max-hakukohteet 2 ["abc" "def" "ghi"]) false]
   [["abc" "jkl"] (field-max-hakukohteet 2 ["abc" "def" "ghi"]) false]])
