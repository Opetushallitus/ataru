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

(defn get-file
  "Fetch a file via cas client and return {:body <InputStream> :content-disposition <string>}"
  [cas-client key]
  (let [url  (resolve-url :liiteri.file key)
        resp (cas/cas-authenticated-get-as-stream cas-client url)
        status (:status resp)
        cd     (get-in resp [:headers "content-disposition"])]
    (log/info "DEBUG GET file status:" status "key:" key)
    (log/info "DEBUG Headers for file key:" key ":" (:headers resp))
    (log/info "DEBUG Extracted content-disposition for key" key ":" (pr-str cd))
    (if (= status 200)
      {:body                (:body resp)
       :content-disposition cd}
      (do
        (log/error "Failed to fetch file:" key "status:" status)
        nil))))

(defn extract-filename
  "Extracts filename from a Content-Disposition header of the form:
   attachment; filename=\"foo.jpg\"
   Returns nil if not present."
  [content-disposition]
  (when content-disposition
    (second (re-matches #"attachment; filename=\"(.*)\"" content-disposition))))

(defn generate-filename [filename counter]
  (let [parts     (str/split filename #"\.")
        name      (str/join "." (butlast parts))
        extension (last parts)
        safe-name (apply str (take 240 name))]
    (str safe-name counter "." extension)))

(defn get-file-zip
  "Write a ZIP to output-stream containing files by keys fetched from CAS.
   Filenames are taken from Content-Disposition headers (exact original behavior).
   If header missing, logs an error and skips the file."
  [liiteri-cas-client keys output-stream]
  (with-open [zip-out (ZipOutputStream. output-stream)]
    (let [seen-filenames (atom #{})
          counter        (atom 0)]
      (doseq [key keys]
        (log/info "DEBUG ZIP processing key:" key)
        (if-let [{:keys [body content-disposition] :as file} (get-file liiteri-cas-client key)]
          (do
            (log/info "DEBUG RAW Content-Disposition for key" key ":" (pr-str content-disposition))
            (let [header-filename (extract-filename content-disposition)]
              (log/info "DEBUG extract-filename ->" (pr-str header-filename))
              (if header-filename
                (let [base-filename  (generate-filename header-filename "")
                      final-filename (if (contains? @seen-filenames base-filename)
                                       (generate-filename header-filename (swap! counter inc))
                                       base-filename)]
                  (swap! seen-filenames conj final-filename)
                  (log/info "DEBUG Adding ZIP entry:" final-filename "for key:" key)
                  (.putNextEntry zip-out (ZipEntry. final-filename))
                  (with-open [file-stream body]
                    (io/copy file-stream zip-out))
                  (.closeEntry zip-out))
                (do
                  (log/error "Missing Content-Disposition header for file key:" key)
                  (log/error "Full file map for key (debug):" (pr-str (select-keys file [:content-disposition])))
                  ;; skip adding this file
                  nil))))
          (log/error "Could not fetch file for key:" key))))))
