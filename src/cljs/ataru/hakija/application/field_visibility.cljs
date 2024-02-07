(ns ataru.hakija.application.field-visibility
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [ataru.application.option-visibility :as option-visibility]
            [ataru.util :as u]))

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

(defn- belongs-to [field-descriptor]
  (set (concat (:belongs-to-hakukohderyhma field-descriptor)
               (:belongs-to-hakukohteet field-descriptor))))

(defn- field-belongs-to [field-descriptor [selected-hakukohteet-and-ryhmat selected-ei-jyemp-hakukohteet-and-ryhmat] jyemp?]
  (let [belongs-to (belongs-to field-descriptor)]
    (when (not (empty? belongs-to))
      (not (empty? (set/intersection
                     belongs-to
                     (if jyemp?
                       selected-ei-jyemp-hakukohteet-and-ryhmat
                       selected-hakukohteet-and-ryhmat)))))))

(defn- jyemp? [ylioppilastutkinto? db field-descriptor]
  (let [excluded-attachment-ids-when-yo-and-jyemp (get-in db [:application :excluded-attachment-ids-when-yo-and-jyemp])]
    (and ylioppilastutkinto?
         (contains? excluded-attachment-ids-when-yo-and-jyemp (:id field-descriptor)))))

(defn- nested-visilibity-inner [db {:keys [children options] :as field} visible? hakukohteet-and-ryhmat]
  (let [id (-> field :id keyword)
        belongs-to-fn (fn []
                        (->> (jyemp? (ylioppilastutkinto? db) db field)
                             (field-belongs-to field hakukohteet-and-ryhmat)))
        visible? (and visible?
                      (case (belongs-to-fn)
                        nil visible?
                        true visible?
                        false false))
        reduce-fn (fn [db child] (nested-visilibity-inner db child visible? hakukohteet-and-ryhmat))]
    (as-> db db'
          (assoc-in db' [:application :ui id :visible?] visible?)
          (reduce reduce-fn db' (mapcat :followups options))
          (reduce reduce-fn db' children))))

(defn set-nested-visibility ([db id visible?]
                             (set-nested-visibility db id visible? (selected-hakukohteet-and-ryhmat db)))
  ([db id visible? hakukohteet-and-ryhmat]
   (nested-visilibity-inner
     db
     (u/find-field (get-in db [:form :content]) id)
     visible?
     hakukohteet-and-ryhmat)))

(declare set-field-visibility)

(defn- set-followup-visibility [db field-descriptor show-followups? show-conditional-followups-fn
                                ylioppilastutkinto? applies-as-identified? hakukohteet-and-ryhmat]
  (let [field-id (-> field-descriptor :id keyword)
        value (get-in db [:application :answers field-id :value])
        fields-by-id (u/form-sections-by-id-memo (:form db))
        remove-fn (fn [condition]
                    (when show-followups?
                      (or
                        (string/blank? value)
                        (option-visibility/non-blank-answer-satisfies-condition? value condition))))
        conditional-sections (->> (:section-visibility-conditions field-descriptor)
                                  (remove remove-fn)
                                  (map (comp keyword :section-name))
                                  (keep (partial get fields-by-id)))]
    (as-> db db'
          (set-field-visibility db' field-descriptor show-followups? ylioppilastutkinto? applies-as-identified? hakukohteet-and-ryhmat)
          (reduce #(set-nested-visibility %1 (:id %2) (show-conditional-followups-fn show-followups? %2) hakukohteet-and-ryhmat)
                  db'
                  conditional-sections))))

(defn- set-visibility-for-option-followups [db options show-followups-fn show-conditional-followups-fn
                                            ylioppilastutkinto? applies-as-identified? hakukohteet-and-ryhmat]
  (reduce (fn [db option]
            (let [show-followups? (show-followups-fn option)]
              (reduce #(set-followup-visibility %1 %2 show-followups?
                                                show-conditional-followups-fn ylioppilastutkinto?
                                                applies-as-identified? hakukohteet-and-ryhmat)
                      db
                      (:followups option))))
          db
          options))

(defn- applies-as-identified? [db]
  (get-in db [:oppija-session :logged-in] false))

(defn- set-followups-visibility
  [db field-descriptor visible? ylioppilastutkinto? applies-as-identified? hakukohteet-and-ryhmat]
  (let [component-visibility (atom {})
        answer-value (get-in db [:application :answers (keyword (:id field-descriptor)) :value])
        visibility-checker (option-visibility/visibility-checker field-descriptor answer-value)
        show-followups-fn #(and visible?
                                (visibility-checker %))
        show-conditional-followups-fn (fn [show? field-descriptor]
                                        (let [id (:id field-descriptor)
                                              should-show? (or show? (get @component-visibility id false))]
                                          (swap! component-visibility assoc id should-show?)
                                          should-show?))]
    (set-visibility-for-option-followups db
                                         (:options field-descriptor)
                                         show-followups-fn
                                         show-conditional-followups-fn
                                         ylioppilastutkinto?
                                         applies-as-identified?
                                         hakukohteet-and-ryhmat)))

(defn- set-option-visibility [db [index option] parent-visible? id selected-hakukohteet-and-ryhmat]
  (let [belongs-to (set (concat (:belongs-to-hakukohderyhma option)
                                (:belongs-to-hakukohteet option)))]
    (assoc-in db [:application :ui id index :visible?]
              (boolean
                (and (and parent-visible? (not (:hidden option)))
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
     (applies-as-identified? db)
     (selected-hakukohteet-and-ryhmat db)))
  ([db
    field-descriptor
    visible?
    ylioppilastutkinto?
    applies-as-identified?
    [selected-hakukohteet-and-ryhmat selected-ei-jyemp-hakukohteet-and-ryhmat]]
   (let [hakukohteet-and-ryhmat [selected-hakukohteet-and-ryhmat selected-ei-jyemp-hakukohteet-and-ryhmat]
         id                     (keyword (:id field-descriptor))
         belongs-to             (belongs-to field-descriptor)
         jyemp?                 (jyemp? ylioppilastutkinto? db field-descriptor)
         form                   (:form db)
         answers                (get-in db [:application :answers])
         visible?               (and (not (or (get-in field-descriptor [:params :hidden])
                                              (get-in field-descriptor [:hidden])))
                                     (not (and (get-in field-descriptor [:params :show-for-identified])
                                               (not applies-as-identified?)))
                                     visible?
                                     (or (not jyemp?) (not (empty? selected-ei-jyemp-hakukohteet-and-ryhmat)))
                                     (or (empty? belongs-to)
                                         (not (empty? (set/intersection
                                                        belongs-to
                                                        (if jyemp?
                                                          selected-ei-jyemp-hakukohteet-and-ryhmat
                                                          selected-hakukohteet-and-ryhmat)))))
                                     (or (not (= :hakukohteet id)) (some? (get-in db [:form :tarjonta])))
                                     (not (u/is-field-hidden-by-section-visibility-conditions form answers field-descriptor)))
         child-visibility       (fn [db]
                                  (reduce #(set-field-visibility %1 %2 visible? ylioppilastutkinto? applies-as-identified? hakukohteet-and-ryhmat)
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
             (set-followups-visibility field-descriptor visible? ylioppilastutkinto? applies-as-identified? hakukohteet-and-ryhmat)))))
