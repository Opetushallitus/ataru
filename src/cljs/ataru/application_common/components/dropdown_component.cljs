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

     ;; Popupin sijoitusankkuri (ks. dropdown-geometry) — erikseen
     ;; root-refistä, koska mobiilin kokoruututilassa hakija.less venyttää
     ;; koko root-refin (.a-dropdown) täyttämään koko jäljellä olevan
     ;; ruudun (ks. application__dropdown-fullscreen-wrapper), mikä oli
     ;; ennen portaalia tarkoituksellista: se antoi TILAN popupille, joka
     ;; oli silloin sen oma flex-lapsi. Nyt popup ei enää ole sen DOM-
     ;; jälkeläinen, joten root-refin reunat eivät enää vastaa kentän
     ;; todellista, näkyvää sijaintia — sen käyttäminen ankkurina asettaisi
     ;; popupin lähelle ruudun alareunaa aina kokoruututilassa.
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

     ;; Reaktiivinen atomi: onko näkymä tällä hetkellä mobiilileveydellä.
     ;; Reaktiivisuus varmistaa, että suunnan vaihto (esim. puhelimen
     ;; kääntäminen) auki olevan listan aikana päivittää heti, käytetäänkö
     ;; kokoruutuesitystä vai ei.
     :mobile?                       mobile?

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
     :outside-click-listener        (listeners/make-outside-click-listener dropdown-id root-ref popup-ref input-ref mobile?)

     ;; Tapahtumakäsittelijä ikkunan koon muutokselle.
     :resize-listener               (listeners/make-resize-listener dropdown-id mobile? sync-popup-geometry!)

     ;; Tapahtumakäsittelijä kokoruutuvalikon taustavierityksen
     ;; estämiselle mobiilissa.
     :fullscreen-touchmove-listener (listeners/make-fullscreen-touchmove-listener root-ref)}))

;; ---------------------------------------------------------------------
;; Elinkaarimetodit
;; ---------------------------------------------------------------------

(defn- mount-dropdown! [{:keys [portal-container] :as context}]
  (reset! portal-container (.createElement js/document "div"))
  (.appendChild (.-body js/document) @portal-container)
  (listeners/attach-listeners! context))

(defn- unmount-dropdown! [{:keys [portal-container] :as context}]
  (listeners/detach-listeners! context)
  (viewport/unlock-body-scroll!)
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
