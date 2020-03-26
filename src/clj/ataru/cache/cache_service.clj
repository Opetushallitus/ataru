(ns ataru.cache.cache-service
  (:refer-clojure :exclude [load]))

(defprotocol CacheLoader
  (load [this key])
  (load-many [this keys])
  (load-many-size [this])
  (check-schema [this value]))

(defn default-load-many [this keys]
  (into {} (keep #(when-let [v (load this %)] [% v]) keys)))

(defn ->FunctionCacheLoader
  ([f]
   (reify CacheLoader
     (load [_ key] (f key))
     (load-many [this keys] (default-load-many this keys))
     (load-many-size [_] 1)
     (check-schema [_ _] nil)))
  ([f checker]
   (reify CacheLoader
     (load [_ key] (f key))
     (load-many [this keys] (default-load-many this keys))
     (load-many-size [_] 1)
     (check-schema [_ value] (checker value)))))

(defprotocol Cache
  (get-from [this key])
  (get-many-from [this keys])
  (remove-from [this key])
  (clear-all [this]))

(defprotocol Stats
  (stats [this]))
