(ns ataru.util.apply-in-batches-spec
  (:require [speclj.core :refer [describe tags it should=]]
            [ataru.util.apply-in-batches :as a]))

(defn test-function [arg1 items]
  (mapv #(- % arg1) items))

(defn test-function2 [arg1 arg2 items]
  (mapv #(* arg2 (- % arg1)) items))


(describe "batch processing"
          (tags :unit)
          (it "applies function to each list item with one argument"
              (let [items [3 3 2 2 2 3 3 4 4 4]
                    result [2 2 1 1 1 2 2 3 3 3]
                    batch-size 3]
              (should=
               result
               (a/apply-in-batches test-function items batch-size 1))))
          (it "applies function to each list item with two arguments"
              (let [items [3 3 2 2 2 3 3 4 4 4]
                    result [4 4 2 2 2 4 4 6 6 6]
                    batch-size 4]
                (should=
                 result
                 (a/apply-in-batches test-function2 items batch-size 1 2)))))
