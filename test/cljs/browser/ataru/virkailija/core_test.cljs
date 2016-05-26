(ns ataru.virkailija.core-test
  (:require [cljs.test :refer-macros [async deftest is testing use-fixtures]]
            [goog.dom :as dom]
            [goog.dom.DomHelper :as dh]
            [jayq.core :as jq]
            [ataru.virkailija.core :as core]))

(defn app-frame
  []
  (-> (jq/$ :#test)
      (first)
      (.-contentWindow)
      (.-document)
      (jq/$)))

(defn setup
  []
  (async done
    (doto (jq/$ :#test)
      (jq/attr :src "/lomake-editori/")
      (jq/attr :width "1024")
      (jq/attr :height "768"))
    (js/setTimeout done 3000)))

(defn not-nil?
  [x]
  (not (nil? x)))

(defn get-element
  [xpath]
  (let [elements (-> (app-frame)
                     (.xpath xpath))]
    (when (= 1 (count elements))
      (first elements))))

(defn editor-link
  []
  (get-element "//span[@class='active-section']/span[text()='Lomake-editori']"))

(use-fixtures :once {:before setup})

(deftest ui-header
  (testing "header has editor link"
    (let [header-link-set? (-> (editor-link)
                               (not-nil?))]
      (is header-link-set?))))
