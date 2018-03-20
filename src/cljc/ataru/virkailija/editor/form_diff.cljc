(ns ataru.virkailija.editor.form-diff)

(defn- index-of-id [id form]
  (if-let [some-id id]
    (if-let [some-match (ffirst (filter #(= id (:id (second %))) (map-indexed vector (:content form))))]
      some-match
      nil)))

(defn- index-of-element [element form]
  (let [id (:id element)]
    (index-of-id id form)))

(defn is-updated-in [old-element element]
  (and old-element
       (not (= old-element element))))

(defn find-element [id form]
  (first (filter #(= (:id %) id) (:content form))))

(defn find-elements [ids form]
  (filter #(contains? ids (:id %)) (:content form)))

(defn find-updated-elements [old-form form]
  (filter #(is-updated-in (find-element (:id %) old-form) %) (:content form)))

(defn insert-adjecent-elements-between [adjecent-elements siblings form]
  (let [content (:content form)
        [above below] (doall (map #(index-of-id % form) siblings))]
    (if (or
         (and (not above) (first siblings))
         (and (not below) (second siblings)))
      (throw (ex-info "Sibling with index not found! Mismatching forms!" {})))
    (cond
     (and above
          (not below)) (assoc form :content (concat content adjecent-elements))
     (and (not above)
          below) (assoc form :content (concat adjecent-elements content))
     (and (not above)
          (not below)) (assoc form :content adjecent-elements)
     (and above
          below) (let [adjecent? (= 1 (- above below))]
                   (if adjecent?
                     (let [[elements-above elements-below] (split-at above content)]
                       (assoc form :content (concat elements-above adjecent-elements elements-below)))
                     (throw (ex-info "Mismatching forms! User should update view." {})))))))

(defn find-element-siblings [form element]
  (let [content (:content form)
        index (index-of-element element form)
        sibling-above (get content (dec index))
        sibling-below (get content (inc index))]
    [sibling-above sibling-below]))

(defn- element-with-new-siblings? [element old-form form]
  (let [old-siblings (find-element-siblings old-form element)
        new-siblings (find-element-siblings form element)]
    (not (= (map :id old-siblings) (map :id new-siblings)))))

(defn find-elements-with-new-siblings
  [old-form form]
  (filter #(element-with-new-siblings? % old-form form) (:content form)))

(defn find-missing-elements
  [old-form form]
  (let [new-ids (set (map :id (:content form)))
        old-ids (set (map :id (:content old-form)))
        keys-in-old (clojure.set/difference old-ids new-ids)]
    (filter #(contains? keys-in-old (:id %)) (:content old-form))))

(defn as-delete-operation [element]
  {:type "delete"
   :element element})

(defn- connect-new-adjecent
  [adjecent]
  (fn
    ([]
     adjecent)
    ([new-adjecent]
     (let [first-id (get-in (first adjecent) [:element :id])
           last-id  (get-in (last adjecent) [:element :id])
           below-id (:sibling-below new-adjecent)
           above-id (:sibling-above new-adjecent)]
       (cond
        (= first-id below-id) (concat  [new-adjecent] adjecent)
        (= last-id above-id) (concat adjecent [new-adjecent] ))))
    ([first-new-adjecent second-new-adjecent]
     (-> (connect-new-adjecent (-> (connect-new-adjecent adjecent)
                                   first-new-adjecent)) second-new-adjecent))))

(defn adjecent-for-create-move-element [cm-element cm-elements]
  (loop [adjecent     [cm-element]
         not-adjecent cm-elements]
    (let [ids          (set (map #(get-in % [:element :id]) adjecent))
          adjecent?    (fn [e]
                         (if (seq (clojure.set/intersection ids #{(:sibling-above e) (:sibling-below e)}))
                           :adjecent
                           :not-adjecent))
          {some-new-adjecent :adjecent
           new-not-adjecent :not-adjecent} (group-by adjecent? not-adjecent)
          new-adjecent (apply (connect-new-adjecent adjecent) some-new-adjecent)]
      (if (and (seq some-new-adjecent)
               (seq new-not-adjecent))
        (recur new-adjecent new-not-adjecent)
        [new-adjecent new-not-adjecent]))))

(defn partition-by-adjecent-elements
  ([create-move-elements]
   (partition-by-adjecent-elements create-move-elements []))
  ([create-move-elements partitioned]
   (let [[head & tail] create-move-elements]
     (if-let [cm-element head]
       (let [[adjecent not-adjecent] (adjecent-for-create-move-element cm-element tail)]
         (partition-by-adjecent-elements not-adjecent (cons adjecent partitioned)))
       partitioned))))

(defn- as-create-move-element [old-form form element]
  (let [[above below] (find-element-siblings form element)
        existing-element? (find-element (:id element) old-form)]
    {:type (if existing-element? "move" "create")
     :sibling-above (some-> above :id)
     :sibling-below (some-> below :id)
     :element       element}))

(defn as-create-move-group-operation [old-form new-form elements]
  {:type  "create-move-group"
   :group (map #(as-create-move-element old-form new-form %) elements)})

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
              (if-let [elements-with-new-siblings (seq (find-elements-with-new-siblings old-form form))]
                (as-create-move-group-operation old-form form elements-with-new-siblings))])))

(defn replace-element [element form]
  (let [index (index-of-element element form)]
    (-> form
        (update-in [:content index] (fn [_] element)))))

(defn drop-nth [n coll]
  (keep-indexed #(if (not= %1 n) %2) coll))

(defn remove-element [form element]
  (let [index (index-of-element element form)]
    (assoc form :content (drop-nth index (:content form)))))

(defn remove-elements [form elements]
  (let [ids (set (map :id elements))]
    (assoc form :content (filter #(not (contains? ids (:id %))) (:content form)))))

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

(defn- apply-adjecent-create-move-elements [detached-form adjecent-create-move-elements]
  (let [[head & tail] adjecent-create-move-elements]
    (if head
      (let [adjecent-elements (map :element head)
            sibling-above (:sibling-above (first head))
            sibling-below (:sibling-below (last head))]
        (apply-adjecent-create-move-elements (insert-adjecent-elements-between adjecent-elements
                                               [sibling-above sibling-below] detached-form) tail))
      detached-form)))

(defn- apply-create-move-group [latest-form create-move-group]
  (let [element-ids (set (map #(get-in % [:element :id]) (:group create-move-group)))
        existing-elements (find-elements element-ids latest-form)
        create-moves-with-existing-elements (map #(replace-with-existing-element % existing-elements) (:group create-move-group))
        adjecent-create-move-elements (partition-by-adjecent-elements create-moves-with-existing-elements)
        detached-form (remove-elements latest-form existing-elements)]
    (apply-adjecent-create-move-elements detached-form adjecent-create-move-elements)))

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
  (let [[head & tail] operations]
    (if head
      (apply-operations (apply-operation latest-form head) tail)
      latest-form)))