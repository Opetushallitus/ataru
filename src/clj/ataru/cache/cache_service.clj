(ns ataru.cache.cache-service)

(defprotocol Cache
  (get-from [this key])
  (put-to [this key value])
  (remove-from [this key])
  (clear-all [this]))

(defn- get-cache
  "Only allow access to preconfigured maps"
  [caches cache]
  (if-let [c (get caches cache)]
    c
    (throw (RuntimeException. (str "Invalid cache: " cache)))))

(defn cache-get
  "Get cached item or return nil if not found.
   e.g. (cache-get :hakukohde objectid-of-hakukohde"
  [caches cache key]
  (get-from (get-cache caches cache) key))

(defn cache-put
  "Store item in cache, returns old value.
   e.g. (cache-put :hakukohde objectid-of-hakukohde {...}"
  [caches cache key value]
  (put-to (get-cache caches cache) key value))

(defn cache-remove
  "Clears given entry in given cache"
  [caches cache key]
  (remove-from (get-cache caches cache) key))

(defn cache-clear
  "Clears all entries of given cache"
  [caches cache]
  (clear-all (get-cache caches cache)))
