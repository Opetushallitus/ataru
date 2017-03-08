(ns ataru.hakija.upload-store
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

(defn update-file [{:keys [tempfile filename]} key]
  (let [url  (str (get-in config [:liiteri :url]) "/api/files/" key)
        resp @(http/put url {:multipart [{:name     "file"
                                          :content  tempfile
                                          :filename filename}]})]
    (when (= (:status resp) 200)
      (-> (:body resp)
          (json/parse-string true)
          (dissoc :version :deleted)))))

(defn delete-file [key]
  (let [url  (str (get-in config [:liiteri :url]) "/api/files/" key)
        resp @(http/delete url)]
    (clojure.pprint/pprint resp)
    (when (= (:status resp) 200)
      (json/parse-string (:body resp) true))))
