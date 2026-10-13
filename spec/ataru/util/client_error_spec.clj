(ns ataru.util.client-error-spec
  (:require [ataru.util.client-error :as client-error]
            [ataru.util.source-map :as source-map]
            [speclj.core :refer :all]
            [taoensso.timbre :as log])
  (:import [java.io StringReader]))

(def ^:private test-source-map
  (str "{\"version\":3,"
       "\"sources\":[\"hakija-app-out/ataru/hakija/core.cljs?rel=1791375784355\"],"
       "\"names\":[\"my-fn\"],"
       "\"mappings\":\"AAAAA,UACA\"}"))

(def ^:private build-id "a1b2c3d4e5f6")

(def ^:private source-maps
  {:source-map-fn (fn [bundle]
                    (when (= "hakija-app.js" bundle)
                      (source-map/parse (StringReader. test-source-map))))
   :build-id-fn   (fn [bundle]
                    (when (= "hakija-app.js" bundle) build-id))})

(def ^:private bundle-frame
  "Jk()@https://opintopolku.fi/hakemus/js/compiled/hakija-app.js?fingerprint=1:1:1")

(def ^:private page-frame
  "Lk()@https://opintopolku.fi/hakemus/haku/1.2.246.562.29.00000000000000101055?lang=fi:225:25")

(defn- resolver [bundle line column]
  (when (and (= "hakija-app.js" bundle) (= 1 line))
    {:source "ataru/hakija/core.cljs" :line line :column column :name "my-fn"}))

(defn- error-details [overrides]
  (merge {:error-message "RangeError: Maximum call stack size exceeded."
          :url           "https://opintopolku.fi/hakemus/haku/1.2.3"
          :line          225
          :col           25
          :description   "Hakija form=c58f1204"
          :user-agent    "Mozilla/5.0 (iPhone)"
          :stack         bundle-frame}
         overrides))

(defn- logged [error-details]
  (let [messages (atom [])]
    (log/with-merged-config
      {:appenders {:println {:enabled? false}
                   :test    {:enabled? true
                             :fn       #(swap! messages conj (force (:msg_ %)))}}}
      (client-error/log-client-error error-details source-maps))
    (apply str @messages)))

(describe "mapping client error stack traces"
  (tags :unit)

  (it "maps frames that point at a compiled bundle"
      (should= "ataru/hakija/core.cljs:1:1 (my-fn)"
               (client-error/map-stack-trace bundle-frame resolver)))

  (it "keeps frames that cannot be resolved"
      (should= (str "ataru/hakija/core.cljs:1:11 (my-fn)\n" page-frame)
               (client-error/map-stack-trace
                 (str "Jk()@https://opintopolku.fi/hakemus/js/compiled/hakija-app.js?fingerprint=1:1:11\n"
                      page-frame)
                 resolver)))

  (it "returns nil when no frame can be resolved"
      (should-be-nil (client-error/map-stack-trace (str page-frame "\n" page-frame) resolver)))

  (it "returns nil for an empty stack trace"
      (should-be-nil (client-error/map-stack-trace "" resolver))))

(describe "logging client errors"
  (tags :unit)

  (it "maps the stack trace when the client ran the build this server has"
      (let [message (logged (error-details {:build-id build-id}))]
        (should-contain "mapped stack trace:\nataru/hakija/core.cljs:1:1 (my-fn)" message)
        (should-contain (str "build: " build-id) message)))

  (it "does not map the stack trace of another build"
      (let [message (logged (error-details {:build-id "0000deadbeef"}))]
        (should-not-contain "mapped stack trace:" message)
        (should-contain "build: 0000deadbeef" message)
        (should-contain "not mapped to sources: this server has build a1b2c3d4e5f6 of hakija-app.js"
                        message)))

  (it "does not map the stack trace of a client that reports no build"
      (let [message (logged (error-details {}))]
        (should-not-contain "mapped stack trace:" message)
        (should-contain "build: unknown" message)))

  (it "always logs the raw error details"
      (let [message (logged (error-details {:build-id build-id}))]
        (should-contain "RangeError: Maximum call stack size exceeded." message)
        (should-contain "line: 225 column: 25" message)
        (should-contain (str "stack trace: " bundle-frame) message)))

  (it "logs the error even when mapping fails"
      (should-not-throw
        (client-error/log-client-error
          (error-details {:build-id build-id})
          {:source-map-fn (fn [_] (throw (Exception. "no source map here")))
           :build-id-fn   (constantly build-id)}))))
