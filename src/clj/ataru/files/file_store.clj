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

(def content-disposition-filename-regex
  ;; Matches both filename="foo.pdf" and filename*=UTF-8''foo.pdf (RFC 5987)
  #"(?i)(?:filename\*?=(?:UTF-8''|\"?))([^\";]+)")

(defn extract-filename [content-disposition]
  (when content-disposition
    (some->> content-disposition
             (re-find content-disposition-filename-regex)
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

(defn get-file-zip [liiteri-cas-client file-keys output-stream]
  (with-open [zip-out (ZipOutputStream. output-stream)]
    (let [seen-filenames (atom #{})
          counter (atom 0)]
      (doseq [file-key file-keys]
        (if-let [{:keys [body content-disposition]} (get-file liiteri-cas-client file-key)]
          (let [header-filename      (extract-filename content-disposition)
                fallback-filename    (or header-filename (str file-key))
                base-filename        (generate-filename fallback-filename 0)
                unique-filename      (if (contains? @seen-filenames base-filename)
                                       (generate-filename fallback-filename (swap! counter inc))
                                       base-filename)]

            ;; Track used filenames to prevent duplicates
            (swap! seen-filenames conj unique-filename)

            ;; Log for debugging
            (log/info "DEBUG Adding ZIP entry:" unique-filename)

            ;; Write file into ZIP
            (.putNextEntry zip-out (ZipEntry. unique-filename))
            (with-open [file-stream body]
              (io/copy file-stream zip-out))
            (.closeEntry zip-out))

          ;; Error if file fetch failed
          (log/error "Could not fetch file for key:" file-key))))))
