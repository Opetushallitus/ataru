(ns ataru.files.file-store
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [cheshire.core :as json]
            [ataru.cas.client :as cas]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [clojure.string :as str])
  (:import [java.util.zip ZipOutputStream ZipEntry]))

(defn delete-file [cas-client file-key]
  (let [url  (resolve-url :liiteri.file file-key)
        resp (cas/cas-authenticated-delete cas-client url)]
    (when (= (:status resp) 200)
      (json/parse-string (:body resp) true))))

(defn get-metadata [cas-client file-keys]
  (if (seq file-keys)
    (let [resp (cas/cas-authenticated-post
                 cas-client
                 (resolve-url :liiteri.metadata)
                 {:keys file-keys})]
      (if (= (:status resp) 200)
        (vec (json/parse-string (:body resp) true))
        (throw (new RuntimeException
                    (str "Could not get metadata for keys "
                         (clojure.string/join ", " file-keys)
                         ". Got status " (:status resp)
                         ", body " (:body resp))))))
    file-keys))

(defn get-file [cas-client key]
  (let [url  (resolve-url :liiteri.file key)
        resp (cas/cas-authenticated-get-as-stream cas-client url)]
    (if (= (:status resp) 200)
      {:body                (:body resp)
       :content-disposition (-> resp :headers :content-disposition)}
      (do (log/error "failed to get file " key " from url " url ", response " + resp)
          nil))))

(defn- generate-filename [filename counter]
  (let [name (str (str/join "." (butlast (str/split filename #"\."))))
        extension (last (str/split filename #"\."))]
    (str (apply str (take 240 name)) counter "." extension)))

(defn get-file-zip [liiteri-cas-client keys out]
  (with-open [zout (ZipOutputStream. out)]
    (let [filenames (atom #{})
          counter (atom 0)]
      (doseq [key keys]
        (if-let [file (get-file liiteri-cas-client key)]
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
