(ns ataru.hakukohde.liitteet)

(defn use-kouta-info-for-liite
  [liite required-attachment-type]
  (and (:toimitetaan-erikseen liite)
       (= (:tyyppi liite) required-attachment-type)))
