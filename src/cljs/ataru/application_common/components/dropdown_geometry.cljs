(ns ataru.application-common.components.dropdown-geometry
  "Popupin sijainnin ja koon synkronointi DOM:iin. Popup renderöidään Reactin
  portaalilla suoraan document.bodyyn (ks. dropdown-component), jotta mikään
  esi-isän stacking context ei voi mennä sen päälle — siksi sen sijainti
  kentän suhteen asetetaan täältä eikä tule CSS:n asettelusta.

  Kaksi tilaa:
  - Työpöytä: popup asemoidaan inline-tyyleillä kentän alle (tai yläpuolelle,
    jos alla ei ole tilaa) kentän getBoundingClientRectin mukaan.
  - Mobiilin kokoruututila: popup asemoidaan CSS-muuttujilla samoin kuin
    kentän sisältävä kiinnitetty wrapper (ks. dropdown-component.less
    .a-dropdown-popup--fullscreen ja hakija.less .application__dropdown-
    fullscreen-wrapper), joten ne liikkuvat aina yhdessä."
  (:require [ataru.application-common.components.dropdown-viewport :as viewport]))

;; Sama kuin .a-dropdown-popupin margin-top (dropdown-component.less) —
;; popupin näkyvä yläreuna on tämän verran kentän alareunan alapuolella.
(def ^:private popup-margin-top 4)

;; Kynnys, jonka alle jäävä tila kentän alapuolella kääntää popupin
;; työpöydällä kentän yläpuolelle — muuten popup "mahtuisi" tekniikassa mutta
;; olisi käytännössä liian ahdas.
(def ^:private min-usable-popup-height 80)

;; Työpöydällä popup ei koskaan levene tätä leveämmäksi.
(def ^:private desktop-popup-max-width 450)

;; Sama kuin .a-dropdown-popupin max-height (dropdown-component.less), jota
;; tämä pienentää, kun kentän ala- tai yläpuolella on vähemmän tilaa.
(def ^:private desktop-popup-default-max-height 400)

;; Kutsujan (hakija.less) kokoruututilassa kiinnitetty wrapper, jonka suhteen
;; popup asemoidaan kokoruututilassa.
(def ^:private fullscreen-wrapper-selector ".application__dropdown-fullscreen-wrapper")

(def ^:private desktop-style-props
  ["left" "width" "minWidth" "maxWidth" "top" "bottom" "height" "maxHeight"])

(defn- px [value]
  (str value "px"))

(defn- set-px! [style prop value]
  (aset style prop (px (js/Math.round value))))

(defn- clear-desktop-style! [style]
  (doseq [prop desktop-style-props]
    (aset style prop "")))

;; Kirjoitetaan vain muuttunut arvo, koska kokoruututilan synkronointi ajetaan
;; joka animaatiokehyksellä (ks. start-fullscreen-geometry-loop!).
(defn- set-css-var! [style name value]
  (when (not= (.getPropertyValue style name) value)
    (.setProperty style name value)))

;; ---------------------------------------------------------------------
;; Mobiilin kokoruututila
;; ---------------------------------------------------------------------

;; iOS:n selaimet (Safari ja kaikki muut iOS:llä WebKitin päällä toimivat)
;; eivät pienennä layout viewportia virtuaalinäppäimistön noustessa, vaan
;; panoroivat visuaalista viewportia fokusoidun kentän näkyville, jolloin
;; position: fixed; top: 0 -elementin yläosa jäisi ruudun yläpuolelle piiloon.
;; Siksi wrapper ja popup asemoidaan visuaalisen viewportin mukaan
;; (--a-dropdown-viewport-top/-height).
;;
;; Popupille annetaan lisäksi vain kentän alareunan etäisyys wrapperin
;; yläreunasta (--a-dropdown-field-offset). Absoluuttinen pikseli-top ei ole
;; luotettava iOS Safarissa näppäimistön noustessa, koska
;; getBoundingClientRectin, position: fixedin ja visualViewport.offsetTopin
;; origot eivät silloin aina täsmää; saman mittauksen kahden elementin erotus
;; on origosta riippumaton, ja yhteiset muuttujat siirtävät wrapperin ja
;; popupin aina yhdessä samassa tyylilaskennassa.
(defn- sync-fullscreen-geometry! [popup-el field-el]
  (let [root-style  (.. js/document -documentElement -style)
        popup-style (.-style popup-el)]
    (clear-desktop-style! popup-style)
    (set-css-var! root-style "--a-dropdown-viewport-top" (px (viewport/viewport-top-offset)))
    (set-css-var! root-style "--a-dropdown-viewport-height" (px (viewport/viewport-height)))
    (when-let [wrapper-el (.closest field-el fullscreen-wrapper-selector)]
      (set-css-var! popup-style "--a-dropdown-field-offset"
                    (px (js/Math.round (- (.-bottom (.getBoundingClientRect field-el))
                                          (.-top (.getBoundingClientRect wrapper-el)))))))))

