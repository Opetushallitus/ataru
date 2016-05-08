(ns lomake-editori.middleware.session-store
  (:require [ring.middleware.session.store :refer [SessionStore]]))

(def atom-store (atom {}))

(defn read-data [key]
  (println "read-data" key)
  (get @atom-store key))

(defn save-data [key data]
  (println "save-data" key data)
  (swap! atom-store assoc key data))

(defn delete-data [key]
  (println "delete-data" key)
  (swap! atom-store assoc key nil))

(defn generate-new-random-key [] (str (java.util.UUID/randomUUID)))

(deftype DatabaseStore []
    SessionStore
  (read-session [_ key]
                (read-data key))
  (write-session [_ key data]
                 (let [key (or key (generate-new-random-key))]
                   (save-data key data)
                   key))
  (delete-session [_ key]
                  (delete-data key)
                nil))

(defn create-store [] (DatabaseStore.))
