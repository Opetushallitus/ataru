(ns ataru.preferred-name
  (:require [clojure.string :as string]
            [clojure.set :as set]))

; This should be equal or tighter than the ONR validation, so that we don't allow for
; first name + preferred name combos that would later make the ONR create / update fail.
(defn main-first-name?
  [{:keys [value answers-by-key]}]
  (let [first-name (clojure.string/trim (or (-> answers-by-key :first-name :value) ""))
        first-names-whitespace      (clojure.string/split first-name #"[\s]")
        first-names-whitespace-dash (clojure.string/split first-name #"[\s-]")
        create-name-set-fn (fn [names]
                             (let [num-names (count names)]
                               (set
                                (for [sub-length (range 1 (inc num-names))
                                      start-idx  (range 0 num-names)
                                      :when (<= (+ sub-length start-idx) num-names)]
                                  (clojure.string/join " " (subvec names start-idx (+ start-idx sub-length)))))))
        possible-names (set/union
                        ; E.g. "arpa-tupla noppa kuutio" =>
                        ; "arpa-tupla" "noppa" "kuutio" "arpa-tupla noppa" "noppa kuutio" "arpa-tupla noppa kuutio", ...
                        (create-name-set-fn first-names-whitespace)
                        ; E.g. "arpa-tupla noppa kuutio" =>
                        ; "arpa" "tupla" "noppa" "kuutio" "arpa tupla noppa" "noppa kuutio" "arpa tupla noppa kuutio", ...
                        (create-name-set-fn first-names-whitespace-dash))]
    (or (and (empty? (filter #(not (string/blank? %)) first-names-whitespace))
             (empty? (filter #(not (string/blank? %)) first-names-whitespace-dash))
             (string/blank? value))
        (contains? possible-names value))))
