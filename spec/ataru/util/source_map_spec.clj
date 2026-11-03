(ns ataru.util.source-map-spec
  (:require [ataru.util.source-map :as source-map]
            [speclj.core :refer :all])
  (:import [java.io StringReader]))

;; Generated line 1: column 0 -> source line 1, column 1, name "my-fn"
;;                   column 10 -> source line 2, column 1
;; Generated line 2: no mappings
;; Generated line 3: column 4 -> source line 3, column 3
(def ^:private test-source-map
  (str "{\"version\":3,"
       "\"sources\":[\"hakija-app-out/ataru/hakija/core.cljs?rel=1791375784355\"],"
       "\"names\":[\"my-fn\"],"
       "\"mappings\":\"AAAAA,UACA;;IACE\"}"))

(defn- parsed []
  (source-map/parse (StringReader. test-source-map)))

(describe "source map"
  (tags :unit)

  (it "strips the output directory and cache busting parameter from source names"
      (should= ["ataru/hakija/core.cljs"] (:sources (parsed))))

  (it "keeps a source that is nothing but the output directory"
      (should= ["hakija-app-out/"]
               (:sources (source-map/parse
                           (StringReader.
                             (str "{\"version\":3,"
                                  "\"sources\":[\"hakija-app-out/?rel=1791375784357\"],"
                                  "\"names\":[],"
                                  "\"mappings\":\"AAAA\"}"))))))

  (it "resolves a position to the original source"
      (should= {:source "ataru/hakija/core.cljs" :line 1 :column 1 :name "my-fn"}
               (source-map/resolve-position (parsed) 1 1)))

  (it "resolves a position to the mapping that precedes it"
      (should= {:source "ataru/hakija/core.cljs" :line 1 :column 1 :name "my-fn"}
               (source-map/resolve-position (parsed) 1 10)))

  (it "resolves a position after the next mapping to that mapping"
      (should= {:source "ataru/hakija/core.cljs" :line 2 :column 1}
               (source-map/resolve-position (parsed) 1 11)))

  (it "keeps source state over generated lines without mappings"
      (should= {:source "ataru/hakija/core.cljs" :line 3 :column 3}
               (source-map/resolve-position (parsed) 3 5)))

  (it "returns nil for a generated line without mappings"
      (should-be-nil (source-map/resolve-position (parsed) 2 1)))

  (it "returns nil for a column before the first mapping of the line"
      (should-be-nil (source-map/resolve-position (parsed) 3 4)))

  (it "returns nil for a position outside the generated file"
      (should-be-nil (source-map/resolve-position (parsed) 99 1)))

  (it "returns nil for a position that no browser would report"
      (should-be-nil (source-map/resolve-position (parsed) 0 0)))

  (it "returns nil without a source map"
      (should-be-nil (source-map/resolve-position nil 1 1)))

  (it "does not load source maps for unknown bundles"
      (should-be-nil (source-map/source-map-for-bundle "../../../etc/passwd"))
      (should-be-nil (source-map/source-map-for-bundle "1.2.246.562.29.00000000000000101055"))))
