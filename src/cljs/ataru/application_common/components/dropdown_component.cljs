(ns ataru.application-common.components.dropdown-component
  (:require [clojure.string :as string]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [schema-tools.core :as st]
            [ataru.translations.translation-util :as translations]
            [ataru.util :as util]))

(s/defschema SelectOptionProps
  {:value s/Str
   :label s/Str})

;; Pidettävä samana kuin @mobile-width component-layout.less:ssä.
(def ^:private mobile-max-width 593)

(def ^:private banner-height-mobile 90)

(defn- mobile-viewport? []
  (<= (.-innerWidth js/window) mobile-max-width))

;; window.visualViewport kutistuu virtuaalinäppäimistön auki ollessa niillä
;; selaimilla, jotka tukevat sitä — window.innerHeight ja vh-yksiköt eivät
;; reagoi näppäimistöön ollenkaan millään selaimella, koska CSS:n
;; spesifikaatio jättää näppäimistön tarkoituksella pois viewport-mittojen
;; laskennasta.
(defn- viewport-height []
  (if-let [vv (.-visualViewport js/window)]
    (.-height vv)
    (.-innerHeight js/window)))

;; Kun kenttä fokusoidaan mobiilissa, vieritetään sivu heti niin, että kentän
;; oma <label> (ei itse syötekenttä/select) asettuu ylätunnisteen alapuolelle
;; — näin sekä otsikko että kenttä pysyvät näkyvissä, eikä pelkkä selaimen
;; oma "scroll into view" -käytös (esim. virtuaalinäppäimistön avautuessa)
;; jätä niitä johonkin muuhun, vaikeammin ennustettavaan kohtaan. Itse kenttää
;; tai labelia ei muuteta millään tavalla — ainoastaan sivun vieritys.
(defn- scroll-field-to-top! [label-id]
  (when (and label-id (mobile-viewport?))
    (when-let [label-el (.getElementById js/document label-id)]
      (let [top (.-top (.getBoundingClientRect label-el))]
        (.scrollBy js/window #js {:top (- top banner-height-mobile)
                                   :left 0
                                   :behavior "instant"})))))

;; Kapealla näytöllä auki oleva pudotusvalikko renderöidään kokoruutuna (ks.
;; render-dropdownin loppu ja a-dropdown--fullscreen dropdown-component.less:
;; ssä) — taustan vieritys lukitaan samaksi ajaksi, ettei sivu vieritykin
;; samanaikaisesti listan sisäisen vierityksen kanssa. Luokka lisätään sekä
;; <html>:iin että <body>:iin, koska standards-modessa (tämän sovelluksen
;; tila) sivun todellinen vierittyvä elementti on <html>, ei <body> — pelkkä
;; body.overflow:hidden ei siis riitä lukitsemaan mitään, vaikka se on
;; historiallisesti totuttu tapa kirjoittaa tämä.
(defn- lock-body-scroll! []
  (when (mobile-viewport?)
    (.add (.-classList (.-documentElement js/document)) "a-dropdown-fullscreen-open")
    (.add (.-classList (.-body js/document)) "a-dropdown-fullscreen-open")))

(defn- unlock-body-scroll! []
  (.remove (.-classList (.-documentElement js/document)) "a-dropdown-fullscreen-open")
  (.remove (.-classList (.-body js/document)) "a-dropdown-fullscreen-open"))

(s/defn dropdown-caret
  [{:keys [expanded?]} :- {:expanded? s/Bool}]
  [:span.a-dropdown-caret
   {:aria-hidden true
    :class       (when expanded? "a-dropdown-caret--expanded")}])

