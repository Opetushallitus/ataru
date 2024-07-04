(ns ataru.virkailija.editor.form-diff
  (:require [clojure.set]
            #?(:clj [ataru.forms.form-payment-info :refer [set-payment-info]])))

(defn- user-feedback-exception
  [message]
  (ex-info message {:type :user-feedback-exception}))

(defn is-updated-in [old-element element]
  (and old-element
       (not (= old-element element))))

(defn find-element [id form]
  (first (filter #(= (:id %) id) (:content form))))

(defn- find-elements [ids form]
  (reduce (fn [elements e]
            (if (contains? ids (:id e))
              (assoc elements (:id e) e)
              elements))
          {}
          (:content form)))

(defn find-updated-elements [old-form form]
  (filter #(is-updated-in (find-element (:id %) old-form) %) (:content form)))

(defn find-element-siblings [form element]
  (let [[before starting-with] (split-with #(not= (:id element) (:id %))
                                           (:content form))]
    (when (not-empty starting-with)
      [(last before) (first starting-with) (second starting-with)])))

(defn- element-with-new-siblings [element old-form form]
  (let [old-siblings (find-element-siblings old-form element)
        new-siblings (find-element-siblings form element)]
    (when-not (= (map :id old-siblings) (map :id new-siblings)) new-siblings)))

(defn- find-groups-of-elements-with-new-siblings
  [old-form form]
  (let [add-to-latest-group (fn [{:keys [groups latest-group]} e]
                              {:groups       groups
                               :latest-group (conj latest-group e)})
        start-new-group     (fn [{:keys [groups latest-group]}]
                              {:groups       (if (empty? latest-group)
                                               groups
                                               (conj groups latest-group))
                               :latest-group []})]
    (->> (:content form)
         (reduce (fn [state element]
                   (if-let [e (element-with-new-siblings element old-form form)]
                     (add-to-latest-group state e)
                     (start-new-group state)))
                 {:groups       []
                  :latest-group []})
         start-new-group
         :groups)))

(defn find-missing-elements
  [old-form form]
  (let [new-ids (set (map :id (:content form)))
        old-ids (set (map :id (:content old-form)))
        keys-in-old (clojure.set/difference old-ids new-ids)]
    (filter #(contains? keys-in-old (:id %)) (:content old-form))))

(defn as-delete-operation [element]
  {:type "delete"
   :element element})

(defn- as-create-move-element [siblings]
  {:sibling-above (:id (first (first siblings)))
   :sibling-below (:id (last (last siblings)))
   :elements      (map second siblings)})

(defn as-create-move-group-operation [elements-with-new-siblings]
  {:type   "create-move-group"
   :groups (map as-create-move-element elements-with-new-siblings)})

(defn as-update-operation [old-form element]
  {:type        "update"
   :old-element (find-element (:id element) old-form)
   :new-element element})

(defn- form-details [form]
  (select-keys form [:name :languages :properties]))

(defn as-update-form-details-operation [old-form form]
  (let [old-form-details (form-details old-form)
        new-form-details (form-details form)]
    (when-not (= old-form-details new-form-details)
      {:type        "update-form-details"
       :old-form old-form-details
       :new-form new-form-details})))

(defn as-operations [old-form form]
  (remove nil?
    (flatten [(as-update-form-details-operation old-form form)
              (map #(as-update-operation old-form %) (find-updated-elements old-form form))
              (map #(as-delete-operation %) (find-missing-elements old-form form))
              (when-let [elements-with-new-siblings (seq (find-groups-of-elements-with-new-siblings old-form form))]
                (as-create-move-group-operation elements-with-new-siblings))])))

(defn replace-element [element form]
  (update form :content (partial map #(if (= (:id element) (:id %)) element %))))

(defn remove-element [form element]
  (update form :content (partial remove #(= (:id element) (:id %)))))

(defn- remove-elements [form elements]
  (assoc form :content (remove #(contains? elements (:id %)) (:content form))))

(defn- apply-update [latest-form {:keys [old-element new-element]}]
  (let [latest-element (find-element (:id new-element) latest-form)]
    (if (= old-element latest-element)
      (replace-element new-element latest-form)
      (throw (user-feedback-exception "Muokatusta osiosta oli uudempi versio.")))))

(defn- replace-if-existing-element [existing-elements element]
  (if-let [existing (get existing-elements (:id element))]
    existing
    element))

(defn- replace-groups-elements-with-existing [existing-elements group]
  (update group :elements
          (partial map (partial replace-if-existing-element existing-elements))))

(defn- insert-group-of-adjacent-elements
  [form {:keys [sibling-above sibling-below elements]}]
  (update form :content
          (fn [content]
            (let [[before after] (if (some? sibling-below)
                                   (split-with #(not= sibling-below (:id %))
                                               content)
                                   [content []])]
              (if (and (or (and (nil? sibling-above) (empty? before))
                           (= sibling-above (:id (last before))))
                       (or (and (nil? sibling-below) (empty? after))
                           (= sibling-below (:id (first after)))))
                (concat before elements after)
                (throw (user-feedback-exception "Lomakkeen rakenteesta oli uudempi versio.")))))))

(defn- apply-create-move-group [latest-form create-move-group]
  (let [element-ids                   (->> (:groups create-move-group)
                                           (mapcat :elements)
                                           (map :id)
                                           set)
        existing-elements             (find-elements element-ids latest-form)
        groups-with-existing-elements (map (partial replace-groups-elements-with-existing
                                                    existing-elements)
                                           (:groups create-move-group))]
    (reduce insert-group-of-adjacent-elements
            (remove-elements latest-form (set (keys existing-elements)))
            groups-with-existing-elements)))

(defn- apply-delete [latest-form {:keys [element]}]
  (let [latest-element (find-element (:id element) latest-form)]
    (if (= element latest-element)
      (remove-element latest-form latest-element)
      (throw (user-feedback-exception "Poistettavasta osiosta oli uudempi versio.")))))

(defn- apply-update-form-details [latest-form {:keys [old-form new-form]}]
  (let [current-form (form-details latest-form)]
    (if (= old-form current-form)
      #?(:clj (-> latest-form
                  (merge (dissoc new-form :payment))
                  (set-payment-info (:payment new-form)))
         :cljs (merge latest-form new-form))

      (throw (user-feedback-exception "Lomakkeen tiedoista oli uudempi versio.")))))

(defn- apply-operation [latest-form operation]
  (condp = (:type operation)
         "update" (apply-update latest-form operation)
         "create-move-group" (apply-create-move-group latest-form operation)
         "delete" (apply-delete latest-form operation)
         "update-form-details" (apply-update-form-details latest-form operation)
         latest-form))

(defn apply-operations [latest-form operations]
  (reduce apply-operation latest-form operations))
