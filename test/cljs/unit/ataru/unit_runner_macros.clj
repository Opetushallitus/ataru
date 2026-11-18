(ns ataru.unit-runner-macros
  (:require [cljs.analyzer :as ana]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private test-root "test/cljs/unit")

(defn- file->ns [file]
  (-> (str (.relativize (.toPath (io/file test-root)) (.toPath file)))
      (str/replace #"\.cljs$" "")
      (str/replace java.io.File/separator ".")
      (str/replace "_" "-")
      symbol))

(defn- test-namespaces []
  (->> (file-seq (io/file test-root))
       (filter #(str/ends-with? (.getName %) "_test.cljs"))
       (map file->ns)
       sort))

; ClojureScriptin ns-muodon requiret ovat staattisia, joten testinimiavaruudet on
; pakko listata kutsujan ns-muotoon. Tämä makro ajaa kaikki test-rootin alla olevat
; *_test.cljs -nimiavaruudet ja kaataa käännöksen, jos jokin niistä puuttuu listalta.
(defmacro run-all-cljs-unit-tests []
  (let [namespaces (test-namespaces)
        required   (set (vals (:requires (ana/get-namespace ana/*cljs-ns*))))
        missing    (remove required namespaces)]
    (when (seq missing)
      (throw (ex-info (str "Seuraavat testinimiavaruudet puuttuvat " ana/*cljs-ns*
                           " -nimiavaruuden requireista: " (str/join ", " missing))
                      {:missing missing})))
    `(cljs.test/run-tests ~@(map (fn [n] `(quote ~n)) namespaces))))
