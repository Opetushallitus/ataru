(ns ataru.files.file-store
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.util.http-util :as http-util]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [clojure.string :as str]
            [string-normalizer.filename-normalizer :as normalizer])
  (:import [java.util.zip ZipOutputStream ZipEntry]))

(defn upload-file [{:keys [tempfile filename]}]
  (let [url  (resolve-url :liiteri.files)
        resp (http-util/do-post url {:multipart [{:name     "file"
                                                  :content  tempfile
                                                  :filename (normalizer/normalize-filename filename)}]})]
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
  (if (seq file-keys)
    (let [resp (http-util/do-post (resolve-url :liiteri.metadata)
                                  {:headers {"Content-Type" "application/json"}
                                   :body    (json/generate-string {:keys file-keys})})]
      (if (= (:status resp) 200)
        (vec (json/parse-string (:body resp) true))
        (throw (new RuntimeException
                    (str "Could not get metadata for keys "
                         (clojure.string/join ", " file-keys)
                         ". Got status " (:status resp)
                         ", body " (:body resp))))))
    file-keys))

(defn get-file [key]
  (let [url  (resolve-url :liiteri.file key)
        resp (http-util/do-get-stream url)]
    (when (= (:status resp) 200)
      {:body                (:body resp)
       :content-disposition (-> resp :headers :content-disposition)})))

(defn- generate-filename [filename counter]
  (let [name (str (str/join "." (butlast (str/split filename #"\."))))
        extension (last (str/split filename #"\."))]
    (str (apply str (take 240 name)) counter "." extension)))

( defn get-file-zip [keys out]
  (with-open [zout (ZipOutputStream. out)]
    (let [filenames (atom #{})
          counter (atom 0)]
      (doseq [key keys]
        (if-let [file (get-file key)]
          (let [[_ filename] (re-matches #"attachment; filename=\"(.*)\"" (:content-disposition file))
                generated-filename (if (contains? @filenames (generate-filename filename ""))
                                     (generate-filename filename (swap! counter inc))
                                     (generate-filename filename ""))]
            (.putNextEntry zout (new ZipEntry generated-filename))
            (with-open [fin (:body file)]
              (io/copy fin zout))
            (swap! filenames conj generated-filename)
            (.closeEntry zout)
            (.flush zout))
          (log/error "Could not get file" key))))))
