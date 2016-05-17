(ns lomake-editori.editor.handlers-test
  (:require [cljs.test :refer-macros [deftest are]]
            [lomake-editori.editor.handlers :as h]))

(defn generate-fn
  []
  {:fake :component})

(deftest generate-component-adds-to-root-level
  (let [form-id 1234
        initial-form {:id form-id
                      :content [{:some :component}]}
        new-content (-> {:editor {:selected-form initial-form
                                  :forms {form-id initial-form}}}
                        (h/generate-component [:generate-component generate-fn 1])
                        (get-in [:editor :forms form-id :content]))]
    (are [x y] (= x y)
      2 (count new-content)
      {:some :component} (first new-content)
      {:fake :component} (second new-content))))
