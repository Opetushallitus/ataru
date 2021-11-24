(ns ataru.hakukohde.liitteet
  (:require [clojure.string :as string]))

(defn use-kouta-info-for-liite
  [liite required-attachment-type]
  (and (:toimitetaan-erikseen liite)
    (= (:tyyppi liite) required-attachment-type)))

(defn- decode-koodi
  [koodi]
  (string/split koodi #"#"))

(defn- attachment-tyyppi
  [attachment]
  (let [koodi (:tyyppi attachment)
        [tyyppi _versio] (decode-koodi koodi)]
    tyyppi))

(defn attachment-for-hakukohde
  [attachment-type hakukohde]
  (let [attachments (:liitteet hakukohde)]
    (first (filter #(= attachment-type (attachment-tyyppi %)) attachments))))

(defn- format-street-address
  [street-address]
  (string/replace street-address "\n" "\n\n"))

(defn- format-postal-code
  [postal-code]
  ; postal-code is formatted as posti_02150#2
  (-> postal-code
    (string/split #"#")
    (first)
    (string/split #"_")
    (second)))

(defn- format-post-office
  [post-office]
  post-office)

(defn attachment-address
  [liite]
  ; TODO: LOCALIZATION
  (let [toimitusosoite (:toimitusosoite liite)
        street-address (:fi (:osoite toimitusosoite))
        postal-code    (:koodiUri (:postinumero toimitusosoite))
        post-office    (:fi (:nimi (:postinumero toimitusosoite)))]
    (when (not-any? nil? [street-address postal-code post-office])
      (str
        (format-street-address street-address) "\n\n"
        (format-postal-code postal-code) "\n\n"
        (format-post-office post-office)))))
