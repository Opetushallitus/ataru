(ns ataru.filename-normalizer
  (:require [clojure.string :as string]
            [schema.core :as s])
  #?(:clj (:import [java.text Normalizer Normalizer$Form])))

;; See UTF codes mapped to characters at https://unicode-table.com/en/

(def ^:private remove-pattern #"[^\u0030-\u0039\u0041-\u005a\u0061-\u007a\.\-_]")

(def ^:private underscore-pattern #"[\s\u0021-\u002c\/\u003a-\u0040\u005b-\u005e\u007b-\u007e]")

(defn- normalize [input]
  #?(:cljs (.normalize input "NFD")
     :clj  (Normalizer/normalize input Normalizer$Form/NFD)))

(s/defn normalize-filename :- s/Str
  "Normalizes filename, removing all characters except a-z, A-Z, 0-9 and converting
   various other characters to underscore (_)."
  [input :- s/Str]
  (->> (string/split input #"")
       (transduce (comp (map normalize)
                        (map #(string/replace % underscore-pattern "_"))
                        (map #(string/replace % remove-pattern "")))
                  str)))
