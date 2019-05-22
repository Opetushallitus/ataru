(ns ataru.cache.cache-service
  (:refer-clojure :exclude [load]))

(defprotocol CacheLoader
  (load [this key])
  (load-many [this keys])
  (load-many-size [this])
  (check-schema [this value]))

(defn ->FunctionCacheLoader
  ([f]
   (reify CacheLoader
     (load [_ key] (f key))
     (load-many [_ keys] (into {} (keep #(when-let [v (f %)] [% v]) keys)))
     (load-many-size [_] 1)
     (check-schema [_ _] nil)))
  ([f checker]
   (reify CacheLoader
     (load [_ key] (f key))
     (load-many [_ keys] (into {} (keep #(when-let [v (f %)] [% v]) keys)))
     (load-many-size [_] 1)
     (check-schema [_ value] (checker value)))))

(defprotocol Cache
  (get-from [this key])
  (get-many-from [this keys])
  (remove-from [this key])
  (clear-all [this]))

(defprotocol Stats
  (stats [this]))
