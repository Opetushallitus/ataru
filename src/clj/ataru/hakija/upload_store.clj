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

(defn update-file [{:keys [tempfile filename]} id]
  (let [url  (str (get-in config [:liiteri :url]) "/api/files/" id)
        resp @(http/put url {:multipart [{:name     "file"
                                          :content  tempfile
                                          :filename filename}]})]
    (when (= (:status resp) 200)
      (-> (:body resp)
          (json/parse-string true)
          (dissoc :version :deleted)))))
