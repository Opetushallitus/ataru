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
        new-content (-> {:editor {:selected-form-id form-id
                                  :forms {form-id initial-form}}}
                        (h/generate-component [:generate-component generate-fn 1])
                        (get-in [:editor :forms form-id :content]))]
    (are [expected actual] (= expected actual)
      2 (count new-content)
      {:some :component} (first new-content)
      {:fake :component} (second new-content))))

(deftest generate-component-adds-to-child
  (let [form-id 1234
        initial-form {:id form-id
                      :content [{:children [{:child :component}]}]}
        new-children (-> {:editor {:selected-form-id form-id
                                   :forms         {form-id initial-form}}}
                         (h/generate-component [:generate-component generate-fn [0 :children 1]])
                         (get-in [:editor :forms form-id :content 0 :children]))]
    (are [expected actual] (= expected actual)
      2 (count new-children)
      {:child :component} (first new-children)
      {:fake :component} (second new-children))))
