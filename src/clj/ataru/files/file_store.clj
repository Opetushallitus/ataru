(ns ataru.files.file-store
  (:require [ataru.config.core :refer [config]]
            [ataru.config.url-helper :refer [resolve-url]]
            [ataru.url :as url]
            [ataru.util.http-util :as http-util]
            [cheshire.core :as json])
  (:import (java.text Normalizer Normalizer$Form)))

(defn upload-file [{:keys [tempfile filename]}]
  (let [url  (resolve-url :liiteri.files)
        resp (http-util/do-post url {:multipart [{:name     "file"
                                                  :content  tempfile
                                                  :filename (Normalizer/normalize filename Normalizer$Form/NFD)}]})]
    (when (= (:status resp) 200)
      (-> (:body resp)
          (json/parse-string true)
          (dissoc :version :deleted)))))

(defn delete-file [file-key]
  (let [url  (resolve-url :liiteri.file file-key)
        resp (http-util/do-delete url)]
    (when (= (:status resp) 200)
      (json/parse-string (:body resp) true))))

(defn get-metadata [file-keys]
  (let [resp (http-util/do-post (resolve-url :liiteri.metadata)
                                {:headers {"Content-Type" "application/json"}
                                 :body    (json/generate-string {:keys file-keys})})]
    (when (= (:status resp) 200)
      (json/parse-string (:body resp) true))))

(defn get-file [key]
  (let [url  (resolve-url :liiteri.file key)
        resp (http-util/do-get url)]
    (when (= (:status resp) 200)
      {:body                (:body resp)
       :content-disposition (-> resp :headers :content-disposition)})))