(s/defn dropdown-clear-button
  [{:keys [on-click lang]} :- {:on-click s/Any
                                :lang     s/Keyword}]
  [:button.a-dropdown-clear-button
   {:type          "button"
    :tab-index     "-1"
    :aria-label    (translations/get-hakija-translation :clear lang)
    ;; Ilman tätä hiiren/kosketuksen painallus siirtää fokuksen napista
    ;; syötekentästä nappiin ennen kuin click ehtii tapahtua. Koska nappi
    ;; poistuu DOM:sta heti valinnan tyhjennyttyä (renderöidään vain kun
    ;; arvo on valittu), fokusoidun napin poistaminen laukaisee selaimen
    ;; oman blur-tapahtuman (relatedTarget null), jonka juuritason
    ;; on-dropdown-blur tulkitsee fokuksen poistumisena koko komponentista
    ;; ja sulkee kokoruutuvalikon (ja vapauttaa vieritys-lukon) hetkeksi.
    :on-mouse-down (fn [e] (.preventDefault e))
    :on-click      (fn dropdown-clear-button-on-click [e]
                     (.stopPropagation e)
                     (on-click))}
   [:i.zmdi.zmdi-close {:aria-hidden true}]])

(s/defn dropdown-select-option
  [{:keys [value
           label]} :- SelectOptionProps]
  [:option {:value value} label])

(s/defn dropdown-select
  [{:keys [expanded?
           options
           unselected-label
           selected-value
           on-click
           dropdown-id
           disabled?
           id
           data-test-id
           on-change]} :- {:expanded?                     s/Bool
                           :options                       [SelectOptionProps]
                           :unselected-label              s/Str
                           :selected-value                (s/maybe s/Str)
                           :on-click                      s/Any
                           :dropdown-id                   s/Str
                           (s/optional-key :disabled?)    s/Bool
                           (s/optional-key :id)           (s/maybe s/Str)
                           (s/optional-key :data-test-id) (s/maybe s/Str)
                           :on-change                     s/Any}]
  [:div.a-dropdown-select-container
   [:select.a-dropdown-select
    {:aria-hidden  true
     :disabled     (boolean disabled?)
     :id           id
     :data-test-id data-test-id
     :on-click     on-click
     :on-change    (fn dropdown-select-on-change [event]
                     (let [value (.. event -target -value)]
                       (on-change value)))
     :value        (or selected-value "")}
    [dropdown-select-option
     {:value ""
      :label unselected-label}]
    (map-indexed (fn [option-idx option-props]
                   (let [key (str "dropdown-select-" dropdown-id "-option-" option-idx)]
                     ^{:key key}
                     [dropdown-select-option option-props]))
                 options)]
   [dropdown-caret
    {:expanded? expanded?}]])

(s/defn dropdown-list-option
  [{:keys [value
           label
           on-click
           option-id
           selected-value
           active?
           register-ref
           data-test-id]} :- (st/assoc
                               SelectOptionProps
                               :on-click s/Any
                               :option-id s/Str
                               :selected-value (s/maybe s/Str)
                               :active? s/Bool
                               :register-ref s/Any
                               :data-test-id (s/maybe s/Str))]
  (let [selected? (= selected-value value)]
    [:li.a-dropdown-list__option
     {:id            option-id
      :ref           (register-ref option-id)
      :class         (str (when selected? "a-dropdown-list__option--selected ")
                           (when active? "a-dropdown-list__option--active"))
      ;; Hiiren painallus (mousedown) ei saa siirtää fokusta pois syötekentästä
      ;; ennen kuin varsinainen valinta (click) ehtii tapahtua.
      :on-mouse-down (fn [e] (.preventDefault e))
      :on-click      (fn dropdown-list-option-on-click []
                       (on-click value))
      :role          "option"
      :aria-selected (when selected?
                       true)
      :data-test-id  data-test-id
      :tab-index     "-1"}
     [:span.a-dropdown-list__option-label label]]))

