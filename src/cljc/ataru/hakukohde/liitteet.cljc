(ns ataru.hakukohde.liitteet
  (:require [clojure.string :as string]
            [ataru.translations.texts :as texts]
            [ataru.util :as util]))

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
  (string/replace street-address "\n" ", "))

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

(defn- format-website
  [website]
  (str "[" website "]" "(" website ")"))

(defn- hakukohde-common-address
  [hakukohde]
  (when (:liitteet-onko-sama-toimitusosoite? hakukohde)
    (:liitteiden-toimitusosoite hakukohde)))

(defn- hakukohde-common-deadline
  [hakukohde]
  (when (:liitteet-onko-sama-toimitusaika? hakukohde)
    (:liitteiden-toimitusaika hakukohde)))

(defn- format-address
  [lang address]
  (let [street-address (util/from-multi-lang (:osoite address) lang)
        postal-code    (:koodiUri (:postinumero address))
        post-office    (util/from-multi-lang (:nimi (:postinumero address)) lang)
        website        (:verkkosivu address)]
    (when (not-any? nil? [street-address postal-code post-office])
      (str
        (lang (:toimitusosoite texts/translation-mapping)) ": "
        (format-street-address street-address) ", "
        (format-postal-code postal-code) " "
        (format-post-office post-office)
        (when website
          (str "\n\n" (lang (:verkkosivu texts/translation-mapping)) ": " (format-website website)))))))

(defn attachment-address
  [lang attachment hakukohde]
  (let [address (or (hakukohde-common-address hakukohde) (:toimitusosoite attachment))]
    (format-address lang address)))

(defn attachment-address-with-hakukohde
  [lang attachment hakukohde]
  (let [address (attachment-address lang attachment hakukohde)]
    (str (util/from-multi-lang (:name hakukohde) lang) " - " (util/from-multi-lang (:tarjoaja-name hakukohde) lang) "\n\n" address)))

(defn attachment-deadline
  [lang attachment hakukohde]
  (let [deadline (or (hakukohde-common-deadline hakukohde) (:toimitusaika attachment))]
     (get deadline lang)))
