(ns ataru.hakukohde.liitteet)

(defn use-kouta-info-for-liite
  [liite required-attachment-type]
  (and (:toimitetaan-erikseen liite)
       (= (:tyyppi liite) required-attachment-type)))

(defn attachment-for-hakukohde
  [attachment-type hakukohde]
  (let [attachments (:liitteet hakukohde)]
    ; TODO Koutan :tyyppi kentässä on versionumero:
    ;      liitetyypitamm_4 vs. liitetyypitamm_4#1
    (first (filter (constantly true) attachments))))

(defn attachment-address
  [liite]
  ; TODO: LOCALIZATION
  (let [toimitusosoite (:toimitusosoite liite)
        street-address (:fi (:osoite toimitusosoite))
        postal-code (:koodiUri (:postinumero toimitusosoite))
        post-office (:fi (:nimi (:postinumero toimitusosoite)))]
    (str
      street-address "\n\n"
      postal-code "\n\n"
      post-office)))
