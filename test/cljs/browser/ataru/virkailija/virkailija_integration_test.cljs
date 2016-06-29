(ns ataru.virkailija.virkailija-integration-test
  (:require [cljs.core.async :refer [<!]]
            [cljs.test :refer-macros [deftest is async use-fixtures]]
            [jayq.core :refer [$ text find attr trigger]]
            [ataru.virkailija.test-util :as util]
            [ataru.common-test-util :refer [await sleep]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn setup
  []
  (util/setup))

(use-fixtures :once {:before setup})

(defn find-in-frame
  [query]
  (find (util/app-frame) query))

(defn- one-exists
  [query-fn]
  (= 1 (count (query-fn))))

(defn- form-list-items
  []
  (find-in-frame "#editor-form__list .editor-form__row"))

(defn- add-new-form-link
  []
  (find-in-frame ".editor-form__add-new a"))

(defn- form-name-input
  []
  (find-in-frame ".editor-form__form-name-input"))

(defn- set-value
  [$el v]
  (attr $el :value v))

(defn- click
  [$el]
  (trigger $el " click"))

(defn- await-is
  [test-fn]
  (let [result-ch (await test-fn)]
    (async done
      (go
        (is (boolean (<! result-ch)))
        (done)))))

(deftest page-loads
  (await-is #(= 1 (count (add-new-form-link)))))

(deftest form-list-is-empty
  (await-is #(empty? (form-list-items))))

(deftest create-new-form
  (click (add-new-form-link))
  (set-value (form-name-input) "Testilomake 1")
  (is true))

;(deftest create-text-area-element
;  (is true))
;
;(deftest enter-text-into-text-area
;  (is true))
;
;(deftest create-text-field-element
;  (is true))
;
;(deftest enter-text-into-text-field
;  (is true))
;
;(deftest create-dropdown-element
;  (is true))
;
;(deftest enter-options-into-dropdown
;  (is true))
