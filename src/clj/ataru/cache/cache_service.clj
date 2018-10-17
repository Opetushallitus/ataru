(ns ataru.cache.cache-service
  (:refer-clojure :exclude [load]))

(defprotocol CacheLoader
  (load [this key])
  (load-many [this keys]))

(defprotocol Cache
  (get-from [this key])
  (get-many-from [this keys])
  (put-to [this key value])
  (remove-from [this key])
  (clear-all [this]))

(defprotocol MappedCache
  (get-from-or-fetch [this fetch-fn key])
  (put-many-to [this key-values]))
