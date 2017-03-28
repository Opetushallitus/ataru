(ns ataru.files.file-store
  (:require [ataru.url :as url]
            [ataru.config.url-helper :refer [resolve-url]]
            [oph.soresu.common.config :refer [config]]
            [org.httpkit.client :as http]
            [cheshire.core :as json]))

(defn upload-file [{:keys [tempfile filename]}]
  (let [url  (resolve-url :liiteri.files)
        resp @(http/post url {:multipart [{:name     "file"
                                           :content  tempfile
                                           :filename filename}]})]
    (when (= (:status resp) 200)
      (-> (:body resp)
          (json/parse-string true)
          (dissoc :version :deleted)))))

(defn delete-file [file-key]
  (let [url  (resolve-url :liiteri.file file-key)
        resp @(http/delete url)]
    (when (= (:status resp) 200)
      (json/parse-string (:body resp) true))))

(defn get-metadata [file-keys]
  (let [query-part (clojure.string/join (url/items->query-part "key" file-keys))
        ; TODO: fix query-part to use url.props correctly
        url        (str (resolve-url :liiteri.metadata) query-part)
        resp       @(http/get url)]
    (when (= (:status resp) 200)
      (json/parse-string (:body resp) true))))

(defn get-file [key]
  (let [url  (resolve-url :liiteri.file key)
        resp @(http/get url)]
    (when (= (:status resp) 200)
      (:body resp))))
