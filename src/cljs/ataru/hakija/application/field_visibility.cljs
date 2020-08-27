(ns ataru.hakija.application.field-visibility
  (:require [clojure.set :as set]
            [ataru.application-common.option-visibility :as option-visibility]))

(defn- ylioppilastutkinto? [db]
  (boolean (some #(or (= "pohjakoulutus_yo" %)
                      (= "pohjakoulutus_yo_ammatillinen" %)
                      (= "pohjakoulutus_yo_kansainvalinen_suomessa" %)
                      (= "pohjakoulutus_yo_ulkomainen" %))
                 (get-in db [:application :answers :higher-completed-base-education :value]))))

(defn- selected-hakukohteet [db]
  (get-in db [:application :answers :hakukohteet :value]))

(defn selected-hakukohteet-and-ryhmat [db]
  (let [selected-hakukohteet                   (set (selected-hakukohteet db))
        selected-hakukohteet-tarjonta          (when (not-empty selected-hakukohteet)
                                                 (filter #(contains? selected-hakukohteet (:oid %))
                                                         (get-in db [:form :tarjonta :hakukohteet])))
        selected-hakukohderyhmat               (set (mapcat :hakukohderyhmat selected-hakukohteet-tarjonta))
        selected-ei-jyemp-hakukohteet-tarjonta (set (remove :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja?
                                                            selected-hakukohteet-tarjonta))
        selected-ei-jyemp-hakukohderyhmat      (set (mapcat :hakukohderyhmat selected-ei-jyemp-hakukohteet-tarjonta))
        selected-ei-jyemp-hakukohteet          (set (map :oid selected-ei-jyemp-hakukohteet-tarjonta))]
    [(set/union selected-hakukohteet selected-hakukohderyhmat)
     (set/union selected-ei-jyemp-hakukohteet selected-ei-jyemp-hakukohderyhmat)]))

(defn- visible-options [field-descriptor visible? selected-hakukohteet-and-ryhmat]
  (if visible?
    (->> (:options field-descriptor)
         (filter #(or (and (empty? (:belongs-to-hakukohteet %))
                           (empty? (:belongs-to-hakukohderyhma %)))
                      (some selected-hakukohteet-and-ryhmat (:belongs-to-hakukohteet %))
                      (some selected-hakukohteet-and-ryhmat (:belongs-to-hakukohderyhma %))))
         (map :value)
         set)
    #{}))

(defn set-field-visibility
  ([db field-descriptor]
   (let [[visible-fields visible-options]
         (set-field-visibility
          [(transient (:visible-fields db #{}))
           (transient (:visible-options db {}))]
          db
          field-descriptor
          true
          (ylioppilastutkinto? db)
          (selected-hakukohteet-and-ryhmat db))]
     (assoc db
            :visible-fields (persistent! visible-fields)
            :visible-options (persistent! visible-options))))
  ([transients
    field-descriptor
    db
    visible?
    ylioppilastutkinto?
    [selected-hakukohteet-and-ryhmat selected-ei-jyemp-hakukohteet-and-ryhmat]]
   (let [hakukohteet-and-ryhmat [selected-hakukohteet-and-ryhmat selected-ei-jyemp-hakukohteet-and-ryhmat]
         id                     (:id field-descriptor)
         belongs-to             (set (concat (:belongs-to-hakukohderyhma field-descriptor)
                                             (:belongs-to-hakukohteet field-descriptor)))
         jyemp?                 (and ylioppilastutkinto?
                                     (contains? (-> db :application :excluded-attachment-ids-when-yo-and-jyemp) id))
         visible?               (and visible?
                                     (not (get-in field-descriptor [:params :hidden]))
                                     (or (not jyemp?) (not (empty? selected-ei-jyemp-hakukohteet-and-ryhmat)))
                                     (or (not (= "hakukohteet" id)) (some? (get-in db [:form :tarjonta])))
                                     (or (empty? belongs-to)
                                         (not (empty? (set/intersection
                                                       belongs-to
                                                       (if jyemp?
                                                         selected-ei-jyemp-hakukohteet-and-ryhmat
                                                         selected-hakukohteet-and-ryhmat))))))
         answer-value           (get-in db [:application :answers (keyword id) :value])
         visibility-checker     (option-visibility/visibility-checker field-descriptor answer-value)
         child-visibility       (fn [transients]
                                  (reduce #(set-field-visibility %1 %2 db visible? ylioppilastutkinto? hakukohteet-and-ryhmat)
                                          transients
                                          (:children field-descriptor)))
         followup-visibility    (fn [transients]
                                  (reduce (fn [db option]
                                            (let [show-followups? (and visible? (visibility-checker option))]
                                              (reduce #(set-field-visibility %1 %2 db show-followups? ylioppilastutkinto? hakukohteet-and-ryhmat)
                                                      transients
                                                      (:followups option))))
                                          transients
                                          (:options field-descriptor)))
         option-visibility      (fn [[transient-visible-fields transient-visible-options]]
                                  [transient-visible-fields
                                   (assoc! transient-visible-options
                                           id
                                           (visible-options field-descriptor
                                                            visible?
                                                            selected-hakukohteet-and-ryhmat))])
         field-visibility       (fn [[transient-visible-fields transient-visible-options]]
                                  [(if (and visible?
                                            (or (empty? (:children field-descriptor))
                                                (some #(contains? transient-visible-fields (:id %))
                                                      (:children field-descriptor))))
                                     (conj! transient-visible-fields id)
                                     (disj! transient-visible-fields id))
                                   transient-visible-options])]
     (-> transients
         child-visibility
         followup-visibility
         option-visibility
         field-visibility))))
