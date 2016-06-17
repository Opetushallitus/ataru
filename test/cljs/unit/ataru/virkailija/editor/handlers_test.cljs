(ns ataru.virkailija.editor.handlers-test
  (:require [cljs.test :refer-macros [async deftest are is]]
            [ataru.virkailija.editor.handlers :as h]
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

(def drag-component-1 {:id "G__2"
                       :label {:fi "First question" :sv ""}})

(def drag-component-2 {:id "G__5"
                       :label {:fi "Second question" :sv ""}})

(defn- as-form
  [content]
  (let [form-id 1234]
    {:editor {:selected-form-id form-id
              :forms {form-id {:content content}}}}))

(deftest on-drop-moves-form-component-at-root-level
  (let [target-path    [0]
        source-path    [1]
        state-before   (as-form [drag-component-1 drag-component-2])
        expected-state (as-form [drag-component-2 drag-component-1])
        actual-state   (h/move-component state-before [:editor/move-component source-path target-path])
        content-path   [:editor :forms 1234 :content]]
    (is (= (get-in actual-state content-path)
           (get-in expected-state content-path)))))

(deftest on-drop-moves-form-component-from-root-to-child-level
  (let [target-path    [1 :children 0]
        source-path    [0]
        state-before   (as-form [drag-component-1 {:children [drag-component-2]}])
        expected-state (as-form [{:children [drag-component-1 drag-component-2]}])
        actual-state   (h/move-component state-before [:editor/move-component source-path target-path])
        content-path   [:editor :forms 1234 :content]]
    (is (= (get-in actual-state content-path)
           (get-in expected-state content-path)))))

(deftest on-drop-moves-form-component-from-child-to-root-level
  (let [target-path    [0]
        source-path    [0 :children 0]
        state-before   (as-form [{:children [drag-component-1 drag-component-2]}])
        expected-state (as-form [drag-component-1 {:children [drag-component-2]}])
        actual-state   (h/move-component state-before [:editor/move-component source-path target-path])
        content-path   [:editor :forms 1234 :content]]
    (is (= (get-in actual-state content-path)
           (get-in expected-state content-path)))))

(deftest on-drop-does-not-secretly-move-component-into-component-group
  (let [target-path    [2]
        source-path    [0]
        state-before   (as-form [drag-component-1 drag-component-2 {:children []}])
        expected-state (as-form [drag-component-2 drag-component-1 {:children []}])
        actual-state   (h/move-component state-before [:editor/move-component source-path target-path])
        content-path   [:editor :forms 1234 :content]]
    (is (= (get-in actual-state content-path)
           (get-in expected-state content-path)))))

(deftest on-drop-does-not-secretly-change-component-order
  (let [target-path    [1]
        source-path    [0]
        state-before   (as-form [drag-component-1 drag-component-2])
        expected-state (as-form [drag-component-1 drag-component-2])
        actual-state   (h/move-component state-before [:editor/move-component source-path target-path])
        content-path   [:editor :forms 1234 :content]]
    (is (= (get-in actual-state content-path)
           (get-in expected-state content-path)))))

(deftest on-drop-moves-component-to-end-of-the-form
  (let [target-path    [2]
        source-path    [0]
        state-before   (as-form [drag-component-1 drag-component-2])
        expected-state (as-form [drag-component-2 drag-component-1])
        actual-state   (h/move-component state-before [:editor/move-component source-path target-path])
        content-path   [:editor :forms 1234 :content]]
    (is (= (get-in actual-state content-path)
           (get-in expected-state content-path)))))

(deftest on-drop-moves-child-component-to-end-of-the-container
  (let [target-path    [0 :children 2]
        source-path    [0 :children 0]
        state-before   (as-form [{:children [drag-component-1 drag-component-2]}])
        expected-state (as-form [{:children [drag-component-2 drag-component-1]}])
        actual-state   (h/move-component state-before [:editor/move-component source-path target-path])
        content-path   [:editor :forms 1234 :content]]
    (is (= (get-in actual-state content-path)
           (get-in expected-state content-path)))))