;; Yksi Popup, jonka sisällä joko List (Item per vaihtoehto) tai Empty-tila.
;;
;; aria-activedescendant EI ole tässä listboxissa, vaan syötekentässä (ks.
;; render-dropdown) — ARIA-yhdistelmäruutumallissa se kuuluu sille elementille,
;; jolla on todellinen näppäimistöfokus, ei sille jota se osoittaa.
(s/defn dropdown-popup
  [{:keys [expanded?
           options-with-id
           on-click
           label-id
           dropdown-id
           selected-value
           active-option-id
           register-ref
           popup-ref
           max-height
           lang
           data-test-id]} :- {:expanded?                            s/Bool
                              :options-with-id                      [(st/assoc SelectOptionProps :option-id s/Str)]
                              :on-click                             s/Any
                              :label-id                             s/Str
                              :dropdown-id                          s/Str
                              :selected-value                       (s/maybe s/Str)
                              (s/optional-key :active-option-id)    (s/maybe s/Str)
                              :register-ref                         s/Any
                              (s/optional-key :popup-ref)           s/Any
                              ;; visualViewportista laskettu, jäljellä olevaan
                              ;; tilaan mukautuva korkeus ylikirjoittaa CSS:n
                              ;; staattisen max-heightin (ks. render-dropdown/
                              ;; sync-popup-height!), ettei lista mene
                              ;; virtuaalinäppäimistön alle.
                              (s/optional-key :max-height)          (s/maybe s/Int)
                              :lang                                 s/Keyword
                              :data-test-id                         (s/maybe s/Str)}]
  (let [listbox-id (str dropdown-id "-listbox")]
    [:div.a-component.a-dropdown-popup
     {:ref          popup-ref
      :data-test-id (str data-test-id "-list")
      :tab-index    "-1"
      :style        (when max-height
                      {:max-height max-height})
      :class        (when-not expanded?
                      "a-dropdown-popup--collapsed")}
     (if (empty? options-with-id)
       [:p.a-dropdown-empty
        (translations/get-hakija-translation :no-dropdown-search-hits lang)]
       [:ul.a-dropdown-list
        {:id              listbox-id
         :aria-labelledby label-id
         :tab-index        "-1"
         :role            "listbox"}
        (map-indexed (fn [option-idx option-props]
                       (let [key (str "dropdown-list-" dropdown-id "-option-" option-idx)]
                         ^{:key key}
                         [dropdown-list-option (merge option-props
                                                      {:on-click       on-click
                                                       :selected-value selected-value
                                                       :active?        (= active-option-id (:option-id option-props))
                                                       :register-ref   register-ref
                                                       :data-test-id   (str data-test-id "-option-" (:value option-props))})]))
                     options-with-id)])]))

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

