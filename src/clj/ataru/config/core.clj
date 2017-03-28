(ns ataru.config.core
  (:require [clojure.edn]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]))

(defn config-name [] (env :config))

(defonce defaults (-> (or (env :configdefaults) "config/defaults.edn")
                      (slurp)
                      (clojure.edn/read-string)))

(defn- slurp-if-found [path]
  (try
    (slurp path)
    (catch Exception e
      (log/warn (str "Could not read configuration from '" path "'"))
      "{}")))

(defonce secrets
         (if-let [config-secrets (env :configsecrets)]
           (-> config-secrets
               (slurp-if-found)
               (clojure.edn/read-string))))

(defn- merge-with-defaults [config]
  (merge-with merge defaults config))

(defn- merge-with-secrets [config]
  (if-let [secrets-config secrets]
    (merge-with merge secrets-config config)
    config))

(defonce config (->> (or (env :config) "config/dev.edn")
                     (slurp)
                     (clojure.edn/read-string)
                     (merge-with-defaults)
                     (merge-with-secrets)))

