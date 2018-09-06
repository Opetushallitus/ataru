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

(defn- get-cache
  "Only allow access to preconfigured maps"
  [caches cache]
  (if-let [c (get caches cache)]
    c
    (throw (RuntimeException. (str "Invalid cache: " cache ", valid caches: " (keys caches))))))

(defn cache-get
  "Get cached item or return nil if not found.
   e.g. (cache-get :hakukohde objectid-of-hakukohde"
  [caches cache key]
  (get-from (get-cache caches cache) key))

(defn cache-get-many
  [caches cache keys]
  (get-many-from (get-cache caches cache) keys))

(defn cache-put
  "Store item in cache, returns old value.
   e.g. (cache-put :hakukohde objectid-of-hakukohde {...}"
  [caches cache key value]
  (put-to (get-cache caches cache) key value))

(defn cache-put-many
  "Stores multiple items (as k-v map) in cache"
  [caches cache key-values]
  (put-many-to (get-cache caches cache) key-values))

(defn cache-remove
  "Clears given entry in given cache"
  [caches cache key]
  (remove-from (get-cache caches cache) key))

(defn cache-clear
  "Clears all entries of given cache"
  [caches cache]
  (clear-all (get-cache caches cache)))

(defn cache-get-from-or-fetch
  [caches cache fetch-fn key]
  (get-from-or-fetch (get-cache caches cache) fetch-fn key))
