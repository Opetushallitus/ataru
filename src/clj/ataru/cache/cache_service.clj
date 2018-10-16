(ns ataru.cache.cache-service)

(defprotocol Cache
  (get-from [this key])
  (get-many-from [this keys])
  (put-to [this key value])
  (remove-from [this key])
  (clear-all [this]))

(defprotocol MappedCache
  (get-from-or-fetch [this fetch-fn key])
  (put-many-to [this key-values]))
