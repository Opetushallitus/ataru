(ns ataru.application-common.components.dropdown-listeners
  "dropdown-componentin globaalit DOM-tapahtumakäsittelijät (ulkopuolelle
  klikkaaminen, ikkunan koon muutos ja vieritys, kokoruutuvalikon
  kosketuseleet mobiilissa) sekä niiden kytkeminen ja irrottaminen."
  (:require [re-frame.db :as db]
            [ataru.application-common.components.dropdown-actions :as actions]))

(defn- expanded? [dropdown-id]
  (get-in @db/app-db [:components :dropdown dropdown-id :expanded?] false))

;; ---------------------------------------------------------------------
;; tapahtumien kuuntelijoiden tehdasfunktiot
;; ---------------------------------------------------------------------

;; Mobiilin kokoruututilassa tätä ei tarvita: kentän ja listan ulkopuolelta
;; alkava kosketus perutaan jo touchstartissa (ks. alla), jolloin selain ei
;; tuota siitä mousedown-tapahtumaa lainkaan.
(defn make-outside-click-listener [dropdown-id root-ref popup-ref]
  (fn outside-click-listener [e]
    (let [target  (.-target e)
          ;; Popup on portaalissa eikä @root-refin DOM-jälkeläinen, joten sen
          ;; sisällä klikkaaminen pitää tunnistaa erikseen.
          inside? (or (and @root-ref (.contains @root-ref target))
                      (and @popup-ref (.contains @popup-ref target)))]
      (when (and (not inside?)
                 (expanded? dropdown-id))
        (actions/collapse-dropdown {:dropdown-id dropdown-id})))))

;; Kokoruutuvalikon ollessa auki muu kuin listan oma vieritys estetään
;; kolmessa kerroksessa:
;;
;; 1. CSS:n touch-action (ks. dropdown-component.less html:has(.a-dropdown-
;;    popup--fullscreen)) estää listan ulkopuolelta alkavat eleet ja
;;    nipistyszoomauksen.
;; 2. iOS kuitenkin panoroi visuaalista viewportia näppäimistön ollessa auki
;;    esim. labelista alkavalla vedolla touch-actionista ja touchmoven
;;    preventDefaultista huolimatta — panorointi ehtii alkaa ennen
;;    ensimmäistä peruttavissa olevaa touchmovea. Siksi listan ja kentän
;;    ulkopuolelta alkava kosketus perutaan jo touchstartissa. Kenttä
;;    (syötekenttä, tyhjennys- ja avausnappi) jätetään pois, koska
;;    touchstartin preventDefault estäisi myös sen napautukset; label,
;;    infoteksti ym. eivät napautuksia kokoruututilassa tarvitse.
;; 3. iOS ketjuttaa listan vedon visuaalisen viewportin panoroinniksi, jos
;;    veto ALKAA listan ylä- tai alareunasta reunan yli (overscroll-behavior:
;;    contain ei estä sitä). Kesken vedon reunaan osuminen ei ketjuunnu:
;;    kerran listaa vierittämään lähtenyt veto pysyy listassa ja vain jousta
;;    sen reunalla. Siksi kunkin vedon kohtalo päätetään kerran, sen
;;    ensimmäisessä liikkeen sisältävässä touchmovessa: jos lista voi
;;    vierittyä liikkeen suuntaan, veto sallitaan eikä siihen enää puututa
;;    (touchmoven preventDefault iOS:llä pysäyttäisi vierityksen koko
;;    loppuvedon ajaksi, myös suunnan vaihtuessa), muuten koko veto estetään.
;;    Listan vierityskohtaan ei kosketa, jotta napautus osuu aina siihen
;;    vaihtoehtoon, jota napautettiin. Liian lyhyen, vierittymättömän listan
;;    (ja kentän) touchmove estetään aina.

;; Käynnissä olevan listavedon tila: {:start-y n} kun suunta on vielä
;; ratkaisematta, {:blocked? bool} kun ratkaistu. Kokoruututilassa voi olla
;; kerrallaan vain yksi valikko, joten yksi jaettu tila riittää.
(defonce ^:private popup-gesture (atom nil))

(defn- popup-max-scroll [popup-el]
  (- (.-scrollHeight popup-el) (.-clientHeight popup-el)))

