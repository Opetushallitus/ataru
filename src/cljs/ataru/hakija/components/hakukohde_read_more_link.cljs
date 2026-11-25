(ns ataru.hakija.components.hakukohde-read-more-link
  (:require [re-frame.core :refer [subscribe]]
            [ataru.application-common.components.link-component :as link-component]
            [ataru.translations.translation-util :as translations]))

(defn read-more-link
  "Lue lisää -linkki hakukohteen toteutuksen tietoihin Opintopolun koulutustarjonnassa.
   Linkki avautuu uuteen välilehteen. Ruudunlukijalle linkin nimeen lisätään hakukohteen nimi,
   jotta useat Lue lisää -linkit erottuvat toisistaan. Linkkiä ei näytetä, jos hakukohteella
   ei ole toteutusta, johon linkittää."
  [hakukohde-oid hakukohde-name]
  (let [lang @(subscribe [:application/form-language])
        url  @(subscribe [:application/hakukohde-konfo-url-by-oid hakukohde-oid])]
    (when url
      (let [read-more (translations/get-hakija-translation :read-more lang)]
        [:a.application__hakukohde-read-more-link
         (cond-> {:href   url
                  :target "_blank"
                  :rel    "noopener noreferrer"}
           hakukohde-name
           (assoc :aria-label (str read-more ": " hakukohde-name " "
                                   (translations/get-hakija-translation :opens-in-new-tab lang))))
         [:i.zmdi.zmdi-open-in-new {:aria-hidden true}]
         read-more
         (when-not hakukohde-name
           [link-component/new-tab-hint lang])]))))
