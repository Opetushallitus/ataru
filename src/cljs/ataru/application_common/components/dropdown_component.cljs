(ns ataru.application-common.components.dropdown-component
  (:require [reagent.core :as reagent]
            [ataru.util :as util]
            [ataru.application-common.components.dropdown-viewport :as viewport]
            [ataru.application-common.components.dropdown-geometry :as geometry]
            [ataru.application-common.components.dropdown-listeners :as listeners]
            [ataru.application-common.components.dropdown-render :as render]))

;; ---------------------------------------------------------------------
;; Yhden komponentti-instanssin refit, atomit ja niistä riippuvat
;; tehdasfunktiot — luodaan kerran komponentin luonnin yhteydessä ja
;; kulkevat sen jälkeen "context"-mappina sekä elinkaarimetodeille että
;; jokaiselle renderöinnille (ks. dropdown-render).
;; ---------------------------------------------------------------------

(defn- make-dropdown-context [dropdown-id]
  (let [input-ref             (atom nil)
        root-ref              (atom nil)
        field-ref             (atom nil)
        option-refs           (atom {})
        popup-ref             (atom nil)
        portal-container      (atom nil)
        mobile?               (reagent/atom (viewport/mobile-viewport?))
        sync-popup-geometry!  (geometry/make-sync-popup-geometry! popup-ref field-ref mobile?)]
    {:dropdown-id                   dropdown-id

     ;; Viittaus näkyvään syötekenttään (<input>-elementti).
     :input-ref                     input-ref

     ;; Viittaus komponentin juurielementtiin (.a-dropdown).
     :root-ref                      root-ref

     ;; Viittaus kentän näkyvään osaan (.a-dropdown-field), joka on popupin
     ;; sijoitusankkuri (ks. dropdown-geometry/make-sync-popup-geometry!).
     :field-ref                     field-ref

     ;; Viittaukset option-id -> DOM-node kutakin renderöityä vaihtoehtoa varten,
     ;; jota move-active-to (ks. dropdown-render) käyttää korostetun
     ;; vaihtoehdon vierittämiseen näkyviin.
     :option-refs                   option-refs

     ;; Viittaus popupin elementtiin (portaalin sisällä), jota
     ;; sync-popup-geometry! käyttää sijainnin/koon asettamiseen.
     :popup-ref                     popup-ref

     ;; Erillinen DOM-solmu popupin portaalikohteeksi (ks. mount-dropdown!
     ;; alempana) — luodaan kerran mountissa ja poistetaan unmountissa,
     ;; jotta useampi tämän komponentin instanssi ei koskaan jaa samaa
     ;; säiliötä.
     :portal-container              portal-container

     ;; Auki olevan valikon ajaksi aria-hiddenillä piilotetut taustan
     ;; elementit alkuperäisine arvoineen (ks. dropdown-aria), nil kun
     ;; mitään ei ole piilotettu.
     :hidden-background             (atom nil)

     ;; Sivun vierityskohta, johon body on lukittu kokoruutuvalikon ajaksi
     ;; (ks. dropdown-viewport/lock-body-scroll!), nil kun ei lukittu.
     :locked-scroll-y               (atom nil)

     ;; Kokoruututilan geometriasilmukan seuraavan animaatiokehyksen id
     ;; (ks. dropdown-geometry/start-fullscreen-geometry-loop!), nil kun
     ;; silmukka ei ole käynnissä.
     :geometry-raf-id               (atom nil)

     ;; Reaktiivinen atomi: onko näkymä tällä hetkellä mobiilileveydellä.
     ;; Reaktiivisuus varmistaa, että suunnan vaihto (esim. puhelimen
     ;; kääntäminen) auki olevan listan aikana päivittää heti, käytetäänkö
     ;; kokoruutuesitystä vai ei.
     :mobile?                       mobile?

     ;; MediaQueryList, jonka change-kuuntelija pitää mobile?:n ajan tasalla
     ;; koko komponentin eliniän ajan (ks. mount-dropdown!) — myös listan
     ;; ollessa kiinni, jolloin globaalit kuuntelijat eivät ole kytkettyinä.
     ;; Muuten suljettuna tehty leveyden muutos jättäisi mobile?:n vanhaksi,
     ;; jolloin on-input-focus ei avaisi listaa mobiilissa ja lista avautuisi
     ;; työpöytätilaan kokoruututilan sijaan.
     :mobile-media-query            (atom nil)

     :mobile-change-listener        (fn mobile-change-listener [_e]
                                      (reset! mobile? (viewport/mobile-viewport?)))

     ;; Funktio, joka synkronoi popupin sijainnin ja koon DOM:iin
     ;; (ks. dropdown-geometry).
     :sync-popup-geometry!          sync-popup-geometry!

     ;; Funktio, joka palauttaa yksittäiselle vaihtoehdolle React-ref-
     ;; callbackin option-refsin päivittämiseksi.
     :register-option-ref           (fn register-option-ref [option-id]
                                      (fn [el]
                                        (if el
                                          (swap! option-refs assoc option-id el)
                                          (swap! option-refs dissoc option-id))))

     ;; Funktio, joka fokusoi syötekentän renderöinnin jälkeen (ks.
     ;; on-trigger-click dropdown-renderissä).
     :focus-input                   (fn focus-input []
                                      (reagent/after-render
                                       (fn []
                                         (when-let [el @input-ref]
                                           (.focus el)))))

     ;; Tapahtumakäsittelijä komponentin ulkopuolelle klikkaamiselle
     :outside-click-listener        (listeners/make-outside-click-listener dropdown-id root-ref popup-ref)}))

;; ---------------------------------------------------------------------
;; Elinkaarimetodit
;; ---------------------------------------------------------------------

(defn- mount-dropdown! [{:keys [portal-container mobile? mobile-media-query
                                mobile-change-listener]}]
  (reset! portal-container (.createElement js/document "div"))
  (.appendChild (.-body js/document) @portal-container)
  (reset! mobile-media-query (viewport/mobile-media-query))
  (.addEventListener @mobile-media-query "change" mobile-change-listener)
  ;; Leveys voi muuttua luonnin ja mountin välillä.
  (reset! mobile? (viewport/mobile-viewport?)))

(defn- unmount-dropdown! [{:keys [portal-container mobile-media-query
                                  mobile-change-listener] :as context}]
  ;; Puretaan auki olevan valikon sivuvaikutukset varmuuden vuoksi myös tässä.
  (render/sync-open-state! context {:expanded?   false
                                    :fullscreen? false})
  (when-let [mql @mobile-media-query]
    (.removeEventListener mql "change" mobile-change-listener))
  (when-let [el @portal-container]
    (.removeChild (.-body js/document) el)))

;; ---------------------------------------------------------------------
;; Pääkomponentti
;; ---------------------------------------------------------------------

(defn dropdown []
  (let [context (make-dropdown-context (util/component-id))]
    (reagent/create-class
      {:component-did-mount    (fn [_this] (mount-dropdown! context))
       :component-will-unmount (fn [_this] (unmount-dropdown! context))
       :reagent-render         (fn [props] (render/render-dropdown context props))})))
