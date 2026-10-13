(ns ataru.util.js-bundle
  "Identity and resources of the compiled ClojureScript bundles."
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as log])
  (:import [java.security MessageDigest]))

(def ^:private bundle-name-pattern
  ;; A plain file name, so that a name coming from a browser stack trace can
  ;; never be turned into a path of its own choosing.
  #"[a-zA-Z0-9._-]+\.js")

(defn resource
  "Classpath resource of a compiled bundle, or of a file next to it when given a
   suffix such as \".map\". Nil when the name is not a plain bundle file name or
   the file is not there."
  ([bundle] (resource bundle ""))
  ([bundle suffix]
   (when (and bundle (re-matches bundle-name-pattern bundle))
     (io/resource (str "public/js/compiled/" bundle suffix)))))

(defn- sha-256-prefix
  "First 12 hex characters of the SHA-256 digest of a resource."
  [source]
  (let [digest (MessageDigest/getInstance "SHA-256")
        buffer (byte-array 8192)]
    (with-open [stream (io/input-stream source)]
      (loop []
        (let [read (.read stream buffer)]
          (when (pos? read)
            (.update digest buffer 0 read)
            (recur)))))
    (->> (.digest digest)
         (take 6)
         (map #(format "%02x" %))
         (apply str))))

(def ^:private build-ids (atom {}))

(defn- read-build-id [bundle]
  (if-let [bundle-resource (resource bundle)]
    (try
      (let [build-id (sha-256-prefix bundle-resource)]
        (log/info "Build id of" bundle "is" build-id)
        build-id)
      (catch Exception e
        (log/warn e "Could not read build id of" bundle)
        nil))
    (do (log/info "No compiled bundle available for" bundle)
        nil)))

(defn build-id
  "Identifies the build of a compiled bundle. Unlike the cache busting
   fingerprint, which is the start time of this server process, this changes
   only when the JavaScript itself changes. Computed once per bundle."
  [bundle]
  (if (contains? @build-ids bundle)
    (get @build-ids bundle)
    (get (swap! build-ids
                (fn [cached]
                  (if (contains? cached bundle)
                    cached
                    (assoc cached bundle (read-build-id bundle)))))
         bundle)))
