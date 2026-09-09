(ns ataru.application-common.components.dropdown-geometry
  "Popupin sijainnin ja koon laskenta ja synkronointi DOM:iin. Popup
  renderöidään Reactin portaalilla suoraan document.bodyyn (ks.
  dropdown-component/dropdown), jotta mikään esi-isän stacking context ei voi
  koskaan mennä sen päälle. Koska se ei siis enää saa sijaintiaan/kokoaan
  ilmaiseksi CSS:llä ankkurinsa suhteen, ne lasketaan tässä JS:llä:
  leveys/sivusijainti kentän oman getBoundingClientRectin mukaan, ja korkeus
  jäljellä olevaan tilaan kentän alapuolella (mobiilin kokoruututilassa
  skaalautuu virtuaalinäppäimistön mukaan niillä selaimilla jotka
  ilmoittavat siitä window.visualViewportin kautta). Työpöydällä tämä on
  tarpeen, koska position: fixed -popup ei enää ole sivun normaalissa
  dokumenttivirrassa (ks. dropdown-component.less) — sivun vierittäminen ei
  siis enää tuo sen mahdollista ruudun ulkopuolelle jäävää osaa näkyviin,
  kuten ennen portaalia, joten sen pitää itse mukautua jäljellä olevaan
  tilaan."
  (:require [ataru.application-common.components.dropdown-viewport :as viewport]))

(defn- set-px! [style prop value]
  (aset style prop (str (js/Math.round value) "px")))

(defn- reset-popup-style! [style]
  (doseq [prop ["left" "width" "minWidth" "maxWidth" "top" "bottom" "height" "maxHeight"]]
    (aset style prop "")))

(defn- set-mobile-geometry! [style rect space-below]
  (set-px! style "top" (.-bottom rect))
  (set-px! style "height" (max viewport/min-mobile-popup-height space-below)))

;; Työpöydällä popup näytetään oletuksena kentän alapuolella, mutta jos
;; siellä ei ole riittävästi tilaa JA yläpuolella on enemmän, näytetään se
;; sen sijaan kentän yläpuolella (vrt. natiivi <select> tai muut popover-
;; komponentit) — muuten sivun alareunan lähellä oleva kenttä jättäisi
;; popupin osittain tai kokonaan näkymän ulkopuolelle, eikä sitä position:
;; fixed -sijoittelun vuoksi voisi enää tuoda näkyviin sivua vierittämällä.
(defn- set-desktop-geometry! [style rect vh space-below space-above]
  (set-px! style "left" (.-left rect))
  (set-px! style "minWidth" (.-width rect))
  (set-px! style "maxWidth" (min viewport/desktop-popup-max-width
                                 (- (viewport/viewport-width) (.-left rect))))
  (if (and (< space-below viewport/min-usable-popup-height)
           (> space-above space-below))
    (do (set-px! style "bottom" (+ (- vh (.-top rect)) viewport/popup-margin-top))
        (set-px! style "maxHeight" (min (max space-above 0) viewport/desktop-popup-default-max-height)))
    (do (set-px! style "top" (.-bottom rect))
        (set-px! style "maxHeight" (min (max space-below 0) viewport/desktop-popup-default-max-height)))))

(defn make-sync-popup-geometry!
  "popup-ref ja field-ref ovat atomeja DOM-solmuihin, mobile? reagent-atom.
  Popupin sijoitusankkuri (field-ref) on erikseen komponentin root-refistä,
  koska mobiilin kokoruututilassa hakija.less venyttää koko root-refin
  (.a-dropdown) täyttämään koko jäljellä olevan ruudun (ks. application__
  dropdown-fullscreen-wrapper), mikä oli ennen portaalia tarkoituksellista:
  se antoi TILAN popupille, joka oli silloin sen oma flex-lapsi. Nyt popup ei
  enää ole sen DOM-jälkeläinen, joten root-refin reunat eivät enää vastaa
  kentän todellista, näkyvää sijaintia — sen käyttäminen ankkurina asettaisi
  popupin lähelle ruudun alareunaa aina kokoruututilassa."
  [popup-ref field-ref mobile?]
  (fn sync-popup-geometry! []
    (when-let [popup-el @popup-ref]
      (when-let [anchor-el @field-ref]
        (let [rect        (.getBoundingClientRect anchor-el)
              style       (.-style popup-el)
              vh          (viewport/viewport-height)
              top-offset  (viewport/viewport-top-offset)
              space-below (- (+ top-offset vh) (.-bottom rect) viewport/popup-margin-top)
              space-above (- (.-top rect) top-offset viewport/popup-margin-top)]
          (reset-popup-style! style)
          (if @mobile?
            (set-mobile-geometry! style rect space-below)
            (set-desktop-geometry! style rect vh space-below space-above)))))))
