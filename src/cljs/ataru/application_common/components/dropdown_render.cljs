(ns ataru.application-common.components.dropdown-render
  "dropdown-componentin renderöintilogiikka: propseista ja komponentin
  omista refeistä johdetun tilan laskenta, tapahtumakäsittelijöiden
  kokoaminen sekä lopullisen hiccupin muodostaminen. ataru.application-
  common.components.dropdown-component (reagent-elinkaaren omistava kuori)
  on tämän namespacen ainoa käyttäjä."
  (:require [clojure.string :as string]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frame.db :as db]
            [schema.core :as s]
            [ataru.application-common.components.dropdown-view :as view]
            [ataru.application-common.components.dropdown-viewport :as viewport]
            [ataru.application-common.components.dropdown-geometry :as geometry]
            [ataru.application-common.components.dropdown-actions :as actions]
            [ataru.application-common.components.dropdown-keyboard :as keyboard]
            [ataru.application-common.components.dropdown-listeners :as listeners]))

;; ---------------------------------------------------------------------
;; Propseista ja re-frame-tilasta johdettu tila
;; ---------------------------------------------------------------------

;; Valikon tila johdettuna sen raa'asta re-frame-tilasta ({:expanded? :query
;; :active-index}). Yhteinen renderöinnille (tilauksista, ks.
;; compute-dropdown-state) ja tapahtumakäsittelijöille (app-db:stä
;; tapahtumahetkellä, ks. make-dropdown-handlers/current-state).
(defn- derive-dropdown-state
  [dropdown-id {:keys [options selected-value disabled?]} {:keys [expanded? query active-index]}]
  (let [expanded?       (boolean (and (not disabled?) expanded?))
        options-with-id (view/get-filtered-options-with-id dropdown-id options query)]
    {:expanded?         expanded?
     :query             query
     :active-index      active-index
     :options-with-id   options-with-id
     :active-option     (when (and expanded? active-index)
                          (get options-with-id active-index))
     :selected-index    (view/find-selected-index options-with-id selected-value)
     :last-option-index (dec (count options-with-id))}))

(defn- compute-dropdown-state
  [{:keys [dropdown-id]} {:keys [options aria-labelledby aria-label] :as props}]
  (let [state-path [:components :dropdown dropdown-id]
        label-id   (str dropdown-id "-label")]
    (assoc (derive-dropdown-state
             dropdown-id
             props
             {:expanded?    @(re-frame/subscribe [:state-query (conj state-path :expanded?) false])
              :query        @(re-frame/subscribe [:state-query (conj state-path :query) nil])
              :active-index @(re-frame/subscribe [:state-query (conj state-path :active-index) nil])})
           :value->label   (into {} (map (juxt :value :label)) options)
           ;; Kentän näkyvän labelin id: kutsujan antama, tai oletus-label-id
           ;; silloin kun kutsuja ei ole antanut myöskään aria-labeliä. nil, kun
           ;; kentällä on vain aria-label eikä näkyvää labelia.
           :field-label-id (or aria-labelledby
                               (when-not aria-label label-id))
           :listbox-id     (str dropdown-id "-listbox"))))

;; ---------------------------------------------------------------------
;; Tapahtumakäsittelijät
;; ---------------------------------------------------------------------

;; Aika, jonka ruudunlukija (iOS:n VoiceOver) tarvitsee fokuksen siirron ja
;; laajan DOM-muutoksen (kokoruututilan alku tai loppu) käsittelyyn, ennen kuin
;; seuraava muutos tehdään (ks. focus-when-fullscreen-opens! ja
;; close-keeping-focus!).
(def ^:private screen-reader-settle-delay-ms 150)

