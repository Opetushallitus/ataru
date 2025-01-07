(ns ataru.virkailija.application.excel-download.excel-utils
  (:require [ataru.util :refer [assoc? to-vec]]
            [ataru.tutkinto.tutkinto-util :refer [resolve-excel-content]]))

(defn assoc-in-excel [db k v]
  (assoc-in db (concat [:application :excel-request] (to-vec k)) v))

(defn get-in-excel [db k]
  (get-in db (concat [:application :excel-request] (to-vec k))))

(defn download-blob [file-name blob]
  (let [object-url (js/URL.createObjectURL blob)
        anchor-element
        (doto (js/document.createElement "a")
          (-> .-href (set! object-url))
          (-> .-download (set! file-name)))]
    (.appendChild (.-body js/document) anchor-element)
    (.click anchor-element)
    (try
      (.removeChild (.-body js/document) anchor-element)
      (js/URL.revokeObjectURL object-url)
      (catch js/Error e (println e)))))

(defn- question-wrapper? [field] (contains? #{"wrapperElement" "questionGroup"} (:fieldClass field)))

(defn- info-element? [field] (contains? #{"infoElement" "modalInfoElement"} (:fieldClass field)))

(defn- tutkinto-wrapper? [field] (and (= "wrapperElement" (:fieldClass field)) (= "tutkinnot" (:fieldType field))))

(defn get-excel-checkbox-filter-defs
  ([form-content form-field-belongs-to form-properties parent-id level parent-index-acc]
   (when (seq form-content)
     (reduce (fn [acc form-field]
               (let [index-acc (+ parent-index-acc (count acc))
                     child-objects (if (tutkinto-wrapper? form-field)
                                     (resolve-excel-content form-field form-properties)
                                     (:children form-field))
                     children (get-excel-checkbox-filter-defs child-objects
                                                              form-field-belongs-to
                                                              form-properties
                                                              (or parent-id (:id form-field))
                                                              (inc level)
                                                              (inc index-acc))]
                 (if (or (and (question-wrapper? form-field) (empty? children))
                         (info-element? form-field)
                         (get-in form-field [:params :hidden])
                         (:hidden form-field)
                         (:exclude-from-answers form-field)
                         (not (form-field-belongs-to form-field)))
                   acc
                   (merge acc children (when (or (= level 0) (not (question-wrapper? form-field)))
                                         {(:id form-field) (-> {:id (:id form-field)
                                                                :index index-acc
                                                                :label (:label form-field)
                                                                :checked true}
                                                               (assoc? :parent-id parent-id)
                                                               (assoc? :child-ids (->> children
                                                                                       (map second)
                                                                                       (sort-by :index)
                                                                                       (map :id))))})))))
             {}
             form-content)))
  ([form-content form-field-belongs-to form-properties]
   (get-excel-checkbox-filter-defs form-content form-field-belongs-to form-properties nil 0 0)))

(defn get-values-for-child-filters [db filter-id]
  (when-let [filter (get-in-excel db [:filters filter-id])]
    (map
     (fn [sibling-id] (boolean (get-in-excel db [:filters sibling-id :checked])))
     (:child-ids filter))))