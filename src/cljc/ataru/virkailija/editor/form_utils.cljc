(ns ataru.virkailija.editor.form-utils)

(defn- belongs-to-hakukohde? [field hakutoiveet]
  (not-empty (clojure.set/intersection (set (:belongs-to-hakukohteet field))
                                       hakutoiveet)))

(defn- belongs-to-hakukohderyhma? [field hakutoiveet hakukohteet]
  (let [applied-hakukohderyhmat (->> hakukohteet
                                     (filter #(contains? hakutoiveet (:oid %)))
                                     (mapcat :hakukohderyhmat)
                                     set)]
    (not-empty (clojure.set/intersection (-> field :belongs-to-hakukohderyhma set)
                                         applied-hakukohderyhmat))))

(defn followup-of-answer [{:keys [id followup-of] :as field}  answers]
  (or (not followup-of)
      (= (:value (first (get answers followup-of)))
         (:option-value field))))

(defn visible? [field  answers hakutoiveet hakukohteet]
  (and (not= "infoElement" (:fieldClass field))
       (not (:exclude-from-answers field))
       (or (and (empty? (:belongs-to-hakukohteet field))
                (empty? (:belongs-to-hakukohderyhma field))
                (followup-of-answer field  answers))
           (belongs-to-hakukohde? field hakutoiveet)
           (belongs-to-hakukohderyhma? field hakutoiveet hakukohteet))))

