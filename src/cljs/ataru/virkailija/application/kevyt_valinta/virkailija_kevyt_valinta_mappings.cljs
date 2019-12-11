(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-mappings)

(defn kevyt-valinta-property->valinta-tulos-service-property [kevyt-valinta-property]
  (case kevyt-valinta-property
    :kevyt-valinta/valinnan-tila :valinnantila
    :kevyt-valinta/julkaisun-tila :julkaistavissa
    :kevyt-valinta/vastaanotto-tila :vastaanottotila
    :kevyt-valinta/ilmoittautumisen-tila :ilmoittautumistila))
