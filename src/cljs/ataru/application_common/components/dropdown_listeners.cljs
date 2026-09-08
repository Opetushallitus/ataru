(ns ataru.application-common.components.dropdown-listeners
  "DOM-tapahtumankuuntelijoiden tehdasfunktiot dropdown-componentille
  (ulkopuolelle klikkaaminen, ikkunan koon muuttuminen, kokoruutuvalikon
  taustavieritys mobiilissa) sekä niiden kytkeminen/irrottaminen komponentin
  elinkaaren mukana."
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [ataru.application-common.components.dropdown-viewport :as viewport]
            [ataru.application-common.components.dropdown-actions :as actions]))

;; ---------------------------------------------------------------------
;; tapahtumien kuuntelijoiden tehdasfunktiot
;; ---------------------------------------------------------------------

(defn make-outside-click-listener [dropdown-id root-ref popup-ref input-ref mobile?]
  (fn outside-click-listener [e]
    (let [target (.-target e)
          ;; Mobiilissa label näytetään kentän kanssa venytettynä, eikä sen näpäyttämisen haluta piilottavan pudotusvalikkoa
          mobile-own-label-click? (and @mobile?
                                        (= @input-ref (some-> target (.closest "label") .-control)))
          ;; Popup renderöidään Reactin portaalilla document.bodyyn
          ;; (ks. dropdown-component/dropdown), jotta se ei koskaan jää
          ;; minkään esi-isän stacking contextin alle — se ei siis ole enää
          ;; @root-refin DOM-jälkeläinen, joten sen sisällä klikkaaminen
          ;; pitää tunnistaa erikseen, ettei sitä tulkita ulkopuoliseksi
          ;; klikkaukseksi.
          inside?                 (or (and @root-ref (.contains @root-ref target))
                                      (and @popup-ref (.contains @popup-ref target)))]
      (when (and (not mobile-own-label-click?)
                 (not inside?)
                 @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false]))
        (actions/collapse-dropdown {:dropdown-id dropdown-id})))))

(defn make-resize-listener [dropdown-id mobile? sync-popup-geometry!]
  (fn resize-listener []
    (reset! mobile? (viewport/mobile-viewport?))
    (when @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false])
      (if @mobile?
        (viewport/lock-body-scroll!)
        (reagent/after-render viewport/unlock-body-scroll!)))
    (sync-popup-geometry!)))

;; Kun virtuaalinäppäimistö on auki, mobiiliselaimet voivat "panoroida"
;; visuaalista viewportia pitääkseen fokusoidun kentän näkyvissä — tämä ei
;; ole minkään elementin CSS-overflow-vieritystä (html/body overflow: hidden
;; ei siis auta) eikä touch-action-hallittu ele, vaan selaimen oma reaktio
;; kosketusliikkeeseen fokusoidulla kentällä, eikä se rajoitu kosketuksiin
;; jotka alkavat itse .a-dropdownin sisältä (esim. myös ylätunnisteesta tai
;; kentän omasta <label>-elementistä alkava veto voi laukaista sen). Ainoa
;; luotettava tapa estää se on preventDefault() touchmove-tapahtumasta koko
;; sivulla kokoruutuvalikon ollessa auki, paitsi silloin kun kosketus on
;; itse listan (popup) sisällä, jonka oma sisäinen vieritys pitää säilyttää.
(defn make-fullscreen-touchmove-listener [root-ref]
  (fn fullscreen-touchmove-listener [e]
    (when-let [root @root-ref]
      (when (and (.contains (.-classList root) "a-dropdown--fullscreen")
                 (not (some-> (.-target e) (.closest ".a-dropdown-popup"))))
        (.preventDefault e)))))

;; ---------------------------------------------------------------------
;; kuuntelijoiden kytkeminen/irrottaminen (ks. dropdown-component)
;; ---------------------------------------------------------------------

(defn attach-listeners! [{:keys [outside-click-listener resize-listener
                                  fullscreen-touchmove-listener sync-popup-geometry!]}]
  ;; capture-vaiheessa, jotta ulkopuolinen klikkaus ehditään havaita ennen
  ;; kuin kohde-elementin oma click-käsittelijä (esim. toisen kentän
  ;; avausklikkaus) ehtii reagoida.
  (.addEventListener js/document "mousedown" outside-click-listener true)
  (.addEventListener js/window "resize" resize-listener)
  (.addEventListener js/document "scroll" sync-popup-geometry!
                      #js {:passive true :capture true})
  (when-let [vv (.-visualViewport js/window)]
    (.addEventListener vv "resize" sync-popup-geometry!)
    (.addEventListener vv "scroll" sync-popup-geometry!))
  ;; passive: false, jotta preventDefault todella estää selaimen oman
  ;; kosketuskäsittelyn eikä vain kirjaudu ohitetuksi (selaimet olettavat
  ;; oletuksena touchmove-kuuntelijat passiivisiksi suorituskykysyistä).
  (.addEventListener js/document "touchmove" fullscreen-touchmove-listener
                      #js {:passive false}))

(defn detach-listeners! [{:keys [outside-click-listener resize-listener
                                  fullscreen-touchmove-listener sync-popup-geometry!]}]
  (.removeEventListener js/document "mousedown" outside-click-listener true)
  (.removeEventListener js/window "resize" resize-listener)
  (.removeEventListener js/document "scroll" sync-popup-geometry!
                         #js {:passive true :capture true})
  (when-let [vv (.-visualViewport js/window)]
    (.removeEventListener vv "resize" sync-popup-geometry!)
    (.removeEventListener vv "scroll" sync-popup-geometry!))
  (.removeEventListener js/document "touchmove" fullscreen-touchmove-listener
                         #js {:passive false}))
