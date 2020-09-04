(ns ataru.hakija.application.field-visibility
  (:require [clojure.set :as set]
            [ataru.application.option-visibility :as option-visibility]))

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

(declare set-field-visibility)

(defn- set-visibility-for-option-followups [db options show-option-followups? ylioppilastutkinto? hakukohteet-and-ryhmat]
  (reduce (fn [db option]
            (let [show-followups? (show-option-followups? option)]
              (reduce #(set-field-visibility %1 %2 show-followups? ylioppilastutkinto? hakukohteet-and-ryhmat)
                      db
                      (:followups option))))
          db
          options))

(defn- set-followups-visibility
  [db field-descriptor visible? ylioppilastutkinto? hakukohteet-and-ryhmat]
  (let [answer-value       (get-in db [:application :answers (keyword (:id field-descriptor)) :value])
        visibility-checker (option-visibility/visibility-checker field-descriptor answer-value)
        show-followups?    #(and visible?
                                 (visibility-checker %))]
    (set-visibility-for-option-followups db
                                         (:options field-descriptor)
                                         show-followups?
                                         ylioppilastutkinto?
                                         hakukohteet-and-ryhmat)))

(defn- set-option-visibility [db [index option] visible? id selected-hakukohteet-and-ryhmat]
  (let [belongs-to (set (concat (:belongs-to-hakukohderyhma option)
                                (:belongs-to-hakukohteet option)))]
    (assoc-in db [:application :ui id index :visible?]
              (boolean
                (and visible?
                     (or (empty? belongs-to)
                         (not (empty? (set/intersection
                                        belongs-to
                                        selected-hakukohteet-and-ryhmat)))))))))

(defn set-field-visibility
  ([db field-descriptor]
   (set-field-visibility
     db
     field-descriptor
     true
     (ylioppilastutkinto? db)
     (selected-hakukohteet-and-ryhmat db)))
  ([db
    field-descriptor
    visible?
    ylioppilastutkinto?
    [selected-hakukohteet-and-ryhmat selected-ei-jyemp-hakukohteet-and-ryhmat]]
   (let [hakukohteet-and-ryhmat [selected-hakukohteet-and-ryhmat selected-ei-jyemp-hakukohteet-and-ryhmat]
         id                     (keyword (:id field-descriptor))
         belongs-to             (set (concat (:belongs-to-hakukohderyhma field-descriptor)
                                             (:belongs-to-hakukohteet field-descriptor)))
         excluded-attachment-ids-when-yo-and-jyemp (-> db :application :excluded-attachment-ids-when-yo-and-jyemp)
         jyemp?                 (and ylioppilastutkinto?
                                     (contains? excluded-attachment-ids-when-yo-and-jyemp (:id field-descriptor)))
         visible?               (and (not (get-in field-descriptor [:params :hidden]))
                                     visible?
                                     (or (not jyemp?) (not (empty? selected-ei-jyemp-hakukohteet-and-ryhmat)))
                                     (or (empty? belongs-to)
                                         (not (empty? (set/intersection
                                                        belongs-to
                                                        (if jyemp?
                                                          selected-ei-jyemp-hakukohteet-and-ryhmat
                                                          selected-hakukohteet-and-ryhmat)))))
                                     (or (not (= :hakukohteet id)) (some? (get-in db [:form :tarjonta]))))
         child-visibility       (fn [db]
                                  (reduce #(set-field-visibility %1 %2 visible? ylioppilastutkinto? hakukohteet-and-ryhmat)
                                          db
                                          (:children field-descriptor)))
         option-visibility      (fn [db]
                                  (reduce #(set-option-visibility %1 %2 visible? id selected-hakukohteet-and-ryhmat)
                                          db
                                          (map-indexed vector (:options field-descriptor))))
         field-visibility       (fn [db]
                                  (assoc-in db
                                            [:application :ui id :visible?]
                                            (boolean
                                              (and visible?
                                                   (or (empty? (:children field-descriptor))
                                                       (some #(get-in db [:application :ui (keyword (:id %)) :visible?])
                                                             (:children field-descriptor)))))))]
     (cond-> (-> db
                 child-visibility
                 option-visibility
                 field-visibility)
             (#{"dropdown" "multipleChoice" "singleChoice" "textField"} (:fieldType field-descriptor))
             (set-followups-visibility field-descriptor visible? ylioppilastutkinto? hakukohteet-and-ryhmat)))))
