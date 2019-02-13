(ns ataru.cache.application-review-cache
  (:require [taoensso.carmine :as car :refer [wcar]]
            [taoensso.carmine.message-queue :as car-mq]
            [taoensso.carmine.locks :as carlocks]))

(defn ->cache-key
  [& keys]
  (str "ataru:application-reviewers:" (clojure.string/join ":" keys)))

(defn redis-scan
  [redis key cursor]
  (wcar (:connection-opts redis)
    (car/scan cursor :match key)))

(defn parse-key [key]
  (let [[_ _ hakemus-oid user-oid name] (clojure.string/split key #":")]
    {:user-oid user-oid
     :name     name}))

(defn get-reviewers-for-application [redis hakemus-oid]
  (let [key (->cache-key hakemus-oid "*")]
    (map parse-key
         (loop [[cursor keys] (redis-scan redis key 0)
                all []]
           (if (= "0" cursor)
             (concat all keys)
             (recur (redis-scan redis key cursor)
               (concat all keys)))))))

(defn upsert-reviewer [redis hakemus-oid user-oid name]
  (let [update-period-s 15
        key (->cache-key hakemus-oid user-oid name)]
    (wcar (:connection-opts redis)
      (car/set key "!" :ex update-period-s))))