(ns ataru.application-common.components.dropdown-component
  (:require [clojure.string :as string]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [ataru.util :as util]
            [ataru.application-common.components.dropdown-view :as view]))

;; ---------------------------------------------------------------------
;; Viewport-/mobiiliapurit
;; ---------------------------------------------------------------------

;; Pidettävä samana kuin @mobile-width component-layout.less:ssä.
(def ^:private mobile-max-width 593)

(def ^:private banner-height-mobile 90)

(defn- mobile-viewport? []
  (<= (.-innerWidth js/window) mobile-max-width))

(defn- viewport-height []
  (if-let [vv (.-visualViewport js/window)]
    (.-height vv)
    (.-innerHeight js/window)))

;; Kun kenttä fokusoidaan mobiilissa, vieritetään sivu heti niin, että kentän
;; oma <label> (ei itse syötekenttä/select) asettuu yläbannerin alapuolelle.
(defn- scroll-field-to-top! [label-id]
  (when (and label-id (mobile-viewport?))
    (when-let [label-el (.getElementById js/document label-id)]
      (let [top (.-top (.getBoundingClientRect label-el))]
        (.scrollBy js/window #js {:top (- top banner-height-mobile)
                                   :left 0
                                   :behavior "instant"})))))

;; Kapealla näytöllä auki oleva pudotusvalikko renderöidään venytettynä, jolloin koko sivun vieritys disabloidaan.
(defn- lock-body-scroll! []
  (when (mobile-viewport?)
    (.add (.-classList (.-body js/document)) "a-dropdown-fullscreen-open")))

(defn- unlock-body-scroll! []
  (.remove (.-classList (.-body js/document)) "a-dropdown-fullscreen-open"))

;; ---------------------------------------------------------------------
;; re-frame-dispatchit
;; ---------------------------------------------------------------------

(s/defn collapse-dropdown
  [{:keys [dropdown-id]} :- {:dropdown-id s/Str}]
  ;; unlock-body-scroll! ei saa ajaa synkronisesti tässä: se poistaisi html/
  ;; body:n vieritys-lukon HETI, mutta a-dropdown--fullscreen-luokka (jonka
  ;; poistuminen on se, mikä oikeasti saa kokoruutuvalikon lakkaamasta
  ;; olemasta position: fixed :has()-wrapperin kautta) poistuu vasta kun
  ;; expanded?-tilan muutos on ehtinyt renderöityä. Näiden kahden välissä
  ;; oleva hetki, jolloin vieritys on jo sallittu mutta kokoruutuylitys on
  ;; silti kiinnitetty, aiheutti näkyvän välähdyksen (koko sivu "venyi"
  ;; hetkeksi) valinnan yhteydessä — after-render synkronoi ne samaan
  ;; committiin.
  (reagent/after-render unlock-body-scroll!)
  (re-frame/dispatch [:application-components/collapse-dropdown {:dropdown-id dropdown-id}]))

(s/defn expand-dropdown
  [{:keys [dropdown-id]} :- {:dropdown-id s/Str}]
  (lock-body-scroll!)
  (re-frame/dispatch [:application-components/expand-dropdown {:dropdown-id dropdown-id}]))

;; ---------------------------------------------------------------------
;; tapahtumien kuuntelijoiden tehdasfunktiot
;; ---------------------------------------------------------------------

(defn- make-outside-click-listener [dropdown-id root-ref input-ref mobile?]
  (fn outside-click-listener [e]
    (let [;; Mobiilissa label näytetään kentän kanssa venytettynä, eikä sen näpäyttämisen haluta piilottavan pudotusvalikkoa
          mobile-own-label-click? (and @mobile?
                                 (= @input-ref (some-> (.-target e) (.closest "label") .-control)))]
      (when (and (not mobile-own-label-click?)
                 @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false])
                 @root-ref
                 (not (.contains @root-ref (.-target e))))
        (collapse-dropdown {:dropdown-id dropdown-id})))))

(defn- make-resize-listener [dropdown-id mobile? sync-popup-height!]
  (fn resize-listener []
    (reset! mobile? (mobile-viewport?))
    (when @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false])
      (if @mobile?
        (lock-body-scroll!)
        (reagent/after-render unlock-body-scroll!)))
    (sync-popup-height!)))

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
(defn- make-fullscreen-touchmove-listener [root-ref]
  (fn fullscreen-touchmove-listener [e]
    (when-let [root @root-ref]
      (when (and (.contains (.-classList root) "a-dropdown--fullscreen")
                 (not (some-> (.-target e) (.closest ".a-dropdown-popup"))))
        (.preventDefault e)))))

;; ---------------------------------------------------------------------
;; Näppäimistönavigointi
;; ---------------------------------------------------------------------

