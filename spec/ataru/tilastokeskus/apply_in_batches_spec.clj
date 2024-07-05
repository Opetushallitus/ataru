(ns ataru.tilastokeskus.apply-in-batches-spec
  (:require [speclj.core :refer [describe tags it should=]]
            [ataru.tilastokeskus.tilastokeskus-service :as tilastokeskus]))

(defn test-function [service items]
  (mapv #(- % service) items))

(defn test-function2 [service arg items]
  (mapv #(* arg (- % service)) items))

(describe "batch processing"
          (tags :unit)
          (it "applies function to each list item with one argument"
              (let [items [3 3 2 2 2 3 3 4 4 4]
                    result [2 2 1 1 1 2 2 3 3 3]
                    batch-size 3]
              (should=
               result
               (tilastokeskus/apply-in-batches test-function 1 items batch-size))))
          (it "applies function to each list item with two arguments"
              (let [items [3 3 2 2 2 3 3 4 4 4]
                    result [4 4 2 2 2 4 4 6 6 6]
                    batch-size 4]
                (should=
                 result
                 (tilastokeskus/apply-in-batches test-function2 1 2 items batch-size)))))
