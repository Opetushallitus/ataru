(ns ataru.virkailija.editor.handlers-test
  (:require [cljs.test :refer-macros [async deftest are is]]
            [ataru.virkailija.editor.handlers :as h]
            [ataru.virkailija.virkailija-ajax :as http :refer [post]]))

(defn generate-fn
  []
  {:fake :component})

(deftest generate-component-adds-to-root-level
  (let [form-key 1234
        initial-form {:key form-key
                      :content [{:some :component}]}
        new-content (-> {:editor {:selected-form-key form-key
                                  :forms {form-key initial-form}}}
                        (h/generate-component [:generate-component generate-fn 1])
                        (get-in [:editor :forms form-key :content]))]
    (are [expected actual] (= expected actual)
      2 (count new-content)
      {:some :component} (first new-content)
      {:fake :component} (second new-content))))

(deftest generate-component-adds-to-child
  (let [form-key 1234
        initial-form {:key form-key
                      :content [{:children [{:child :component}]}]}
        new-children (-> {:editor {:selected-form-key form-key
                                   :forms         {form-key initial-form}}}
                         (h/generate-component [:generate-component generate-fn [0 :children 1]])
                         (get-in [:editor :forms form-key :content 0 :children]))]
    (are [expected actual] (= expected actual)
      2 (count new-children)
      {:child :component} (first new-children)
      {:fake :component} (second new-children))))

(deftest remove-component-removes-from-root-level
  (let [form-key 1234
        initial-form {:key form-key
                      :content [{:first :component} {:second :another-component}]}
        new-content (-> {:editor {:selected-form-key form-key
                                  :forms {form-key initial-form}}}
                        (h/remove-component [0])
                        (get-in [:editor :forms form-key :content]))]
    (are [expected actual] (= expected actual)
      1 (count new-content)
      {:second :another-component} (first new-content))))

(deftest remove-component-removes-from-child
  (let [form-key 1234
        initial-form {:key form-key
                      :content [{:children [{:first :component} {:second :another-component}]}]}
        new-children (-> {:editor {:selected-form-key form-key
                                   :forms {form-key initial-form}}}
                         (h/remove-component [0 :children 1])
                         (get-in [:editor :forms form-key :content 0 :children]))]
    (are [expected actual] (= expected actual)
      1 (count new-children)
      {:first :component} (first new-children))))

(def drag-component-1 {:id "G__2"
                       :label {:fi "First question" :sv ""}})

(def drag-component-2 {:id "G__5"
                       :label {:fi "Second question" :sv ""}})

(defn- as-form
  [content]
  (let [form-key 1234]
    {:editor {:selected-form-key form-key
              :forms {form-key {:content content}}}}))

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
  (let [target-path    [1]
        source-path    [0 :children 0]
        state-before   (as-form [{:children [drag-component-1]}])
        expected-state (as-form [{:children []} drag-component-1])
        actual-state   (h/move-component state-before [:editor/move-component source-path target-path])
        content-path   [:editor :forms 1234 :content]]
    (is (= (get-in actual-state content-path)
           (get-in expected-state content-path)))))

(deftest adds-validator-to-component
  (let [path          [0 :children 0]
        actual        (-> (as-form [{:children [drag-component-1]}])
                          (h/add-validator [:editor/add-validator "required" path])
                          (get-in [:editor :forms 1234 :content 0 :children 0 :validators]))]
    (is (= actual ["required"]))))

(deftest adds-validators-to-component-with-existing-validators
  (let [path          [0 :children 0]
        component     (assoc drag-component-1 :validators ["some-validator"])
        actual        (-> (as-form [{:children [component]}])
                          (h/add-validator [:editor/add-validator "required" path])
                          (get-in [:editor :forms 1234 :content 0 :children 0 :validators]))]
    (is (= (count actual) 2))
    (is (some #(= % "some-validator") actual))
    (is (some #(= % "required") actual))))

(deftest removes-validator-from-component
  (let [path          [0 :children 0]
        component     (assoc drag-component-1 :validators ["required" "some-validator"])
        actual        (-> (as-form [{:children [component]}])
                          (h/remove-validator [:editor/remove-validator "required" path])
                          (get-in [:editor :forms 1234 :content 0 :children 0 :validators]))]
    (is (= actual ["some-validator"]))))

(deftest does-not-create-empty-validator-list-when-removing-validator
  (let [path          [0 :children 0]
        actual        (-> (as-form [{:children [drag-component-1]}])
                          (h/remove-validator [:editor/remove-validator "required" path])
                          (get-in [:editor :forms 1234 :content 0 :children 0 :validators]))]
    (is (nil? actual))))

(deftest handles-removal-of-validator-that-does-not-exist
  (let [path          [0 :children 0]
        component     (assoc drag-component-1 :validators ["required"])
        actual        (-> (as-form [{:children [component]}])
                        (h/remove-validator [:editor/remove-validator "some-validator" path])
                        (get-in [:editor :forms 1234 :content 0 :children 0 :validators]))]
    (is (= actual ["required"]))))
