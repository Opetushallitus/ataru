(ns ataru.virkailija.application.excel-download.excel-utils
  (:require [ataru.util :refer [assoc? to-vec]]))

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

(defn get-excel-checkbox-filter-defs
  ([form-content form-field-belongs-to parent-id level parent-index-acc]
   (if (empty? form-content)
     nil
     (reduce (fn [acc form-field]
               (let [index-acc (+ parent-index-acc (count acc))
                     children (get-excel-checkbox-filter-defs (:children form-field)
                                                              form-field-belongs-to
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
  ([form-content form-field-belongs-to]
   (get-excel-checkbox-filter-defs form-content form-field-belongs-to nil 0 0)))