;; Siirtää fokuksen elementtiin (syötekenttä tai avausnappi). blur ennen
;; focusta, koska elementti on yleensä jo fokusoitu: pelkkä focus ei silloin
;; laukaise focus-tapahtumaa, eikä ruudunlukijan (esim. VoiceOver) kursori
;; siirry siihen. refocusing? estää blurin sulkemasta valikkoa
;; (on-dropdown-blur). Fokus ei vieritä sivua, koska vierityskohta hoidetaan
;; erikseen (ks. sync-open-state!).
(defn- refocus! [el refocusing?]
  (when el
    (reset! refocusing? true)
    (try
      (.blur el)
      (.focus el #js {:preventScroll true})
      (finally
        (reset! refocusing? false)))))

(defn- make-dropdown-handlers
  [{:keys [dropdown-id root-ref popup-ref input-ref option-refs mobile?
           refocusing?]}
   {:keys [on-change disabled? clearable? selected-value] :as props}]
  ;; dispatch-sync eikä dispatch näissä kahdessa: syötekentän arvo ja
  ;; korostettu vaihtoehto ovat molemmat re-frame-tilan varassa, jota
  ;; näppäimistökäsittelijä (ks. dropdown-keyboard) lukee SYNKRONISESTI
  ;; heti seuraavan näppäinpainalluksen yhteydessä (esim. ArrowUp tarkistaa
  ;; active-indexin, jonka edellinen ArrowDown juuri asetti). Tavallinen
  ;; dispatch on asynkroninen (jonossa seuraavaan animaatiokehykseen asti),
  ;; joten nopea näppäinsarja voisi lukea vielä vanhaa, ei-vielä-käsiteltyä
  ;; tilaa. Reagentin oma <input>-kääre (ks. reagent.impl.input) tekee
  ;; asian pahemmaksi syötekentän kohdalla: se ei luota Reactiin kontrol-
  ;; loidun arvon ylläpitäjänä vaan asettaa DOM-arvon itse jokaisen
  ;; renderöinnin jälkeen — jos tuo renderöinti tapahtuu ehjän dispatchin
  ;; JÄLKEEN mutta asynkronisen dispatchin jonossa olevan päivityksen
  ;; EDELLÄ, se pakottaa DOM:n takaisin vanhaan arvoon. Sama kuvio kuin
  ;; editorin tekstikenttien :editor/set-component-value -dispatchit.
  ;;
  ;; Samasta syystä näppäimistökäsittelijä ja move-active-to lukevat tilan
  ;; re-frame-tilasta tapahtumahetkellä (current-state) eivätkä edellisen
  ;; renderöinnin arvoista: renderöinti on asynkroninen, joten nopeasti
  ;; peräkkäiset näppäimet (esim. End ja heti Enter) näkisivät muuten vielä
  ;; vanhan korostuksen tai sulkutilan.
  (let [current-state     (fn current-state []
                             (derive-dropdown-state dropdown-id
                                                    props
                                                    (get-in @db/app-db [:components :dropdown dropdown-id])))
        set-active-index  (fn set-active-index [idx]
                             (re-frame/dispatch-sync [:application-components/set-dropdown-active-index
                                                       {:dropdown-id  dropdown-id
                                                        :active-index idx}]))
        ;; Pitkässä listassa (esim. maat) korostettu vaihtoehto vieritetään
        ;; näkyviin.
        move-active-to    (fn move-active-to [idx]
                             (set-active-index idx)
                             (when-let [option-id (:option-id (get (:options-with-id (current-state)) idx))]
                               (reagent/after-render
                                 #(geometry/scroll-option-into-popup-view! @popup-ref
                                                                           (get @option-refs option-id)))))
        ;; dispatch-sync tässäkin, ks. selitys yllä.
        on-query-change   (fn on-query-change [value]
                             (re-frame/dispatch-sync [:application-components/set-dropdown-query
                                                       {:dropdown-id dropdown-id
                                                        :query       value}])
                             (set-active-index nil))
        ;; Oletuksena dispatch-sync (ks. selitys yllä), jotta heti seuraava
        ;; näppäin näkee listan auki eikä avaa sitä uudelleen — se tyhjentäisi
        ;; haun. Avausnappi avaa asynkronisesti (ks. on-trigger-click).
        open-popup        (fn open-popup
                             ([] (open-popup {:sync? true}))
                             ([{:keys [sync?]}]
                              (when-not (or disabled? (:expanded? (current-state)))
                                (on-query-change nil)
                                (let [event [:application-components/expand-dropdown
                                             {:dropdown-id dropdown-id}]]
                                  (if sync?
                                    (re-frame/dispatch-sync event)
                                    (re-frame/dispatch event))))))
        ;; Pelkkä fokus ei avaa listaa — vain klikkaus (myös napautus ja
        ;; VoiceOverin kaksoisnapautus), nuolinäppäimet tai kirjoittaminen
        ;; avaavat sen. Muuten kenttien välillä siirtyminen (Tab,
        ;; mobiilinäppäimistön seuraava/edellinen-nuolet, VoiceOverin
        ;; pyyhkäisy) avaisi jokaisen pudotusvalikon matkalla, ja mobiilissa
        ;; kokoruutudialogi estäisi siirtymästä eteenpäin. Klikkaus fokusoi
        ;; kentän selaimen oletustoiminnolla, joten virtuaalinäppäimistö nousee
        ;; normaalisti heti ensimmäisestä kosketuksesta.
        on-input-click    (fn on-input-click [_e]
                             (open-popup))
        on-input-change   (fn on-input-change [e]
                             (open-popup)
                             (on-query-change (.. e -target -value)))
        ;; :on-blur juuritasolla (ei pelkässä syötekentässä), jotta vain
        ;; fokuksen siirtyminen kokonaan komponentin ulkopuolelle sulkee sen.
        ;; Työpöydällä popup on portaalissa eikä @root-refin DOM-jälkeläinen,
        ;; joten fokuksen siirtyminen sen sisälle pitää tunnistaa erikseen.
        on-dropdown-blur  (fn on-dropdown-blur [e]
                             (let [related-target (.-relatedTarget e)
                                   inside?        (fn [ref]
                                                    (and @ref (.contains @ref related-target)))]
                               (when (and (not @refocusing?)
                                          (or (nil? related-target)
                                              (and @root-ref
                                                   (not (inside? root-ref))
                                                   (not (inside? popup-ref)))))
                                 (actions/collapse-dropdown {:dropdown-id dropdown-id}))))
        ;; Sulkee valikon niin, että fokus jää focus-el:iin. Fokus asetetaan
        ;; synkronisesti tapahtumankäsittelijässä, koska iOS päättää muuten
        ;; itse — vaihtelevasti — säilyykö fokus kokoruututilan sulkeutuessa,
        ;; ja sallii ohjelmallisen fokuksen (ja pitää näppäimistön auki) vain
        ;; käyttäjän eleen omassa käsittelijässä.
        ;;
        ;; Mobiilissa valikko suljetaan vasta viiveen jälkeen: ruudunlukija
        ;; käsittelee fokuksen siirron asynkronisesti, ja VoiceOverin
        ;; kaksoisnapautus on synteettinen tapahtumasarja — jos kokoruututila
        ;; katoaisi ennen niitä, kursori siirtyisi mielivaltaiseen kohtaan ja
        ;; loput tapahtumat osuisivat sivulla nyt samassa kohdassa olevaan
        ;; elementtiin.
        close-keeping-focus! (fn close-keeping-focus! [focus-el]
                               (refocus! focus-el refocusing?)
                               (if @mobile?
                                 (js/setTimeout #(actions/collapse-dropdown {:dropdown-id dropdown-id})
                                                screen-reader-settle-delay-ms)
                                 (actions/collapse-dropdown {:dropdown-id dropdown-id})))
        ;; Fokus jää valinnan jälkeen kenttään, jotta käyttäjä voi siirtyä
        ;; seuraavaan kenttään (Tab, mobiilinäppäimistön seuraava-nuoli).
        on-option-click   (fn on-option-click [value]
                             (on-change value)
                             (close-keeping-focus! @input-ref))
        clear-value       (fn clear-value []
                             (on-change ""))
        on-input-key-down (keyboard/make-on-input-key-down
                             {:dropdown-id       dropdown-id
                              :current-state     current-state
                              :open-popup        open-popup
                              :move-active-to    move-active-to
                              :set-active-index  set-active-index
                              :on-option-click   on-option-click
                              :clearable?        clearable?
                              :selected-value    selected-value
                              :clear-value       clear-value})
        ;; Avausnapista avattaessa fokus siirtyy syötekenttään kuten kentästä
        ;; avattaessa: synkronisesti tässä käsittelijässä, jotta iOS avaa
        ;; näppäimistön (se sallii sen vain käyttäjän eleen omassa
        ;; käsittelijässä). Kokoruututilan avauduttua ruudunlukijan kursori
        ;; siirretään kenttään (ks. focus-when-fullscreen-opens!). Suljettaessa
        ;; fokus jää nappiin: ruudunlukijan kursori pysyy siinä, eikä
        ;; näppäimistö avaudu.
        ;;
        ;; Avaus asynkronisesti (open-popup {:sync? false}): VoiceOverin
        ;; kaksoisnapautus voi tuottaa saman eleen aikana toisen clickin, joka
        ;; synkronisesti avatun listan jo auki nähdessään sulkisi sen heti.
        on-trigger-click  (fn on-trigger-click [e]
                             (.preventDefault e)
                             (when-not disabled?
                               (if (:expanded? (current-state))
                                 (close-keeping-focus! (.-currentTarget e))
                                 (do (open-popup {:sync? false})
                                     (some-> @input-ref (.focus #js {:preventScroll true}))))))]
    {:on-input-click    on-input-click
     :on-input-change   on-input-change
     :on-dropdown-blur  on-dropdown-blur
     :on-option-click   on-option-click
     :on-input-key-down on-input-key-down
     :on-trigger-click  on-trigger-click
     :on-clear-click    clear-value}))

;; ---------------------------------------------------------------------
;; Auki/kiinni- ja kokoruututilan sivuvaikutukset
;; ---------------------------------------------------------------------

;; Kokoruututilan wrapper merkitään modaaliseksi dialogiksi: ruudunlukija
;; (esim. VoiceOver) rajaa navigoinnin siihen, mutta muuta sivua ei poisteta
;; sen elementtilistasta. Sivun piilottaminen (aria-hidden tai visibility)
;; saisi ruudunlukijan sulkemisen jälkeen tulkitsemaan muutoksen näkymän
;; vaihdoksi ja aloittamaan sivun alusta. Wrapper on kutsujan (hakija)
;; renderöimä, joten attribuutit asetetaan suoraan DOM:iin; vain muuttuneet,
;; koska tätä kutsutaan jokaisella renderöinnillä.
(def ^:private dialog-attrs ["role" "aria-modal" "aria-labelledby"])

(defn- set-attr! [el attr value]
  (if (some? value)
    (when (not= (.getAttribute el attr) value)
      (.setAttribute el attr value))
    (when (.hasAttribute el attr)
      (.removeAttribute el attr))))

(defn- sync-fullscreen-dialog! [root-el fullscreen? label-id]
  (when-let [wrapper-el (some-> root-el (.closest geometry/fullscreen-wrapper-selector))]
    (doseq [[attr value] (map vector dialog-attrs
                              (if fullscreen?
                                ["dialog" "true" label-id]
                                [nil nil nil]))]
      (set-attr! wrapper-el attr value))))

;; Kokoruututilan avautuessa ruudunlukija (VoiceOver) siirtää kursorinsa
;; dialogiin siihen vaihtoehtoon, joka osuu edellisen kohdan (esim. avausnapin)
;; alle, ja alkaa lukea sitä. Heti asetettu fokus ei auta, koska ruudunlukija
;; käsittelee avaamisen vasta sen jälkeen. Siksi, kun popup on DOM:issa
;; (popupin elinkaaressa, ks. dropdown-view/dropdown-popup):
;; 1. lista asemoidaan ja valittu vaihtoehto vieritetään sen yläosaan,
;; 2. vaihtoehdot piilotetaan ruudunlukijalta (aria-hidden), jottei sen kursori
;;    voi osua mihinkään niistä avaamisen ajan, ja
;; 3. viiveen jälkeen piilotus poistetaan ja fokus asetetaan uudelleen
;;    syötekenttään (sekä kentästä että avausnapista avattaessa), jos fokus on
;;    yhä siinä.
;; aria-hidden asetetaan suoraan DOM:iin, koska se on tilapäinen eikä React
;; hallitse sitä.
(defn- focus-when-fullscreen-opens! [{:keys [input-ref refocusing? sync-popup-geometry!]}
                                     popup-el]
  (sync-popup-geometry!)
  (geometry/scroll-selected-option-to-popup-top! popup-el)
  (let [listbox-el (.querySelector popup-el "[role=\"listbox\"]")]
    (some-> listbox-el (.setAttribute "aria-hidden" "true"))
    (js/setTimeout
      (fn []
        (some-> listbox-el (.removeAttribute "aria-hidden"))
        (let [input-el @input-ref]
          (when (and input-el (= input-el (.-activeElement js/document)))
            (refocus! input-el refocusing?))))
      screen-reader-settle-delay-ms)))

(defn sync-open-state!
  "Kytkee auki olevan valikon ja kokoruututilan sivuvaikutukset päälle tai
  pois tilan mukaan. Kaikki vaiheet ovat idempotentteja (addEventListener/
  removeEventListener samalle funktioviitteelle, sekä tilan atomeihin
  kirjaavat apurit), joten tätä kutsutaan jokaisella renderöinnillä."
  [context {:keys [expanded? fullscreen? label-id]}]
  (sync-fullscreen-dialog! @(:root-ref context) fullscreen? label-id)
  ;; Globaalit kuuntelijat vain auki olevalle valikolle — muuten niitä
  ;; ajettaisiin turhaan jokaiselle lomakkeen pudotusvalikolle.
  (if expanded?
    (listeners/attach-global-listeners! context)
    (listeners/detach-global-listeners! context))
  (if fullscreen?
    (do (listeners/attach-fullscreen-listeners!)
        (viewport/lock-body-scroll! (:locked-scroll-y context))
        (geometry/start-fullscreen-geometry-loop! (:geometry-raf-id context)
                                                  (:sync-popup-geometry! context)))
    (do (listeners/detach-fullscreen-listeners!)
        (geometry/stop-fullscreen-geometry-loop! (:geometry-raf-id context))
        ;; Palautetaan sivun vierityskohta, jossa se oli avattaessa, ja
        ;; renderöinnin jälkeen (kun kokoruutuwrapper on palannut sivulle)
        ;; vieritetään kenttä tarvittaessa näkyviin — fokus jää kenttään, joten
        ;; näppäimistö voi muuten peittää sen.
        ;;
        ;; Lopuksi fokus asetetaan vielä kerran samaan komponentin elementtiin,
        ;; jossa se on (syötekenttä tai avausnappi): ruudunlukija (VoiceOver)
        ;; tulkitsee kokoruututilan päättymisen näkymän vaihdoksi ja siirtää
        ;; kursorinsa muualle, vaikka fokus olisi jo paikallaan. Heti
        ;; sulkevan renderöinnin jälkeen eikä viiveellä, jottei ruudunlukija
        ;; ehdi aloittaa lukemaan muuta. Vain jos fokus on yhä komponentissa,
        ;; jottei käyttäjän itse siirtämää fokusta viedä pois.
        (when (some? @(:locked-scroll-y context))
          (viewport/unlock-body-scroll! (:locked-scroll-y context))
          (reagent/after-render
            (fn []
              (viewport/scroll-into-visual-viewport! @(:field-ref context))
              (let [root-el   @(:root-ref context)
                    active-el (.-activeElement js/document)]
                (when (and root-el active-el (.contains root-el active-el))
                  (refocus! active-el (:refocusing? context)))))))))
  ;; Alkuarvo heti avattaessa (ks. dropdown-geometry). Sen jälkeen ajan
  ;; tasalla pitävät resize/scroll-kuuntelijat ja kokoruututilassa
  ;; geometriasilmukka.
  (when expanded?
    (reagent/after-render (:sync-popup-geometry! context))))

;; ---------------------------------------------------------------------
;; Hiccupin muodostaminen
;; ---------------------------------------------------------------------

(s/defn render-dropdown
  [context
   {:keys [unselected-label
           unselected-label-icon
           selected-value
           disabled?
           required?
           clearable?
           invalid?
           id
           aria-label
           data-test-id]
    :as   props} :- {:options                                [view/SelectOptionProps]
                      :unselected-label                       s/Str
                      ;; Yksittäinen hiccup-elementti (esim. [:i.zmdi...]), ei merkkijono.
                      (s/optional-key :unselected-label-icon) s/Any
                      :selected-value                         (s/maybe s/Str)
                      :on-change                               s/Any
                      (s/optional-key :disabled?)             s/Bool
                      (s/optional-key :required?)             s/Bool
                      ;; false vain kentille, joiden vaihtoehtolistassa ei koskaan ole
                      ;; tyhjää/valitsematonta vaihtoehtoa (ks. hakija/components/
                      ;; dropdown_component.cljs:n no-blank-option) — niille tyhjennys-
                      ;; nappi olisi ainoa tapa saada kenttä tilaan, jota se ei koskaan
                      ;; voi luonnostaan olla. Puuttuessaan tyhjennettävissä.
                      (s/optional-key :clearable?)            s/Bool
                      (s/optional-key :invalid?)              s/Bool
                      (s/optional-key :id)                    (s/maybe s/Str)
                      (s/optional-key :aria-labelledby)       (s/maybe s/Str)
                      ;; Vaihtoehto aria-labelledby:lle silloin, kun kentällä ei
                      ;; ole omaa näkyvää <label>-elementtiä (ks. hakija-dropdown).
                      (s/optional-key :aria-label)            (s/maybe s/Str)
                      (s/optional-key :data-test-id)          (s/maybe s/Str)}]
  (let [disabled?      (boolean disabled?)
        required?      (boolean required?)
        invalid?       (boolean invalid?)
        clearable?     (not (false? clearable?))
        lang           @(re-frame/subscribe [:application/form-language])
        props          (assoc props
                          :disabled?  disabled?
                          :clearable? clearable?)
        _              (actions/maybe-collapse-when-disabled!
                         {:dropdown-id (:dropdown-id context)
                          :disabled?   disabled?})
        state          (compute-dropdown-state context props)
        {:keys [expanded? query options-with-id active-option field-label-id
                listbox-id value->label]} state
        handlers       (make-dropdown-handlers context props)
        ;; Kentässä näytettävä teksti, kun käyttäjä ei ole kirjoittamassa hakua.
        display-label  (if (string/blank? selected-value)
                         unselected-label
                         (get value->label selected-value))
        input-value    (view/compute-input-value {:expanded?     expanded?
                                                    :query         query
                                                    :display-label display-label})
        fullscreen?    (and expanded? @(:mobile? context))
        _              (sync-open-state! context {:expanded?   expanded?
                                                  :fullscreen? fullscreen?
                                                  :label-id    field-label-id})]
    [:div.a-dropdown
     {:ref     #(reset! (:root-ref context) %)
      :class   (str (when disabled? "a-dropdown--disabled ")
                     (when fullscreen? "a-dropdown--fullscreen"))
      :on-blur (:on-dropdown-blur handlers)}
     [view/dropdown-field
      {:input-ref             #(reset! (:input-ref context) %)
       :field-ref             #(reset! (:field-ref context) %)
       :id                    id
       :value                 input-value
       :unselected-label      unselected-label
       :unselected-label-icon unselected-label-icon
       :disabled?             disabled?
       :required?             required?
       :invalid?              invalid?
       :data-test-id          data-test-id
       :aria-labelledby       field-label-id
       :aria-label            aria-label
       :expanded?             expanded?
       :listbox-id            listbox-id
       :active-option-id      (:option-id active-option)
       :selected-value        selected-value
       :clearable?            clearable?
       :lang                  lang
       :on-input-click        (:on-input-click handlers)
       :on-input-change       (:on-input-change handlers)
       :on-input-key-down     (:on-input-key-down handlers)
       :on-clear-click        (:on-clear-click handlers)
       :on-trigger-click      (:on-trigger-click handlers)}]
     ;; Kokoruututilassa lista renderöidään heti kentän perään eikä
     ;; portaaliin: ruudunlukijan (esim. VoiceOver) pyyhkäisy etenee kentästä
     ;; suoraan vaihtoehtoihin, eikä muuta sivua tarvitse piilottaa siltä.
     ;; Valinnan jälkeen vain lista katoaa kentän vierestä, joten kursori
     ;; pysyy kentässä ja seuraava pyyhkäisy jatkaa seuraavaan kenttään.
     ;; Ulkoasu ei muutu, koska popup on position: fixed ja kokoruutuwrapperin
     ;; sisällä. Työpöydällä portaali estää esi-isien stacking contextia
     ;; peittämästä listaa.
     (let [popup [view/dropdown-popup
                  {:expanded?          expanded?
                   :fullscreen?        fullscreen?
                   :options-with-id    options-with-id
                   :on-click           (:on-option-click handlers)
                   :listbox-labelledby field-label-id
                   :listbox-label      aria-label
                   :dropdown-id        (:dropdown-id context)
                   :selected-value     selected-value
                   :active-option-id   (:option-id active-option)
                   :register-ref       (:register-option-ref context)
                   :popup-ref          #(reset! (:popup-ref context) %)
                   ;; Avattaessa (millä tavalla tahansa) valittu vaihtoehto
                   ;; vieritetään listan yläosaan; kokoruututilassa samalla
                   ;; asemoidaan lista ja asetetaan fokus (ks. focus-when-
                   ;; fullscreen-opens!).
                   :on-open            (if fullscreen?
                                         #(focus-when-fullscreen-opens! context %)
                                         geometry/scroll-selected-option-to-popup-top!)
                   :lang               lang
                   :data-test-id       data-test-id}]]
       (cond
         fullscreen?                   popup
         @(:portal-container context) (js/ReactDOM.createPortal
                                        (reagent/as-element popup)
                                        @(:portal-container context))))
     ;; Listan jälkeen, jotta ruudunlukijan pyyhkäisy etenee kentästä suoraan
     ;; vaihtoehtoihin: live-alueena ilmoitus kuuluu ilman, että siihen
     ;; siirrytään, joten se luettaisiin muuten kahdesti.
     [view/dropdown-status
      {:searching?   (and expanded? (some? query))
       :option-count (count options-with-id)
       :lang         lang}]]))
