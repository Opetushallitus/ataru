(ns ataru.cache.cache-service
  (:refer-clojure :exclude [load]))

(defprotocol CacheLoader
  (load [this key])
  (load-many [this keys]))

(defrecord FunctionCacheLoader [f]
  CacheLoader

  (load [_ key]
    (f key))

  (load-many [this keys]
    (into {} (keep #(when-let [v (f %)] [% v]) keys))))

(defprotocol Cache
  (get-from [this key])
  (get-many-from [this keys])
  (remove-from [this key])
  (clear-all [this]))
