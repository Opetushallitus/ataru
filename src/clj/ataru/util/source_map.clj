(ns ataru.util.source-map
  "Resolves positions in the compiled ClojureScript bundles back to the original
   sources with the source maps produced by the build."
  (:require [ataru.util.js-bundle :as js-bundle]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [taoensso.timbre :as log]))

(def ^:private base64-chars "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/")

(def ^:private base64-values
  (let [values (int-array 128 -1)]
    (dotimes [i (count base64-chars)]
      (aset values (int (.charAt base64-chars i)) i))
    values))

(defn- decode-vlq
  "Decodes a single Base64 VLQ encoded segment into a vector of integers."
  [^String segment]
  (loop [i      0
         shift  0
         value  0
         result []]
    (if (= i (.length segment))
      result
      (let [digit (aget ^ints base64-values (int (.charAt segment i)))]
        (when (neg? digit)
          (throw (ex-info "Invalid base64 character in source map segment"
                          {:segment segment})))
        (let [continue? (pos? (bit-and digit 32))
              value     (+ value (bit-shift-left (bit-and digit 31) shift))]
          (if continue?
            (recur (inc i) (+ shift 5) value result)
            (recur (inc i)
                   0
                   0
                   (conj result (if (odd? value)
                                  (- (unsigned-bit-shift-right value 1))
                                  (unsigned-bit-shift-right value 1))))))))))

(defn- decode-line
  "Decodes the segments of one generated line. Returns the state carried over to
   the next line and the segments of this line as a flat seq of
   [generated-column source-index source-line source-column name-index]."
  [state ^String line]
  (loop [segments (remove string/blank? (string/split line #"," -1))
         state    (assoc state :generated-column 0)
         entries  []]
    (if-let [segment (first segments)]
      (let [values (decode-vlq segment)
            state  (cond-> (update state :generated-column + (first values))
                           (<= 4 (count values))
                           (-> (update :source + (nth values 1))
                               (update :source-line + (nth values 2))
                               (update :source-column + (nth values 3)))
                           (= 5 (count values))
                           (update :name + (nth values 4)))]
        (recur (rest segments)
               state
               (if (<= 4 (count values))
                 (conj entries
                       (:generated-column state)
                       (:source state)
                       (:source-line state)
                       (:source-column state)
                       (if (= 5 (count values)) (:name state) -1))
                 entries)))
      [state entries])))

(defn- decode-mappings
  "Decodes the mappings string into {generated-line <int-array>}, where each
   array holds the quintuples of that line ordered by generated column."
  [^String mappings]
  (loop [lines     (string/split mappings #";" -1)
         line-no   0
         state     {:generated-column 0 :source 0 :source-line 0 :source-column 0 :name 0}
         by-line   (transient {})]
    (if-let [line (first lines)]
      (let [[state entries] (decode-line state line)]
        (recur (rest lines)
               (inc line-no)
               state
               (if (seq entries)
                 (assoc! by-line line-no (int-array entries))
                 by-line)))
      (persistent! by-line))))

(defn- clean-source-name
  "Turns 'hakija-app-out/ataru/hakija/core.cljs?rel=123' into
   'ataru/hakija/core.cljs'. Sources that are nothing but the output directory
   have no original file behind them and are left as they are."
  [source]
  (let [without-cache-buster (string/replace source #"\?.*$" "")
        without-output-dir   (string/replace without-cache-buster #"^[^/]*-out/" "")]
    (if (string/blank? without-output-dir)
      without-cache-buster
      without-output-dir)))

(defn parse
  "Parses a source map (as a reader, file or resource) into a form that
   resolve-position can use."
  [source]
  (let [parsed (json/parse-stream (io/reader source) true)]
    {:sources (mapv clean-source-name (:sources parsed))
     :names   (vec (:names parsed))
     :lines   (decode-mappings (:mappings parsed))}))

(defn- entry-index
  "Index of the last quintuple in entries whose generated column is at most
   column, or nil when the column precedes every mapping on the line."
  [^ints entries column]
  (let [n (quot (alength entries) 5)]
    (loop [low 0
           high (dec n)
           found nil]
      (if (< high low)
        found
        (let [mid (quot (+ low high) 2)]
          (if (<= (aget entries (* 5 mid)) column)
            (recur (inc mid) high mid)
            (recur low (dec mid) found)))))))

(defn resolve-position
  "Maps a position in the generated file to the original source. Line and column
   are 1-based, as browsers report them. Returns nil when the position has no
   mapping."
  [source-map line column]
  (when (and source-map (pos? line) (pos? column))
    (when-let [^ints entries (get (:lines source-map) (dec line))]
      (when-let [index (entry-index entries (dec column))]
        (let [offset     (* 5 index)
              name-index (aget entries (+ offset 4))]
          (cond-> {:source (get (:sources source-map) (aget entries (+ offset 1)))
                   :line   (inc (aget entries (+ offset 2)))
                   :column (inc (aget entries (+ offset 3)))}
                  (not (neg? name-index))
                  (assoc :name (get (:names source-map) name-index))))))))

(def ^:private cache (atom {}))

(defn- load-source-map [bundle]
  (if-let [resource (js-bundle/resource bundle ".map")]
    (try
      (log/info "Parsing source map for" bundle)
      (parse resource)
      (catch Exception e
        (log/warn e "Could not parse source map for" bundle)
        nil))
    (do (log/info "No source map available for" bundle)
        nil)))

(defn source-map-for-bundle
  "Source map of a compiled bundle, or nil when it is unknown or unavailable.
   Parsed once per bundle and then kept in memory."
  [bundle]
  (if (contains? @cache bundle)
    (get @cache bundle)
    (get (swap! cache
                (fn [cached]
                  (if (contains? cached bundle)
                    cached
                    (assoc cached bundle (load-source-map bundle)))))
         bundle)))
