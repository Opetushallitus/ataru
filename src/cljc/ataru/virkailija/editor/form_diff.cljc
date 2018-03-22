(ns ataru.virkailija.editor.form-diff)

(defn- index-of-id [id form]
  (when id
    (->> (:content form)
         (keep-indexed (fn [index e]
                         (when (= id (:id e))
                           index)))
         first)))

(defn- index-of-element [element form]
  (index-of-id (:id element) form))

(defn is-updated-in [old-element element]
  (and old-element
       (not (= old-element element))))

(defn find-element [id form]
  (first (filter #(= (:id %) id) (:content form))))

(defn find-elements [ids form]
  (filter #(contains? ids (:id %)) (:content form)))

(defn find-updated-elements [old-form form]
  (filter #(is-updated-in (find-element (:id %) old-form) %) (:content form)))

(defn insert-adjacent-elements-between [adjacent-elements siblings form]
  (let [content (:content form)
        [above below] (doall (map #(index-of-id % form) siblings))]
    (if (or
         (and (not above) (first siblings))
         (and (not below) (second siblings)))
      (throw (ex-info "Sibling with index not found! Mismatching forms!" {})))
    (cond
     (and above
          (not below)) (assoc form :content (concat content adjacent-elements))
     (and (not above)
          below) (assoc form :content (concat adjacent-elements content))
     (and (not above)
          (not below)) (assoc form :content adjacent-elements)
     (and above
          below) (let [adjacent? (= 1 (- above below))]
                   (if adjacent?
                     (let [[elements-above elements-below] (split-at above content)]
                       (assoc form :content (concat elements-above adjacent-elements elements-below)))
                     (throw (ex-info "Mismatching forms! User should update view." {})))))))

(defn find-element-siblings [form element]
  (let [[before starting-with] (slit-with #(not= (:id element) (:id %))
                                          (:content form))]
    (when (not-empty starting-with)
      [(last before) (first starting-with) (second starting-with)])))

(defn- element-with-new-siblings [element old-form form]
  (let [old-siblings (find-element-siblings old-form element)
        new-siblings (find-element-siblings form element)]
    (when-not (= (map :id old-siblings) (map :id new-siblings)) new-siblings)))

(defn find-elements-with-new-siblings
  [old-form form]
  (keep #(element-with-new-siblings % old-form form) (:content form)))

(defn find-missing-elements
  [old-form form]
  (let [new-ids (set (map :id (:content form)))
        old-ids (set (map :id (:content old-form)))
        keys-in-old (clojure.set/difference old-ids new-ids)]
    (filter #(contains? keys-in-old (:id %)) (:content old-form))))

(defn as-delete-operation [element]
  {:type "delete"
   :element element})

(defn- connect-new-adjacent
  [adjacent]
  (fn
    ([]
     adjacent)
    ([new-adjacent]
     (let [first-id (get-in (first adjacent) [:element :id])
           last-id  (get-in (last adjacent) [:element :id])
           below-id (:sibling-below new-adjacent)
           above-id (:sibling-above new-adjacent)]
       (cond
        (= first-id below-id) (concat  [new-adjacent] adjacent)
        (= last-id above-id) (concat adjacent [new-adjacent] ))))
    ([first-new-adjacent second-new-adjacent]
     (-> (connect-new-adjacent (-> (connect-new-adjacent adjacent)
                                   first-new-adjacent)) second-new-adjacent))))

(defn adjacent-for-create-move-element [cm-element cm-elements]
  (loop [adjacent     [cm-element]
         not-adjacent cm-elements]
    (let [ids          (set (map #(get-in % [:element :id]) adjacent))
          adjacent?    (fn [e]
                         (if (seq (clojure.set/intersection ids #{(:sibling-above e) (:sibling-below e)}))
                           :adjacent
                           :not-adjacent))
          {some-new-adjacent :adjacent
           new-not-adjacent :not-adjacent} (group-by adjacent? not-adjacent)
          new-adjacent (apply (connect-new-adjacent adjacent) some-new-adjacent)]
      (if (and (seq some-new-adjacent)
               (seq new-not-adjacent))
        (recur new-adjacent new-not-adjacent)
        [new-adjacent new-not-adjacent]))))

(defn partition-by-adjacent-elements
  ([create-move-elements]
   (partition-by-adjacent-elements create-move-elements []))
  ([create-move-elements partitioned]
   (let [[head & tail] create-move-elements]
     (if-let [cm-element head]
       (let [[adjacent not-adjacent] (adjacent-for-create-move-element cm-element tail)]
         (partition-by-adjacent-elements not-adjacent (cons adjacent partitioned)))
       partitioned))))

(defn- as-create-move-element [[above element below]]
  {:sibling-above (some-> above :id)
   :sibling-below (some-> below :id)
   :element       element})

(defn as-create-move-group-operation [elements-with-new-siblings]
  {:type  "create-move-group"
   :group (map as-create-move-element elements-with-new-siblings)})

(defn as-update-operation [old-form element]
  {:type        "update"
   :old-element (find-element (:id element) old-form)
   :new-element element})

(defn as-rename-operation [old-form form]
  {:type        "rename-form"
   :old-name (:name old-form)
   :new-name (:name form)})

(defn as-operations [old-form form]
  (remove nil?
    (flatten [(if (= (:name old-form) (:name form)) nil (as-rename-operation old-form form))
              (map #(as-update-operation old-form %) (find-updated-elements old-form form))
              (map #(as-delete-operation %) (find-missing-elements old-form form))
              (when-let [elements-with-new-siblings (seq (find-elements-with-new-siblings old-form form))]
                (as-create-move-group-operation elements-with-new-siblings))])))

(defn replace-element [element form]
  (update form :content (partial map #(if (= (:id element) (:id %)) element %))))

(defn remove-element [form element]
  (update form :content (partial remove #(= (:id element) (:id %)))))

(defn remove-elements [form elements]
  (let [ids (set (map :id elements))]
    (assoc form :content (remove #(contains? ids (:id %)) (:content form)))))

(defn- apply-update [latest-form update]
  (let [id (get-in update [:new-element :id])
        old-element (:old-element update)
        new-element (:new-element update)
        latest-element (find-element id latest-form)]
    (if (= old-element latest-element)
      (replace-element new-element latest-form)
      (throw (ex-info (str "Update on modified element " id " is disallowed!") {})))))

(defn- replace-with-existing-element [create-move-element existing-elements]
  (if-let [existing-element (first (filter #(= (:id %) (get-in create-move-element [:element :id])) existing-elements))]
    (assoc create-move-element :element existing-element)
    create-move-element))

(defn- apply-adjacent-create-move-elements [detached-form adjacent-create-move-elements]
  (let [[head & tail] adjacent-create-move-elements]
    (if head
      (let [adjacent-elements (map :element head)
            sibling-above (:sibling-above (first head))
            sibling-below (:sibling-below (last head))]
        (apply-adjacent-create-move-elements (insert-adjacent-elements-between adjacent-elements
                                               [sibling-above sibling-below] detached-form) tail))
      detached-form)))

(defn- apply-create-move-group [latest-form create-move-group]
  (let [element-ids (set (map #(get-in % [:element :id]) (:group create-move-group)))
        existing-elements (find-elements element-ids latest-form)
        create-moves-with-existing-elements (map #(replace-with-existing-element % existing-elements) (:group create-move-group))
        adjacent-create-move-elements (partition-by-adjacent-elements create-moves-with-existing-elements)
        detached-form (remove-elements latest-form existing-elements)]
    (apply-adjacent-create-move-elements detached-form adjacent-create-move-elements)))

(defn- apply-delete [latest-form delete]
  (let [id (get-in delete [:element :id])
        removed-element (:element delete)
        latest-element (find-element id latest-form)]
    (if (= removed-element latest-element)
      (remove-element latest-form latest-element)
      (throw (ex-info (str "Deleting modified element " id " is disallowed!") {})))))

(defn- apply-rename-form [latest-form rename]
  (let [old-name (:old-name rename)
        new-name (:new-name rename)
        current-name (:name latest-form)]
    (if (= old-name current-name)
      (assoc latest-form :name new-name)
      (throw (ex-info "Renaming modified name is disallowed!" {})))))

(defn- apply-operation [latest-form operation]
  (condp = (:type operation)
         "update" (apply-update latest-form operation)
         "create-move-group" (apply-create-move-group latest-form operation)
         "delete" (apply-delete latest-form operation)
         "rename-form" (apply-rename-form latest-form operation)
         latest-form))

(defn apply-operations [latest-form operations]
  (reduce apply-operation latest-form operations))
