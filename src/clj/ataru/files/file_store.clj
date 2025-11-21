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
    (log/info "Get file response headers:" (:headers resp))
    (if (= (:status resp) 200)
      {:body                (:body resp)
       :content-disposition (-> resp :headers :content-disposition)}
      (do (log/error "failed to get file " key " from url " url ", response " + resp)
          nil))))

(def cd-filename-regex
  #"(?i)(?:filename\*?=(?:UTF-8''|\"?))([^\";]+)")

(defn extract-filename [content-disposition]
  (when content-disposition
    (some->> content-disposition
             (re-find cd-filename-regex)
             second)))

(defn generate-filename [filename counter]
  (let [[name ext] (let [parts (str/split filename #"\.")]
                     (if (> (count parts) 1)
                       [(str/join "." (butlast parts)) (last parts)]
                       [filename ""]))

        base (apply str (take 240 name))

        counter-suffix (if (seq (str counter))
                         (str "-" counter)
                         "")]

    (if (seq ext)
      (str base counter-suffix "." ext)
      (str base counter-suffix))))

(defn get-file-zip [liiteri-cas-client keys out]
  (with-open [zout (ZipOutputStream. out)]
    (let [seen-filenames (atom #{})
          counter (atom 0)]
      (doseq [key keys]
        (if-let [{:keys [body content-disposition]} (get-file liiteri-cas-client key)]
          (do
            (log/info "Content-disposition:" content-disposition)
            (let [raw-filename (extract-filename content-disposition)
                ;; fallback name if header missing
                filename     (or raw-filename (str key))
                base         (generate-filename filename "")
                unique-name  (if (contains? @seen-filenames base)
                               (generate-filename filename (swap! counter inc))
                               base)]

              (swap! seen-filenames conj unique-name)
              (.putNextEntry zout (ZipEntry. unique-name))
              (with-open [file-stream body]
                (io/copy file-stream zout))
              (.closeEntry zout)))

          (log/error "Could not get file" key))))))
