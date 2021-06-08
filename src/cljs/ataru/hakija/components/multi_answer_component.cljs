(ns ataru.hakija.components.multi-answer-component
  (:require [re-frame.core :refer [subscribe]]))

(defn multi-answer
  [field-descriptor]
  (fn [body]
    (if (:per-hakukohde field-descriptor)
      (do (println field-descriptor)
        [:section
          (for [hakukohde @(subscribe [:application/hakukohteet-in-hakukohderyhmat (:belongs-to-hakukohderyhma field-descriptor)])]
            ^{:key (:oid hakukohde)} [:section {:style {:margin-bottom "15px"}} body])])
      body)
    ))