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

(declare visible?)

(defn- parent-is-visible [children-of fields answers hakutoiveet hakukohteet]
  (or (not children-of)
      (visible? (get fields children-of) fields answers hakutoiveet hakukohteet)))

(defn- followup-of-answer [{:keys [id followup-of] :as field} fields answers hakutoiveet hakukohteet]
  (or (not followup-of)
      (and (= (:value (first (get answers followup-of)))
              (:option-value field))
           (parent-is-visible followup-of fields answers hakutoiveet hakukohteet))))

(defn visible? [field fields answers hakutoiveet hakukohteet]
  (and (not (get-in field [:params :hidden]))
       (not= "infoElement" (:fieldClass field))
       (not= "modalInfoElement" (:fieldClass field))
       (not (:exclude-from-answers field))
       (or (and (empty? (:belongs-to-hakukohteet field))
                (empty? (:belongs-to-hakukohderyhma field)))
           (followup-of-answer field fields answers hakutoiveet hakukohteet)
           (belongs-to-hakukohde? field hakutoiveet)
           (belongs-to-hakukohderyhma? field hakutoiveet hakukohteet))
       (parent-is-visible (:children-of field) fields answers hakutoiveet hakukohteet)))

