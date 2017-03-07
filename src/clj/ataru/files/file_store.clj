(ns ataru.files.file-store
  (:require [oph.soresu.common.config :refer [config]]
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

(defn get-metadata [application-keys]
  (let [query-part (->> application-keys
                        (map-indexed (fn [idx key]
                                       (let [separator (if (= idx 0) "?" "&")]
                                         (str separator "key=" key))))
                        (clojure.string/join))
        url        (str (get-in config [:liiteri :url]) "/api/files" query-part)
        resp       @(http/get url)]
    (when (= (:status resp) 200)
      (json/parse-string (:body resp) true))))
