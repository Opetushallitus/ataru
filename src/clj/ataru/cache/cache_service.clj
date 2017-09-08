(ns ataru.cache.cache-service)

(defprotocol Cache
  (get-from [this key] [this key get-fn])
  (put-to [this key value])
  (remove-from [this key])
  (clear-all [this]))

(defn- get-cache [caches cache]
  "Only allow access to preconfigured maps"
  (if-let [c (get caches cache)]
    c
    (throw (RuntimeException. (str "Invalid cache: " cache)))))

(defn cache-get [caches cache key]
  "Get cached item or return nil if not found.
    e.g. (cache-get :hakukohde objectid-of-hakukohde"
  (get-from (get-cache caches cache) key))

(defn cache-put [caches cache key value]
  "Store item in cache, returns old value.
    e.g. (cache-put :hakukohde objectid-of-hakukohde {...}"
  (put-to (get-cache caches cache) key value))

(defn cache-get-or-fetch [caches cache key fetch-fn]
  "Get cached item or invoke get-fn to store & return
    e.g. (cache-get-or-fetch :hakukohde #(hakukohde-client/get-hakukohde objectid-of-hakukohde)"
  (get-from (get-cache caches cache) key fetch-fn))

(defn cache-remove [caches cache key]
  "Clears given entry in given cache"
  (remove-from (get-cache caches cache) key))

(defn cache-clear [caches cache]
  "Clears all entries of given cache"
  (clear-all (get-cache caches cache)))