;; Kokoruututilassa kentän sijainti voi muuttua ilman yhtäkään tapahtumaa —
;; esim. infotekstin height-transitio (hakija.less), labelin uudelleenrivitys
;; fontin latauduttua tai iOS:n näppäimistöanimaation loppu, josta
;; visualViewport ei aina ilmoita. Siksi kokoruututilan ajan synkronoidaan joka
;; animaatiokehyksellä; sync-fullscreen-geometry! kirjoittaa vain muuttuneet
;; arvot. Molemmat idempotentteja: kutsutaan jokaisella renderöinnillä.
(defn start-fullscreen-geometry-loop! [raf-id sync-popup-geometry!]
  (when (nil? @raf-id)
    (letfn [(tick []
              (sync-popup-geometry!)
              (reset! raf-id (js/requestAnimationFrame tick)))]
      (reset! raf-id (js/requestAnimationFrame tick)))))

(defn stop-fullscreen-geometry-loop! [raf-id]
  (when-let [id @raf-id]
    (js/cancelAnimationFrame id)
    (reset! raf-id nil)))

;; ---------------------------------------------------------------------
;; Työpöytä
;; ---------------------------------------------------------------------

;; Popup näytetään oletuksena kentän alapuolella, mutta jos siellä ei ole
;; riittävästi tilaa JA yläpuolella on enemmän, kentän yläpuolella (vrt.
;; natiivi <select>) — position: fixed -popupin ruudun ulkopuolelle jäävää
;; osaa ei voisi tuoda näkyviin sivua vierittämällä.
(defn- sync-desktop-geometry! [popup-el field-el]
  (let [style       (.-style popup-el)
        rect        (.getBoundingClientRect field-el)
        vh          (viewport/viewport-height)
        top-offset  (viewport/viewport-top-offset)
        space-below (- (+ top-offset vh) (.-bottom rect) popup-margin-top)
        space-above (- (.-top rect) top-offset popup-margin-top)]
    (.removeProperty style "--a-dropdown-field-offset")
    (clear-desktop-style! style)
    (set-px! style "left" (.-left rect))
    (set-px! style "minWidth" (.-width rect))
    (set-px! style "maxWidth" (min desktop-popup-max-width
                                   (- (viewport/viewport-width) (.-left rect))))
    (if (and (< space-below min-usable-popup-height)
             (> space-above space-below))
      (do (set-px! style "bottom" (+ (- vh (.-top rect)) popup-margin-top))
          (set-px! style "maxHeight" (min (max space-above 0) desktop-popup-default-max-height)))
      (do (set-px! style "top" (.-bottom rect))
          (set-px! style "maxHeight" (min (max space-below 0) desktop-popup-default-max-height))))))

;; ---------------------------------------------------------------------

(defn make-sync-popup-geometry!
  "popup-ref ja field-ref ovat atomeja DOM-solmuihin, mobile? reagent-atom.
  Ankkurina on kenttä (field-ref) eikä komponentin juurielementti, koska
  kokoruututilassa hakija.less venyttää juurielementin täyttämään koko
  jäljellä olevan ruudun."
  [popup-ref field-ref mobile?]
  (fn sync-popup-geometry! []
    (when-let [popup-el @popup-ref]
      (when-let [field-el @field-ref]
        (if @mobile?
          (sync-fullscreen-geometry! popup-el field-el)
          (sync-desktop-geometry! popup-el field-el))))))