(defn dropdown []
  (let [dropdown-id             (util/component-id)
        input-ref               (atom nil)
        root-ref                (atom nil)
        ;; Kun kenttää kosketetaan uudestaan listan ollessa jo auki (ks.
        ;; on-input-click), kenttä sumennetaan ja fokusoidaan heti uudestaan
        ;; pakottaaksemme aidon fokusoitumistapahtuman, jotta virtuaali-
        ;; näppäimistö nousee esiin (pelkkä jo-fokusoidun kentän fokusointi
        ;; uudestaan ei riitä). Tämä sumennus kuitenkin kuplii juuritason
        ;; on-dropdown-bluriin asti, joka muuten tulkitsisi sen "fokus
        ;; poistui koko komponentista" -signaaliksi ja sulkisi listan —
        ;; tämä lippu ohittaa sen väliaikaisesti sumennuksen ja uudelleen-
        ;; fokusoinnin ajaksi.
        suppress-blur-close?    (atom false)
        ;; option-id -> DOM-node kutakin renderöityä vaihtoehtoa varten.
        ;; Käytetään React:in itsensä ylläpitämiä :ref-kutsuja document-tason
        ;; getElementById-haun sijaan, koska React 18:n createRoot-juuren
        ;; kanssa automaattinen batching voi ajaa reagent/after-renderin
        ;; suhteessa eri committiin kuin mihin getElementById-haku osuisi —
        ;; :ref sen sijaan osoittaa aina siihen DOM-solmuun, jonka React
        ;; viimeksi todella committasi tälle tietylle elementille.
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
        ;; Kokoruutuvalikon listan korkeus lasketaan jäljellä olevaan tilaan
        ;; (ks. sync-popup-height! ja viewport-resize-listener) sen sijaan
        ;; että se olisi kiinteä — kutistuu virtuaalinäppäimistön auki
        ;; ollessa ja kasvaa takaisin täyteen kokoon sen sulkeutuessa,
        ;; niillä selaimilla jotka ilmoittavat siitä window.visualViewportin
        ;; kautta.
        popup-ref               (atom nil)
        popup-max-height        (reagent/atom nil)
        viewport-resize-listener (atom nil)
        sync-popup-height!      (fn sync-popup-height! []
                                   (when-let [el @popup-ref]
                                     (let [available (- (viewport-height)
                                                        (.-top (.getBoundingClientRect el))
                                                        8)
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
        ;; Kun virtuaalinäppäimistö on auki, mobiiliselaimet voivat "panoroida"
        ;; visuaalista viewportia pitääkseen fokusoidun kentän näkyvissä — tämä
        ;; ei ole minkään elementin CSS-overflow-vieritystä (html/body
        ;; overflow: hidden ei siis auta) eikä touch-action-hallittu ele, vaan
        ;; selaimen oma reaktio kosketusliikkeeseen fokusoidulla kentällä.
        ;; Ainoa luotettava tapa estää se on preventDefault() touchmove-
        ;; tapahtumasta, paitsi silloin kun kosketus on itse listan (popup)
        ;; sisällä, jonka oma sisäinen vieritys pitää säilyttää.
        fullscreen-touchmove-listener
        (fn [e]
          (when-let [root @root-ref]
            (when (and (.contains (.-classList root) "a-dropdown--fullscreen")
                       (.contains root (.-target e))
                       (not (some-> (.-target e) (.closest ".a-dropdown-popup"))))
              (.preventDefault e))))]
    (reagent/create-class
      {:component-did-mount
       (fn [_this]
         (reset! outside-click-listener
                 (fn [e]
                   (when (and @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false])
                              @root-ref
                              (not (.contains @root-ref (.-target e))))
                     (collapse-dropdown {:dropdown-id dropdown-id}))))
         ;; capture-vaiheessa, jotta ulkopuolinen klikkaus ehditään havaita
         ;; ennen kuin kohde-elementin oma click-käsittelijä (esim. toisen
         ;; kentän avausklikkaus) ehtii reagoida.
         (.addEventListener js/document "mousedown" @outside-click-listener true)

         (reset! resize-listener
                 (fn [] (reset! mobile? (mobile-viewport?))))
         (.addEventListener js/window "resize" @resize-listener)

         (reset! viewport-resize-listener sync-popup-height!)
         (.addEventListener js/window "resize" @viewport-resize-listener)
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
         (.removeEventListener js/window "resize" @viewport-resize-listener)
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
                  invalid?
                  id
                  aria-labelledby
                  data-test-id]} :- {:options                                [SelectOptionProps]
                                     :unselected-label                       s/Str
                                     ;; Yksittäinen hiccup-elementti (esim. [:i.zmdi...]), ei merkkijono.
                                     (s/optional-key :unselected-label-icon) s/Any
                                     :selected-value                         (s/maybe s/Str)
                                     :on-change                              s/Any
                                     (s/optional-key :disabled?)             s/Bool
                                     (s/optional-key :required?)             s/Bool
                                     (s/optional-key :invalid?)              s/Bool
                                     (s/optional-key :id)                    (s/maybe s/Str)
                                     (s/optional-key :aria-labelledby)       (s/maybe s/Str)
                                     (s/optional-key :data-test-id)          (s/maybe s/Str)}]
         (let [disabled?          (boolean disabled?)
               lang               @(re-frame/subscribe [:application/form-language])
               expanded?          (and (not disabled?)
                                        @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false]))
               query              @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :query] nil])
               active-index       @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :active-index] nil])
               options-with-id    (->> (if (string/blank? query)
                                         options
                                         (let [query-lower (string/lower-case query)]
                                           (filter (fn [{:keys [label]}]
                                                    (string/includes? (string/lower-case label) query-lower))
                                                  options)))
                                       (map-indexed (fn [option-idx option-props]
                                                     (assoc option-props
                                                            :option-id
                                                            (str dropdown-id "-option-" option-idx))))
                                       vec)
               active-option      (when (and expanded? active-index)
                                    (get options-with-id active-index))
               selected-index     (when-not (string/blank? selected-value)
                                    (->> options-with-id
                                        (keep-indexed (fn [idx {:keys [value]}]
                                                       (when (= value selected-value) idx)))
                                        first))
               set-active-index   (fn set-active-index [idx]
                                    (re-frame/dispatch [:application-components/set-dropdown-active-index
                                                         {:dropdown-id dropdown-id
                                                          :active-index idx}]))
               ;; Popup on korkeintaan 300px korkea ja vierittyvä, joten pitkässä
               ;; listassa (esim. maat) korostettu vaihtoehto pitää vierittää
               ;; näkyviin, muuten se katoaa näkymästä muutaman nuolinäppäimen
               ;; painalluksen jälkeen.
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
                                        (expand-dropdown {:dropdown-id dropdown-id}))))
               ;; Pelkkä näppäimistöfokus (esim. Tab kenttään) ei avaa listaa —
               ;; vain klikkaus, nuolinäppäimet tai kirjoittaminen avaavat sen.
               ;;
               ;; Mobiilissa ensimmäinen kosketus (expanded? on vielä false
               ;; tässä, koska se on tämän renderin, siis ENNEN klikkausta,
               ;; arvo) avaa vain listan: kenttä on silloin read-only (ks.
               ;; :read-only alempana), joten kosketus fokusoi sen ilman että
               ;; virtuaalinäppäimistö nousee esiin. Kun lista on JO auki,
               ;; kosketus kentän päällä tulkitaan haluksi kirjoittaa/hakea —
               ;; kenttä ei ole silloin enää read-only, mutta koska se on jo
               ;; fokusoitu, pelkkä sen fokusointi uudestaan ei toisi näppäi-
               ;; mistöä esiin (selain päättää sen näkyvyyden vain aidosta,
               ;; uudesta fokusoitumisesta) — sumennetaan siis ensin ja
               ;; fokusoidaan sitten uudestaan.
               on-input-click     (fn on-input-click [_e]
                                    (let [was-expanded? expanded?]
                                      (open-popup)
                                      (when (and @mobile? was-expanded?)
                                        (reset! suppress-blur-close? true)
                                        (when-let [el @input-ref]
                                          (.blur el))
                                        (reagent/after-render
                                          (fn []
                                            (when-let [el @input-ref]
                                              (.focus el))
                                            (reset! suppress-blur-close? false))))))
               on-input-change    (fn on-input-change [e]
                                    (open-popup)
                                    (on-query-change (.. e -target -value)))
               ;; :on-blur juuritasolla (ei pelkässä syötekentässä), jotta
               ;; sarkaimella siirtyminen syötekentän ja tyhjennysnapin välillä
               ;; ei sulje listaa — vain fokuksen siirtyminen kokonaan
               ;; komponentin ulkopuolelle sulkee sen.
               on-dropdown-blur   (fn on-dropdown-blur [e]
                                    (when-not @suppress-blur-close?
                                      (let [related-target (.-relatedTarget e)]
                                        (when (or (nil? related-target)
                                                  (and @root-ref
                                                       (not (.contains @root-ref related-target))))
                                          (collapse-dropdown {:dropdown-id dropdown-id})))))
               on-option-click    (fn on-option-click [value]
                                    (collapse-dropdown {:dropdown-id dropdown-id})
                                    (on-change value))
               last-option-index  (dec (count options-with-id))
               on-input-key-down  (fn on-input-key-down [e]
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
                                          (on-option-click (:value active-option)))))
               on-trigger-click   (fn on-trigger-click [e]
                                    (.preventDefault e)
                                    (when-not disabled?
                                      (if expanded?
                                        (collapse-dropdown {:dropdown-id dropdown-id})
                                        (do (open-popup)
                                            ;; Ei fokusoida eksplisiittisesti
                                            ;; mobiilissa: :read-only on jo
                                            ;; poistunut siihen mennessä kun
                                            ;; focus-input (after-render)
                                            ;; ehtii ajaa, joten .focus()
                                            ;; toisi virtuaalinäppäimistön
                                            ;; esiin heti ensimmäisestä
                                            ;; avauksesta (ks. on-input-click).
                                            (when-not @mobile?
                                              (focus-input))))))
               label-id           (str dropdown-id "-label")
               listbox-id         (str dropdown-id "-listbox")
               selected-label     (->> options
                                       (filter (fn filter-dropdown-select-option [{option-value :value}]
                                                 (= option-value selected-value)))
                                       (map :label)
                                       first)
               button-label       (if-not (string/blank? selected-value)
                                    selected-label
                                    unselected-label)
               ;; query on nil kunnes käyttäjä muokkaa kenttää (myös heti
               ;; avattaessa, ks. open-popup) — silloin näytetään nykyinen
               ;; valinta, mutta listaa ei silti suodateta (blank query = ei
               ;; suodatusta). Vasta kun käyttäjä kirjoittaa jotain (myös
               ;; tyhjäksi asti poistaen, jolloin query on "" eikä nil),
               ;; kenttä näyttää kirjoitetun haun.
               input-value        (if (and expanded? (some? query))
                                    query
                                    (or button-label ""))
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
              [:div.a-dropdown-field.a-component
             (when (seq unselected-label-icon)
               [:span.a-dropdown-field__icon unselected-label-icon])
             [:input.a-dropdown-input
              {:ref                  #(reset! input-ref %)
               :type                 "text"
               :value                input-value
               :placeholder          unselected-label
               :disabled             disabled?
               ;; Estävät virtuaalinäppäimistön avautumisen kentän
               ;; ensimmäisestä kosketuksesta mobiilissa (ks. on-input-click)
               ;; — vain listan avaaminen, ei kirjoittaminen, on ensimmäisen
               ;; kosketuksen tarkoitus. Molemmat poistuvat heti kun lista on
               ;; auki. read-only ei riitä yksin: esim. Firefox Androidilla
               ;; se ei estä näppäimistön avautumista, joten myös
               ;; input-mode "none" tarvitaan (selainten dokumentoitu tapa
               ;; sanoa "tämä kenttä ei tarvitse virtuaalinäppäimistöä").
               :read-only            (boolean (and @mobile? (not expanded?)))
               :inputMode            (when (and @mobile? (not expanded?)) "none")
               :required             (boolean required?)
               :aria-invalid         (boolean invalid?)
               :autoComplete         "off"
               :data-test-id         (str data-test-id "-button")
               :role                 "combobox"
               :aria-labelledby      (or aria-labelledby label-id)
               :aria-expanded        expanded?
               :aria-haspopup        "listbox"
               :aria-controls        listbox-id
               :aria-autocomplete    "list"
               :aria-activedescendant (:option-id active-option)
               :on-click             on-input-click
               :on-change            on-input-change
               :on-key-down          on-input-key-down
               :on-focus             (fn [_e] (scroll-field-to-top! (or aria-labelledby label-id)))}]
             (when (and (not disabled?) (not (string/blank? selected-value)))
               [dropdown-clear-button
                {:lang     lang
                 ;; Nappi katoaa DOM:sta heti tyhjennyksen jälkeen (renderöidään
                 ;; vain kun arvo on valittu), jolloin fokus katoaisi kokonaan
                 ;; ellei sitä siirretä eksplisiittisesti takaisin kenttään.
                 :on-click (fn dropdown-clear-button-clicked []
                             (on-change "")
                             (focus-input))}])
             [:button.a-dropdown-trigger
              {:type      "button"
               :tab-index "-1"
               :aria-hidden true
               :disabled  disabled?
               :on-click  on-trigger-click}
              [dropdown-caret
               {:expanded? expanded?}]]]
            [dropdown-select
             {:expanded?        expanded?
              :options          options
              :unselected-label unselected-label
              :selected-value   selected-value
              :dropdown-id      dropdown-id
              :disabled?        disabled?
              :id               id
              :data-test-id     data-test-id
              :on-click         (fn []
                                  (when-not disabled?
                                    (expand-dropdown {:dropdown-id dropdown-id})))
              :on-change        on-option-click}]
            [dropdown-popup
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
