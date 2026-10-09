(ns ataru.application-common.components.dropdown-viewport
  "dropdown-componentin viewport-apurit: näytön koon ja visuaalisen viewportin
  mittaus sekä sivun vierityksen lukitus. Puhtaita
  DOM-sivuvaikutuksia, ei re-frame-dispatchia eikä komponentin omaa tilaa.")

;; Pidettävä samana kuin @mobile-width component-layout.less:ssä.
(def mobile-max-width 593)

(defn mobile-viewport? []
  (<= (.-innerWidth js/window) mobile-max-width))

;; Sama raja kuin mobile-viewport?:ssa (max-width vertaa samaan leveyteen
;; kuin innerWidth, vierityspalkki mukaan lukien).
(defn mobile-media-query []
  (.matchMedia js/window (str "(max-width: " mobile-max-width "px)")))

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

;; Sivun vierityksen lukitus kokoruutuvalikon ajaksi. html:n overflow:
;; hidden (dropdown-component.less) ei yksin riitä iOS:llä: kun eleen
;; ensimmäinen touchmove on päästetty läpi (lista alkaa vierittyä), iOS
;; tekee saman eleen loppujen touchmove-tapahtumien preventDefaultista
;; tehottoman, joten listan reunaan osuva veto tai sen liike-energia
;; ketjuuntuu sivulle. Kiinnitetään siksi itse body paikalleen nykyiseen
;; vierityskohtaan (näyttää samalta kuin ennen) ja palautetaan vierityskohta
;; vapautettaessa. Molemmat idempotentteja: kutsutaan jokaisella renderöinnillä.
(def ^:private body-lock-props ["position" "top" "left" "right" "width"])

(defn lock-body-scroll! [locked-scroll-y]
  (when (nil? @locked-scroll-y)
    (let [scroll-y (.-scrollY js/window)
          style    (.. js/document -body -style)]
      (reset! locked-scroll-y scroll-y)
      (set! (.-position style) "fixed")
      (set! (.-top style) (str (- scroll-y) "px"))
      (set! (.-left style) "0")
      (set! (.-right style) "0")
      (set! (.-width style) "100%"))))

(defn unlock-body-scroll! [locked-scroll-y]
  (when-let [scroll-y @locked-scroll-y]
    (let [style (.. js/document -body -style)]
      (doseq [prop body-lock-props]
        (.removeProperty style prop))
      (reset! locked-scroll-y nil)
      (.scrollTo js/window #js {:top scroll-y :behavior "instant"}))))

(def ^:private scroll-into-view-padding 8)

;; Vierittää sivua vain sen verran, että el näkyy kokonaan visuaalisessa
;; viewportissa. scrollIntoView ei kelpaa: iOS ei pienennä layout viewportia
;; näppäimistön noustessa, joten se pitäisi näppäimistön alle jäävää kenttää
;; näkyvänä.
(defn scroll-into-visual-viewport! [el]
  (when (and el (.-isConnected el))
    (let [rect     (.getBoundingClientRect el)
          top      (+ (viewport-top-offset) scroll-into-view-padding)
          bottom   (- (+ (viewport-top-offset) (viewport-height)) scroll-into-view-padding)]
      (cond
        (> (.-bottom rect) bottom) (.scrollBy js/window 0 (- (.-bottom rect) bottom))
        (< (.-top rect) top)       (.scrollBy js/window 0 (- (.-top rect) top))))))
