(ns ataru.application-common.components.dropdown-viewport
  "dropdown-componentin viewport- ja mobiiliapurit: näytön koon tunnistus sekä
  sivun vierityksen/lukituksen hallinta. Puhtaita DOM-sivuvaikutuksia, ei
  re-frame-dispatchia eikä komponentin omaa tilaa.")

;; Pidettävä samana kuin @mobile-width component-layout.less:ssä.
(def mobile-max-width 593)

;; Pidettävä samana kuin .a-dropdown-popupin margin-top dropdown-component.
;; less:ssä — vähennetään käytettävissä olevasta tilasta dropdown-geometry/
;; make-sync-popup-geometry!:ssä, koska top asetetaan kentän alareunaan mutta
;; popupin todellinen näkyvä alareuna on vielä tämän marginaalin verran
;; alempana.
(def popup-margin-top 4)

;; Kynnys, jonka alle jäävä tila kentän alapuolella saa työpöydällä laukaista
;; popupin kääntämisen kentän yläpuolelle (ks. dropdown-geometry) — jos tätä
;; ei tehtäisi, kentän alapuolelle jäisi tilaa vielä sen verran, että popup
;; näyttäisi teknisesti "mahtuvan", mutta käytännössä liian vähän että käyttäjä näkisi vaihtoehtoja.
(def min-usable-popup-height 80)

(def scroll-to-top-padding 8)

(defn mobile-viewport? []
  (<= (.-innerWidth js/window) mobile-max-width))

(defn viewport-height []
  (if-let [vv (.-visualViewport js/window)]
    (.-height vv)
    (.-innerHeight js/window)))

;; Kun kenttä fokusoidaan mobiilissa, vieritetään sivu heti niin, että kentän
;; oma <label> (ei itse syötekenttä/select) asettuu ruudun ylälaitaan
(defn scroll-field-to-top! [label-id]
  (when (and label-id (mobile-viewport?))
    (when-let [label-el (.getElementById js/document label-id)]
      (set! (.. label-el -style -scrollMarginTop) (str scroll-to-top-padding "px"))
      (.scrollIntoView label-el #js {:block "start" :behavior "instant"}))))

(defn- scrolling-element []
  (or (.-scrollingElement js/document) (.-documentElement js/document)))

;; Kapealla näytöllä auki oleva pudotusvalikko renderöidään venytettynä, jolloin koko sivun vieritys disabloidaan.
(defn lock-body-scroll! []
  (when (mobile-viewport?)
    (.add (.-classList (scrolling-element)) "a-dropdown-fullscreen-open")))

(defn unlock-body-scroll! []
  (.remove (.-classList (scrolling-element)) "a-dropdown-fullscreen-open"))
