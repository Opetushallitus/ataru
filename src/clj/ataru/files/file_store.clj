(ns ataru.files.file-store
  (:require [ataru.url :as url]
            [oph.soresu.common.config :refer [config]]
            [org.httpkit.client :as http]
            [cheshire.core :as json]))

(defn upload-file [{:keys [tempfile filename]}]
  (let [url  (str (get-in config [:liiteri :url]) "/api/files")
        resp @(http/post url {:multipart [{:name     "file"
                                           :content  tempfile
                                           :filename filename}]})]
    (when (= (:status resp) 200)
      (-> (:body resp)
          (json/parse-string true)
          (dissoc :version :deleted)))))

(defn update-file [{:keys [tempfile filename]} file-key]
  (let [url  (str (get-in config [:liiteri :url]) "/api/files/" file-key)
        resp @(http/put url {:multipart [{:name     "file"
                                          :content  tempfile
                                          :filename filename}]})]
    (when (= (:status resp) 200)
      (-> (:body resp)
          (json/parse-string true)
          (dissoc :version :deleted)))))

(defn delete-file [file-key]
  (let [url  (str (get-in config [:liiteri :url]) "/api/files/" file-key)
        resp @(http/delete url)]
    (when (= (:status resp) 200)
      (json/parse-string (:body resp) true))))

(defn get-metadata [file-keys]
  (let [query-part (clojure.string/join (url/items->query-part "key" file-keys))
        url        (str (get-in config [:liiteri :url]) "/api/files/metadata" query-part)
        resp       @(http/get url)]
    (when (= (:status resp) 200)
      (json/parse-string (:body resp) true))))

(defn get-file [key]
  (let [url  (str (get-in config [:liiteri :url]) "/api/files/" key)
        resp @(http/get url)]
    (when (= (:status resp) 200)
      (:body resp))))
