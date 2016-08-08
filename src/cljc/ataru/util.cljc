(ns ataru.util
  (:require #?(:cljs [ataru.cljs-util :as util])
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]]))
  (:import #?(:clj [java.util UUID])))

(defn map-kv [m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn group-by-first [kw m]
  (-> (group-by kw m)
      (map-kv first)))

(defn component-id []
  #?(:cljs (util/new-uuid)
     :clj  (str (UUID/randomUUID))))

(defn flatten-form-fields [fields]
  (flatten
    (for [field fields]
      (match
        field

        {:fieldClass "wrapperElement"
         :fieldType  "fieldset"
         :children   children
         :id         id}
        (flatten-form-fields
          (map #(assoc % :wrapper-id id) children))

        {:fieldClass "wrapperElement"
         :fieldType  "rowcontainer"
         :children   children
         :wrapper-id wrapper-id}
        (flatten-form-fields
          (map #(assoc % :wrapper-id wrapper-id) children))

        :else field))))