(defn- make-on-input-key-down
  [{:keys [dropdown-id expanded? active-index selected-index last-option-index
           active-option open-popup move-active-to set-active-index on-option-click]}]
  (fn on-input-key-down [e]
    (cond
      (= "Escape" (.-key e))
      (do (.preventDefault e)
          (collapse-dropdown {:dropdown-id dropdown-id}))

      (not expanded?)
      (when (#{"ArrowDown" "ArrowUp"} (.-key e))
        (.preventDefault e)
        (open-popup)
        (move-active-to (or selected-index 0)))

      (< last-option-index 0)
      nil

      (= "ArrowDown" (.-key e))
      (do (.preventDefault e)
          (move-active-to (min last-option-index (inc (or active-index -1)))))

      (= "ArrowUp" (.-key e))
      (do (.preventDefault e)
          (if (or (nil? active-index) (zero? active-index))
            (set-active-index nil)
            (move-active-to (dec active-index))))

      (= "Home" (.-key e))
      (do (.preventDefault e)
          (move-active-to 0))

      (= "End" (.-key e))
      (do (.preventDefault e)
          (move-active-to last-option-index))

      (and (= "Enter" (.-key e)) active-option)
      (do (.preventDefault e)
          (on-option-click (:value active-option))))))

;; ---------------------------------------------------------------------
;; Pääkomponentti
;; ---------------------------------------------------------------------

(defn dropdown []
  (let [dropdown-id             (util/component-id)
        input-ref               (atom nil)
        root-ref                (atom nil)
        ;; option-id -> DOM-node kutakin renderöityä vaihtoehtoa varten.
        option-refs             (atom {})
        register-option-ref     (fn register-option-ref [option-id]
                                   (fn [el]
                                     (if el
                                       (swap! option-refs assoc option-id el)
                                       (swap! option-refs dissoc option-id))))
        outside-click-listener  (atom nil)
        ;; Reaktiivinen, jotta suunnan vaihto (esim. puhelimen kääntäminen)
        ;; auki olevan listan aikana päivittää heti, käytetäänkö kokoruutu-
        ;; esitystä vai ei.
        mobile?                 (reagent/atom (mobile-viewport?))
        resize-listener         (atom nil)
        ;; Kokoruutuvalikon listan korkeus lasketaan jäljellä olevaan tilaan:
        ;; kutistuu virtuaalinäppäimistön auki ollessa ja kasvaa takaisin täyteen kokoon sen sulkeutuessa niillä selaimilla jotka ilmoittavat siitä window.visualViewportin kautta.
        popup-ref               (atom nil)
        popup-max-height        (reagent/atom nil)
        viewport-resize-listener (atom nil)
        sync-popup-height!      (fn sync-popup-height! []
                                   (when-let [el @popup-ref]
                                     (let [available (- (viewport-height)
                                                        (.-top (.getBoundingClientRect el)))
                                           available (-> available
                                                        (max 100)
                                                        js/Math.round)]
                                       (when (not= available @popup-max-height)
                                         (reset! popup-max-height available)))))
        focus-input             (fn []
                                   (reagent/after-render
                                     (fn []
                                       (when-let [el @input-ref]
                                         (.focus el)))))
        fullscreen-touchmove-listener (make-fullscreen-touchmove-listener root-ref)]
    (reagent/create-class
      {:component-did-mount
       (fn [_this]
         (reset! outside-click-listener (make-outside-click-listener dropdown-id root-ref input-ref mobile?))
         ;; capture-vaiheessa, jotta ulkopuolinen klikkaus ehditään havaita
         ;; ennen kuin kohde-elementin oma click-käsittelijä (esim. toisen
         ;; kentän avausklikkaus) ehtii reagoida.
         (.addEventListener js/document "mousedown" @outside-click-listener true)

         (reset! resize-listener (make-resize-listener dropdown-id mobile? sync-popup-height!))
         (.addEventListener js/window "resize" @resize-listener)

         (reset! viewport-resize-listener sync-popup-height!)
         (when-let [vv (.-visualViewport js/window)]
           (.addEventListener vv "resize" @viewport-resize-listener)
           (.addEventListener vv "scroll" @viewport-resize-listener))

         ;; passive: false, jotta preventDefault todella estää selaimen oman
         ;; kosketuskäsittelyn eikä vain kirjaudu ohitetuksi (selaimet
         ;; olettavat oletuksena touchmove-kuuntelijat passiivisiksi
         ;; suorituskykysyistä).
         (.addEventListener js/document "touchmove" fullscreen-touchmove-listener
                             #js {:passive false}))

       :component-will-unmount
       (fn [_this]
         (.removeEventListener js/document "mousedown" @outside-click-listener true)
         (.removeEventListener js/window "resize" @resize-listener)
         (when-let [vv (.-visualViewport js/window)]
           (.removeEventListener vv "resize" @viewport-resize-listener)
           (.removeEventListener vv "scroll" @viewport-resize-listener))
         (.removeEventListener js/document "touchmove" fullscreen-touchmove-listener
                                #js {:passive false})
         (unlock-body-scroll!))

       :reagent-render
       (s/fn render-dropdown
         [{:keys [options
                  unselected-label
                  unselected-label-icon
                  selected-value
                  on-change
                  disabled?
                  required?
                  clearable?
                  invalid?
                  id
                  aria-labelledby
                  aria-label
                  data-test-id]} :- {:options                                [view/SelectOptionProps]
                                     :unselected-label                       s/Str
                                     ;; Yksittäinen hiccup-elementti (esim. [:i.zmdi...]), ei merkkijono.
                                     (s/optional-key :unselected-label-icon) s/Any
                                     :selected-value                         (s/maybe s/Str)
                                     :on-change                              s/Any
                                     (s/optional-key :disabled?)             s/Bool
                                     (s/optional-key :required?)             s/Bool
                                     ;; false vain kentille, joiden vaihtoehtolistassa ei koskaan ole
                                     ;; tyhjää/valitsematonta vaihtoehtoa (ks. hakija/components/
                                     ;; dropdown_component.cljs:n no-blank-option) — niille tyhjennys-
                                     ;; nappi olisi ainoa tapa saada kenttä tilaan, jota se ei koskaan
                                     ;; voi luonnostaan olla. Oletuksena (puuttuessaan) tyhjennettävissä,
                                     ;; kuten ennen tämän lipun lisäämistä.
                                     (s/optional-key :clearable?)            s/Bool
                                     (s/optional-key :invalid?)              s/Bool
                                     (s/optional-key :id)                    (s/maybe s/Str)
                                     (s/optional-key :aria-labelledby)       (s/maybe s/Str)
                                     ;; Vaihtoehto aria-labelledby:lle silloin, kun kentällä ei
                                     ;; ole omaa näkyvää <label>-elementtiä (ks. hakija-dropdown).
                                     (s/optional-key :aria-label)            (s/maybe s/Str)
                                     (s/optional-key :data-test-id)          (s/maybe s/Str)}]
         (let [disabled?          (boolean disabled?)
               required?          (boolean required?)
               invalid?           (boolean invalid?)
               lang               @(re-frame/subscribe [:application/form-language])
               expanded?          (and (not disabled?)
                                        @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false]))
               query              @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :query] nil])
               active-index       @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :active-index] nil])
               value->label       (into {} (map (juxt :value :label)) options)
               options-with-id    (view/get-filtered-options-with-id dropdown-id options query)
               active-option      (when (and expanded? active-index)
                                    (get options-with-id active-index))
               selected-index     (view/find-selected-index options-with-id selected-value)
               last-option-index  (dec (count options-with-id))
               label-id           (str dropdown-id "-label")
               listbox-id         (str dropdown-id "-listbox")

               set-active-index   (fn set-active-index [idx]
                                    (re-frame/dispatch [:application-components/set-dropdown-active-index
                                                         {:dropdown-id dropdown-id
                                                          :active-index idx}]))
               ;; Popup on korkeintaan 300px korkea ja vierittyvä, joten pitkässä
               ;; listassa (esim. maat) korostettu vaihtoehto pitää vierittää
               ;; näkyviin.
               move-active-to     (fn move-active-to [idx]
                                    (set-active-index idx)
                                    (when-let [option-id (:option-id (get options-with-id idx))]
                                      (reagent/after-render
                                        (fn []
                                          (when-let [el (get @option-refs option-id)]
                                            (.scrollIntoView el #js {:block "nearest"}))))))
               on-query-change    (fn on-query-change [value]
                                    (re-frame/dispatch [:application-components/set-dropdown-query
                                                         {:dropdown-id dropdown-id
                                                          :query       value}])
                                    (set-active-index nil))
               open-popup         (fn open-popup []
                                    (when-not disabled?
                                      (when-not expanded?
                                        (on-query-change nil)
                                        (scroll-field-to-top! (or aria-labelledby label-id))
                                        (expand-dropdown {:dropdown-id dropdown-id}))))
               ;; Pelkkä näppäimistöfokus (esim. Tab kenttään) ei avaa listaa —
               ;; vain klikkaus, nuolinäppäimet tai kirjoittaminen avaavat sen.
               ;; Klikkaus fokusoi kentän selaimen oletustoiminnolla, joten
               ;; myös virtuaalinäppäimistö nousee esiin normaalisti heti
               ;; ensimmäisestä kosketuksesta mobiilissa.
               on-input-click     (fn on-input-click [_e]
                                    (open-popup))
               on-input-change    (fn on-input-change [e]
                                    (open-popup)
                                    (on-query-change (.. e -target -value)))
               ;; :on-blur juuritasolla (ei pelkässä syötekentässä), jotta vain fokuksen siirtyminen kokonaan komponentin ulkopuolelle sulkee sen.
               on-dropdown-blur   (fn on-dropdown-blur [e]
                                    (let [related-target (.-relatedTarget e)]
                                      (when (or (nil? related-target)
                                                (and @root-ref
                                                     (not (.contains @root-ref related-target))))
                                        (collapse-dropdown {:dropdown-id dropdown-id}))))
               on-option-click    (fn on-option-click [value]
                                    (collapse-dropdown {:dropdown-id dropdown-id})
                                    (on-change value))
               on-input-key-down  (make-on-input-key-down
                                    {:dropdown-id        dropdown-id
                                     :expanded?          expanded?
                                     :active-index       active-index
                                     :selected-index     selected-index
                                     :last-option-index  last-option-index
                                     :active-option      active-option
                                     :open-popup         open-popup
                                     :move-active-to     move-active-to
                                     :set-active-index   set-active-index
                                     :on-option-click    on-option-click})
               on-trigger-click   (fn on-trigger-click [e]
                                    (.preventDefault e)
                                    (when-not disabled?
                                      (if expanded?
                                        (collapse-dropdown {:dropdown-id dropdown-id})
                                        (do (open-popup)
                                            (focus-input)))))
               ;; Mobiilissa listan avaaminen ei saa nojata pelkkään
               ;; klikkaukseen: kentän fokusoituminen käynnistää heti
               ;; virtuaalinäppäimistön animaation, jonka aiheuttama
               ;; asetteluhyppäys kesken kosketuksen voi saada selaimen
               ;; peruuttamaan sitä seuraavan synteettisen click-tapahtuman
               ;; kokonaan — jolloin on-input-click ei koskaan ehtisi ajaa.
               ;; Fokus sen sijaan laukeaa aina aidosti, joten avataan
               ;; valikko jo sen yhteydessä.
               on-input-focus     (fn [_e]
                                    (scroll-field-to-top! (or aria-labelledby label-id))
                                    (when @mobile?
                                      (open-popup)))
               on-clear-click     (fn dropdown-clear-button-clicked []
                                    (on-change ""))
               resolved-aria-labelledby (or aria-labelledby
                                            (when-not aria-label label-id))
               selected-label     (get value->label selected-value)
               button-label       (if-not (string/blank? selected-value)
                                    selected-label
                                    unselected-label)
               input-value        (view/compute-input-value {:expanded?    expanded?
                                                               :query        query
                                                               :button-label button-label})
               fullscreen?        (and expanded? @mobile?)
               ;; Alkuarvo heti avattaessa — sen jälkeen resize/scroll-
               ;; kuuntelijat (ks. component-did-mount) pitävät sen ajan
               ;; tasalla myös näppäimistön sulkeutuessa.
               _                  (when fullscreen?
                                    (reagent/after-render sync-popup-height!))]
           [:div.a-dropdown
              {:ref     #(reset! root-ref %)
               :class   (str (when disabled? "a-dropdown--disabled ")
                              (when fullscreen? "a-dropdown--fullscreen"))
               :on-blur on-dropdown-blur}
              [view/dropdown-field
               {:input-ref             #(reset! input-ref %)
                :id                    id
                :value                 input-value
                :unselected-label      unselected-label
                :unselected-label-icon unselected-label-icon
                :disabled?             disabled?
                :required?             required?
                :invalid?              invalid?
                :data-test-id          data-test-id
                :aria-labelledby       resolved-aria-labelledby
                :aria-label            aria-label
                :expanded?             expanded?
                :listbox-id            listbox-id
                :active-option-id      (:option-id active-option)
                :selected-value        selected-value
                :clearable?            (not (false? clearable?))
                :lang                  lang
                :on-input-click        on-input-click
                :on-input-change       on-input-change
                :on-input-key-down     on-input-key-down
                :on-input-focus        on-input-focus
                :on-clear-click        on-clear-click
                :on-trigger-click      on-trigger-click}]
              [view/dropdown-popup
               {:expanded?        expanded?
                :options-with-id  options-with-id
                :on-click         on-option-click
                :label-id         label-id
                :dropdown-id      dropdown-id
                :selected-value   selected-value
                :active-option-id (:option-id active-option)
                :register-ref     register-option-ref
                :popup-ref        #(reset! popup-ref %)
                :max-height       (when fullscreen? @popup-max-height)
                :lang             lang
                :data-test-id     data-test-id}]]))})))
