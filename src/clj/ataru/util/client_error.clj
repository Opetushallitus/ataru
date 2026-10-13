(ns ataru.util.client-error
  (:require [ataru.util.js-bundle :as js-bundle]
            [ataru.util.source-map :as source-map]
            [clojure.string :as string]
            [taoensso.timbre :as log]
            [schema.core :as s]))

(s/defschema ClientError {:error-message                s/Str
                          :url                          s/Str
                          :line                         s/Int
                          :col                          s/Int
                          :description                  s/Str
                          :user-agent                   s/Str
                          :stack                        s/Str
                          ;; Optional: browsers running a bundle built before
                          ;; this was added do not send it.
                          (s/optional-key :build-id)    s/Str})

(def ^:private max-mapped-frames
  "Stack traces come from an unauthenticated endpoint, so only this many frames
   are resolved."
  100)

(def ^:private max-frame-length 2000)

(def ^:private frame-pattern
  ;; The format stacktrace-js produces: functionName(args)@fileName:line:column
  #"^(.*)@(\S+?):(\d+):(\d+)$")

(def ^:private compiled-bundle-pattern #"/js/compiled/[^/\s]+\.js")

(defn- from-our-code?
  "Whether anything in the error points at one of our compiled bundles. In-app
   browsers inject scripts of their own into the page, and the errors those
   throw end up in our global error handler attributed to the page itself. They
   are not ours to fix, so they are not logged as errors. An error without a
   stack trace cannot be attributed either way and is kept as ours."
  [{:keys [url stack]}]
  (boolean (or (string/blank? stack)
               (re-find compiled-bundle-pattern (or url ""))
               (re-find compiled-bundle-pattern stack))))

(defn- bundle-name
  "The compiled bundle a stack frame points at, e.g.
   'https://opintopolku.fi/hakemus/js/compiled/hakija-app.js?fingerprint=1' ->
   'hakija-app.js'."
  [url]
  (some-> url
          (string/replace #"[?#].*$" "")
          (string/split #"/")
          last))

(defn- format-position [{:keys [source line column name]}]
  (str source ":" line ":" column (when name (str " (" name ")"))))

(defn- resolve-url [resolve-fn url line column]
  (some-> (bundle-name url) (resolve-fn line column)))

(defn- resolve-frame [resolve-fn frame]
  (when-let [[_ _ url line column] (re-matches frame-pattern frame)]
    (resolve-url resolve-fn url (Integer/parseInt line) (Integer/parseInt column))))

(defn map-stack-trace
  "Rewrites the frames of a stack trace to original ClojureScript positions with
   resolve-fn, which takes a bundle name, a line and a column. Frames that
   cannot be resolved are kept as they are. Returns nil when no frame could be
   resolved, i.e. when the mapped trace would add nothing."
  [stack resolve-fn]
  (let [frames (->> (string/split-lines (or stack ""))
                    (take max-mapped-frames))
        mapped (map (fn [frame]
                      (if (< max-frame-length (count frame))
                        frame
                        (if-let [position (resolve-frame resolve-fn frame)]
                          (format-position position)
                          frame)))
                    frames)]
    (when (not= (seq frames) (seq mapped))
      (string/join "\n" mapped))))

(defn- source-map-resolver
  "Resolves positions with the source map of the bundle, but only for a client
   that ran the same build as this server has: the same line in another build is
   another piece of code, so mapping it would be worse than not mapping at all.
   Build ids that did not match are collected into mismatches."
  [client-build-id {:keys [source-map-fn build-id-fn]} mismatches]
  (fn [bundle line column]
    (let [server-build-id (build-id-fn bundle)]
      (if (and (some? server-build-id) (= client-build-id server-build-id))
        (some-> (source-map-fn bundle) (source-map/resolve-position line column))
        (when (some? server-build-id)
          (swap! mismatches conj [bundle server-build-id])
          nil)))))

(defn- mapped-details
  [{:keys [url line col stack build-id]} source-maps]
  (try
    (let [mismatches (atom #{})
          resolve-fn (source-map-resolver build-id source-maps mismatches)]
      {:position   (some-> (resolve-url resolve-fn url line col) format-position)
       :stack      (map-stack-trace stack resolve-fn)
       :mismatches @mismatches})
    (catch Exception e
      (log/warn e "Could not map client error to original sources")
      nil)))

(defn- build-description [client-build-id mismatches]
  (str "build: " (or client-build-id "unknown")
       (when-let [[bundle server-build-id] (first mismatches)]
         (str " (not mapped to sources: this server has build " server-build-id
              " of " bundle ")"))))

(def ^:private default-source-maps
  {:source-map-fn source-map/source-map-for-bundle
   :build-id-fn   js-bundle/build-id})

(defn log-client-error
  ([error-details]
   (log-client-error error-details default-source-maps))
  ([error-details source-maps]
   (let [mapped  (mapped-details error-details source-maps)
         ours?   (from-our-code? error-details)
         message (str "Error from client browser:\n"
                      (:description error-details) "\n"
                      (:error-message error-details) "\n"
                      (:url error-details) "\n"
                      "line: " (:line error-details) " column: " (:col error-details) "\n"
                      (when-let [position (:position mapped)]
                        (str "source: " position "\n"))
                      (build-description (:build-id error-details) (:mismatches mapped)) "\n"
                      (when-not ours?
                        "no frame points at our bundle, so this is most likely a script injected by the browser\n")
                      "user-agent: " (:user-agent error-details) "\n"
                      "stack trace: " (:stack error-details)
                      (when-let [stack (:stack mapped)]
                        (str "\nmapped stack trace:\n" stack)))]
     (if ours?
       (log/error message)
       (log/warn message)))))
