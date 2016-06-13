(ns ataru.virkailija.editor.handlers-test
  (:require [cljs.test :refer-macros [async deftest are is]]
            [ataru.virkailija.editor.handlers :as h]
            ;[ataru.virkailija.editor.handlers-test-macros :refer-macros [with-mock-fn]]
            [ataru.virkailija.virkailija-ajax :as http :refer [post]]))

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

(deftest remove-component-removes-from-root-level
  (let [form-id 1234
        initial-form {:id form-id
                      :content [{:first :component} {:second :another-component}]}
        new-content (-> {:editor {:selected-form-id form-id
                                  :forms {form-id initial-form}}}
                        (h/remove-component [0])
                        (get-in [:editor :forms form-id :content]))]
    (are [expected actual] (= expected actual)
      1 (count new-content)
      {:second :another-component} (first new-content))))

(deftest remove-component-removes-from-child
  (let [form-id 1234
        initial-form {:id form-id
                      :content [{:children [{:first :component} {:second :another-component}]}]}
        new-children (-> {:editor {:selected-form-id form-id
                                   :forms {form-id initial-form}}}
                         (h/remove-component [0 :children 1])
                         (get-in [:editor :forms form-id :content 0 :children]))]
    (are [expected actual] (= expected actual)
      1 (count new-children)
      {:first :component} (first new-children))))