(defn- popup-can-scroll? [popup-el dy]
  (let [scroll-top (.-scrollTop popup-el)]
    ;; Sormi liikkuu alas -> sisältö vierittyy ylöspäin.
    (if (pos? dy)
      (pos? scroll-top)
      ;; 1px toleranssi murto-osaisille vierityskohdille.
      (< scroll-top (dec (popup-max-scroll popup-el))))))

(defn- first-touch-y [e]
  (some-> (aget (.-touches e) 0) .-clientY))

(defn fullscreen-touchstart-listener [e]
  (if (some-> (.-target e) (.closest ".a-dropdown-popup"))
    (reset! popup-gesture {:start-y (first-touch-y e)})
    (when-not (some-> (.-target e) (.closest ".a-dropdown-field"))
      (.preventDefault e))))

(defn- decide-popup-gesture! [popup-el e]
  (let [{:keys [start-y blocked?]} @popup-gesture
        y  (first-touch-y e)
        dy (if (and start-y y) (- y start-y) 0)]
    (cond
      (some? blocked?) blocked?
      ;; Ei vielä liikettä — ratkaistaan seuraavassa touchmovessa.
      (zero? dy)       false
      :else            (let [blocked? (not (popup-can-scroll? popup-el dy))]
                         (reset! popup-gesture {:blocked? blocked?})
                         blocked?))))

(defn fullscreen-touchmove-listener [e]
  (let [popup-el (some-> (.-target e) (.closest ".a-dropdown-popup"))]
    (when (or (nil? popup-el)
              (not (pos? (popup-max-scroll popup-el)))
              (decide-popup-gesture! popup-el e))
      (.preventDefault e))))

;; ---------------------------------------------------------------------
;; kuuntelijoiden kytkeminen/irrottaminen (ks. dropdown-component)
;; ---------------------------------------------------------------------

;; Auki olevan valikon kuuntelijat (myös työpöydällä). Mobiilissa valikko on
;; aina kokoruututilassa, jossa geometriasilmukka (ks. dropdown-geometry/
;; start-fullscreen-geometry-loop!) seuraa visuaalista viewportia joka
;; kehyksellä — visualViewportin omia tapahtumia ei siksi tarvita. mobile?:n
;; ajan tasalla pitää dropdown-componentin matchMedia-kuuntelija; sen
;; muutoksesta seuraava renderöinti synkronoi geometrian, joten resizen
;; tarvitsee vain synkronoida.
(defn attach-global-listeners! [{:keys [outside-click-listener sync-popup-geometry!]}]
  ;; capture-vaiheessa, jotta ulkopuolinen klikkaus ehditään havaita ennen
  ;; kuin kohde-elementin oma click-käsittelijä (esim. toisen kentän
  ;; avausklikkaus) ehtii reagoida.
  (.addEventListener js/document "mousedown" outside-click-listener true)
  (.addEventListener js/window "resize" sync-popup-geometry!)
  (.addEventListener js/document "scroll" sync-popup-geometry!
                      #js {:passive true :capture true}))

(defn detach-global-listeners! [{:keys [outside-click-listener sync-popup-geometry!]}]
  (.removeEventListener js/document "mousedown" outside-click-listener true)
  (.removeEventListener js/window "resize" sync-popup-geometry!)
  (.removeEventListener js/document "scroll" sync-popup-geometry!
                         #js {:passive true :capture true}))

;; Vain kokoruututilan ajan kytketyt kosketuskuuntelijat, joten niiden ei
;; tarvitse itse tarkistaa, onko valikko kokoruututilassa. Tilattomia, joten
;; samat funktioviitteet kelpaavat kaikille instansseille (kokoruututilassa
;; voi olla kerrallaan vain yksi valikko).
(defn attach-fullscreen-listeners! []
  ;; passive: false, jotta preventDefault todella estää selaimen oman
  ;; kosketuskäsittelyn eikä vain kirjaudu ohitetuksi (selaimet olettavat
  ;; dokumenttitason touchstart/touchmove-kuuntelijat oletuksena
  ;; passiivisiksi suorituskykysyistä).
  (.addEventListener js/document "touchstart" fullscreen-touchstart-listener
                      #js {:passive false})
  (.addEventListener js/document "touchmove" fullscreen-touchmove-listener
                      #js {:passive false}))

(defn detach-fullscreen-listeners! []
  (.removeEventListener js/document "touchstart" fullscreen-touchstart-listener
                         #js {:passive false})
  (.removeEventListener js/document "touchmove" fullscreen-touchmove-listener
                         #js {:passive false}))
