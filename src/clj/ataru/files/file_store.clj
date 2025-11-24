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
        resp (cas/cas-authenticated-get-as-stream cas-client url)
        status (:status resp)]
    (log/info "DEBUG GET file status:" status "key:" key)
    (log/info "DEBUG Headers for file key:" key ":" (:headers resp))

    (if (= status 200)
      {:body                (:body resp)
       :content-disposition (-> resp :headers :content-disposition)}
      (do
        (log/error "Failed to fetch file:" key "status:" status)
        nil))))

(defn extract-filename [content-disposition]
  ;; Returns nil if header missing or no match
  (when content-disposition
    (second
     (re-matches
      #"(?i)attachment;\s*filename=\"?(.*?)\"?$"
      content-disposition))))

(defn generate-filename [filename counter]
  (let [parts     (str/split filename #"\.")
        name      (str/join "." (butlast parts))
        extension (last parts)
        safe-name (apply str (take 240 name))]
    (str safe-name counter "." extension)))

(defn get-file-zip [liiteri-cas-client keys output-stream]
  (with-open [zip-out (ZipOutputStream. output-stream)]
    (let [seen-filenames (atom #{})
          counter (atom 0)]
      (doseq [key keys]
        (if-let [{:keys [body content-disposition]} (get-file liiteri-cas-client key)]
          (let [header-filename   (extract-filename content-disposition)
                fallback-filename (or header-filename (str key))
                base-filename     (generate-filename fallback-filename "")
                final-filename    (if (contains? @seen-filenames base-filename)
                                    (generate-filename fallback-filename
                                                       (swap! counter inc))
                                    base-filename)]
            (swap! seen-filenames conj final-filename)
            (log/info "DEBUG Adding ZIP entry:" final-filename)
            (.putNextEntry zip-out (ZipEntry. final-filename))
            (with-open [file-stream body]
              (io/copy file-stream zip-out))
            (.closeEntry zip-out))

          (log/error "Could not fetch file" key))))))
