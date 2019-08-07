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

(def exluded-attachment-ids-when-yo
  (set ["pohjakoulutus-yo--attachment"
        "pohjakoulutus_kk_ulk--attachement"
        "oppilaitoksen-myontama-ennakkoarvio-arvosanoista"
        "oppilaitoksen-myontama-ennakkoarvio-arvosanoista-2"
        "reifeprufung-dia-tutkintotodistus"
        "reifeprufung-dia-tutkintotodistus-2"
        "reifeprufung-dia-tutkintotodistus-3"
        "reifeprufung-dia-tutkintotodistus-4"
        "candidate-predicted-grades"
        "candidate-predicted-grades-2"
        "oppilaitoksen-myontama-todistus-arvosanoista"
        "oppilaitoksen-myontama-todistus-arvosanoista-2"
        "european-baccalaureate-certificate"
        "european-baccalaureate-certificate-2"
        "european-baccalaureate-certificate-3"
        "european-baccalaureate-certificate-4"
        "diploma-programme"
        "diploma-programme-2"
        "lukion-paattotodistus"
        "pohjakoulutus_ulk--attachment"
        "pohjakoulutus_avoin--attachment"
        "ib-diploma"
        "ib-diploma-2"
        "todistus-muusta-korkeakoulukelpoisuudesta"
        "korkeakoulututkinnon-tutkintotodistus"
        ]))

(defn visible-for-ylioppilas? [field answers]
  (if (get answers :pohjakoulutus_yo)
    (let [id (:id field)]
      (not (get exluded-attachment-ids-when-yo id)))
    true))

(defn visible? [field fields answers hakutoiveet hakukohteet]
  (and (not (get-in field [:params :hidden]))
       (visible-for-ylioppilas? field answers)
       (not= "infoElement" (:fieldClass field))
       (not (:exclude-from-answers field))
       (or (and (empty? (:belongs-to-hakukohteet field))
                (empty? (:belongs-to-hakukohderyhma field)))
           (followup-of-answer field fields answers hakutoiveet hakukohteet)
           (belongs-to-hakukohde? field hakutoiveet)
           (belongs-to-hakukohderyhma? field hakutoiveet hakukohteet))
       (parent-is-visible (:children-of field) fields answers hakutoiveet hakukohteet)))

