(ns ataru.application-common.components.dropdown-viewport
  "dropdown-componentin viewport- ja mobiiliapurit: näytön koon tunnistus sekä
  sivun vierityksen/lukituksen hallinta. Puhtaita DOM-sivuvaikutuksia, ei
  re-frame-dispatchia eikä komponentin omaa tilaa.")

;; Pidettävä samana kuin @mobile-width component-layout.less:ssä.
(def mobile-max-width 593)

;; Sama kuin .a-dropdown-popupin margin-top (dropdown-component.less) —
;; vähennetään dropdown-geometryssä, koska top on kentän alareunassa mutta
;; popupin näkyvä yläreuna on tämän verran alempana.
(def popup-margin-top 4)

;; Kynnys, jonka alle jäävä tila kentän alapuolella kääntää popupin
;; työpöydällä kentän yläpuolelle (ks. dropdown-geometry) — muuten popup
;; "mahtuisi" tekniikassa mutta olisi käytännössä liian ahdas.
(def min-usable-popup-height 80)

;; Popupin korkeuden alaraja mobiilissa — kentän yläpuolinen sisältö tai
;; avoin näppäimistö voi painaa jäljellä olevan tilan lähelle nollaa.
(def min-mobile-popup-height 100)

(def scroll-to-top-padding 8)

;; Työpöydällä popup ei koskaan levene tätä leveämmäksi (ks. dropdown-geometry).
(def desktop-popup-max-width 450)

;; Sama kuin .a-dropdown-popupin oma CSS-oletus (dropdown-component.less) —
;; dropdown-geometry käyttää tätä vain kun tilaa on vähemmän.
(def desktop-popup-default-max-height 400)

(defn mobile-viewport? []
  (<= (.-innerWidth js/window) mobile-max-width))

(defn viewport-height []
  (if-let [vv (.-visualViewport js/window)]
    (.-height vv)
    (.-innerHeight js/window)))

(defn viewport-width []
  (if-let [vv (.-visualViewport js/window)]
    (.-width vv)
    (.-innerWidth js/window)))

(defn viewport-top-offset []
  (if-let [vv (.-visualViewport js/window)]
    (.-offsetTop vv)
    0))

;; iOS:n selaimet (Safari ja kaikki muut iOS:llä WebKitin päällä toimivat,
;; myös Chrome, Firefox ja sovellusten sisäiset selaimet) eivät pienennä
;; layout viewportia virtuaalinäppäimistön noustessa (interactive-widget=
;; resizes-content ei ole niillä tuettu), vaan panoroivat visuaalista
;; viewportia fokusoidun kentän näkyville. position: fixed; top: 0 -elementti
;; on kiinnitetty layout viewportiin, joten panoroinnin jälkeen sen yläosa
;; (label ja hakukenttä) jää ruudun yläpuolelle piiloon. Panoroinnin määrä
;; riippuu selaimen palkkien korkeudesta ja siitä, kuinka alhaalla kenttä
;; oli ennen avaamista, joten oire vaihtelee selaimen ja avaustavan mukaan.
;; Siksi kokoruutuwrapperi (ks. hakija.less .application__dropdown-
;; fullscreen-wrapper) asemoidaan näiden muuttujien avulla aina täsmälleen
;; visuaalisen viewportin päälle. Muuttujia ei tarvitse poistaa sulkiessa:
;; niitä käytetään vain kokoruututilassa, ja ne päivitetään aina ennen
;; ensimmäistä piirtoa avattaessa (ks. dropdown-geometry).
(defn sync-viewport-css-vars! []
  (let [style (.. js/document -documentElement -style)]
    (.setProperty style "--a-dropdown-viewport-top" (str (viewport-top-offset) "px"))
    (.setProperty style "--a-dropdown-viewport-height" (str (viewport-height) "px"))))

;; Kun kenttä fokusoidaan mobiilissa, vieritetään sivu heti niin, että kentän
;; oma <label> (ei itse syötekenttä/select) asettuu ruudun ylälaitaan
(defn scroll-field-to-top! [label-id]
  (when (and label-id (mobile-viewport?))
    (when-let [label-el (.getElementById js/document label-id)]
      (set! (.. label-el -style -scrollMarginTop) (str scroll-to-top-padding "px"))
      (.scrollIntoView label-el #js {:block "start" :behavior "instant"}))))
