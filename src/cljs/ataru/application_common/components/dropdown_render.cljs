(ns ataru.application-common.components.dropdown-render
  "dropdown-componentin renderöintilogiikka: propseista ja komponentin
  omista refeistä johdetun tilan laskenta, tapahtumakäsittelijöiden
  kokoaminen sekä lopullisen hiccupin muodostaminen. ataru.application-
  common.components.dropdown-component (reagent-elinkaaren omistava kuori)
  on tämän namespacen ainoa käyttäjä."
  (:require [clojure.string :as string]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [ataru.application-common.components.dropdown-view :as view]
            [ataru.application-common.components.dropdown-viewport :as viewport]
            [ataru.application-common.components.dropdown-actions :as actions]
            [ataru.application-common.components.dropdown-keyboard :as keyboard]))

;; ---------------------------------------------------------------------
;; Propseista ja re-frame-tilasta johdettu tila
;; ---------------------------------------------------------------------

(defn- compute-dropdown-state
  [{:keys [dropdown-id]} {:keys [options selected-value disabled?]}]
  (let [expanded?          (and (not disabled?)
                                 @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false]))
        query              @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :query] nil])
        active-index       @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :active-index] nil])
        value->label       (into {} (map (juxt :value :label)) options)
        options-with-id    (view/get-filtered-options-with-id dropdown-id options query)
        active-option      (when (and expanded? active-index)
                              (get options-with-id active-index))
        selected-index     (view/find-selected-index options-with-id selected-value)
        last-option-index  (dec (count options-with-id))]
    {:expanded?         expanded?
     :query             query
     :active-index      active-index
     :value->label      value->label
     :options-with-id   options-with-id
     :active-option     active-option
     :selected-index    selected-index
     :last-option-index last-option-index
     :label-id          (str dropdown-id "-label")
     :listbox-id        (str dropdown-id "-listbox")}))

;; ---------------------------------------------------------------------
;; Tapahtumakäsittelijät
;; ---------------------------------------------------------------------

(defn- make-dropdown-handlers
  [{:keys [dropdown-id root-ref option-refs focus-input mobile?]}
   {:keys [on-change disabled? aria-labelledby]}
   {:keys [expanded? active-index selected-index last-option-index
           active-option options-with-id label-id]}]
  (let [set-active-index  (fn set-active-index [idx]
                             (re-frame/dispatch [:application-components/set-dropdown-active-index
                                                  {:dropdown-id  dropdown-id
                                                   :active-index idx}]))
        ;; Popupin sisältö on vierittyvä, joten pitkässä listassa (esim. maat) korostettu vaihtoehto 
        ;; pitää vierittää näkyviin.
        move-active-to    (fn move-active-to [idx]
                             (set-active-index idx)
                             (when-let [option-id (:option-id (get options-with-id idx))]
                               (reagent/after-render
                                 (fn []
                                   (when-let [el (get @option-refs option-id)]
                                     (.scrollIntoView el #js {:block "nearest"}))))))
        on-query-change   (fn on-query-change [value]
                             (re-frame/dispatch [:application-components/set-dropdown-query
                                                  {:dropdown-id dropdown-id
                                                   :query       value}])
                             (set-active-index nil))
        open-popup        (fn open-popup []
                             (when-not disabled?
                               (when-not expanded?
                                 (on-query-change nil)
                                 (viewport/scroll-field-to-top! (or aria-labelledby label-id))
                                 (actions/expand-dropdown {:dropdown-id dropdown-id}))))
        ;; Pelkkä näppäimistöfokus (esim. Tab kenttään) ei avaa listaa —
        ;; vain klikkaus, nuolinäppäimet tai kirjoittaminen avaavat sen.
        ;; Klikkaus fokusoi kentän selaimen oletustoiminnolla, joten myös
        ;; virtuaalinäppäimistö nousee esiin normaalisti heti ensimmäisestä
        ;; kosketuksesta mobiilissa.
        on-input-click    (fn on-input-click [_e]
                             (open-popup))
        on-input-change   (fn on-input-change [e]
                             (open-popup)
                             (on-query-change (.. e -target -value)))
        ;; :on-blur juuritasolla (ei pelkässä syötekentässä), jotta vain
        ;; fokuksen siirtyminen kokonaan komponentin ulkopuolelle sulkee sen.
        on-dropdown-blur  (fn on-dropdown-blur [e]
                             (let [related-target (.-relatedTarget e)]
                               (when (or (nil? related-target)
                                         (and @root-ref
                                              (not (.contains @root-ref related-target))))
                                 (actions/collapse-dropdown {:dropdown-id dropdown-id}))))
        on-option-click   (fn on-option-click [value]
                             (actions/collapse-dropdown {:dropdown-id dropdown-id})
                             (on-change value))
        on-input-key-down (keyboard/make-on-input-key-down
                             {:dropdown-id       dropdown-id
                              :expanded?         expanded?
                              :active-index      active-index
                              :selected-index    selected-index
                              :last-option-index last-option-index
                              :active-option     active-option
                              :open-popup        open-popup
                              :move-active-to    move-active-to
                              :set-active-index  set-active-index
                              :on-option-click   on-option-click})
        on-trigger-click  (fn on-trigger-click [e]
                             (.preventDefault e)
                             (when-not disabled?
                               (if expanded?
                                 (actions/collapse-dropdown {:dropdown-id dropdown-id})
                                 (do (open-popup)
                                     (focus-input)))))
        ;; Mobiilissa listan avaaminen ei saa nojata pelkkään klikkaukseen:
        ;; kentän fokusoituminen käynnistää heti virtuaalinäppäimistön
        ;; animaation, jonka aiheuttama asetteluhyppäys kesken kosketuksen
        ;; voi saada selaimen peruuttamaan sitä seuraavan synteettisen
        ;; click-tapahtuman kokonaan — jolloin on-input-click ei koskaan
        ;; ehtisi suorittua. Fokus sen sijaan tapahtuu aina, joten
        ;; avataan valikko jo sen yhteydessä.
        on-input-focus    (fn on-input-focus [_e]
                             (viewport/scroll-field-to-top! (or aria-labelledby label-id))
                             (when @mobile?
                               (open-popup)))
        on-clear-click    (fn dropdown-clear-button-clicked []
                             (on-change ""))]
    {:set-active-index  set-active-index
     :move-active-to    move-active-to
     :on-query-change   on-query-change
     :open-popup        open-popup
     :on-input-click    on-input-click
     :on-input-change   on-input-change
     :on-dropdown-blur  on-dropdown-blur
     :on-option-click   on-option-click
     :on-input-key-down on-input-key-down
     :on-trigger-click  on-trigger-click
     :on-input-focus    on-input-focus
     :on-clear-click    on-clear-click}))

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
           aria-labelledby
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
  (let [disabled?      (boolean disabled?)
        required?      (boolean required?)
        invalid?       (boolean invalid?)
        lang           @(re-frame/subscribe [:application/form-language])
        props          (assoc props :disabled? disabled?)
        state          (compute-dropdown-state context props)
        {:keys [expanded? query options-with-id active-option label-id listbox-id
                value->label]} state
        handlers       (make-dropdown-handlers context props state)
        resolved-aria-labelledby (or aria-labelledby
                                     (when-not aria-label label-id))
        selected-label (get value->label selected-value)
        button-label   (if-not (string/blank? selected-value)
                         selected-label
                         unselected-label)
        input-value    (view/compute-input-value {:expanded?    expanded?
                                                    :query        query
                                                    :button-label button-label})
        fullscreen?    (and expanded? @(:mobile? context))
        ;; Alkuarvo heti avattaessa — sen jälkeen resize/scroll-kuuntelijat
        ;; (ks. dropdown-listeners/attach-listeners!) pitävät sen ajan
        ;; tasalla myös näppäimistön sulkeutuessa. Tarvitaan aina kun auki
        ;; (ei vain kokoruututilassa), koska popup on nyt portaali eikä saa
        ;; sijaintiaan enää ilmaiseksi CSS:llä.
        _              (when expanded?
                         (reagent/after-render (:sync-popup-geometry! context)))]
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
       :aria-labelledby       resolved-aria-labelledby
       :aria-label            aria-label
       :expanded?             expanded?
       :listbox-id            listbox-id
       :active-option-id      (:option-id active-option)
       :selected-value        selected-value
       :clearable?            (not (false? clearable?))
       :lang                  lang
       :on-input-click        (:on-input-click handlers)
       :on-input-change       (:on-input-change handlers)
       :on-input-key-down     (:on-input-key-down handlers)
       :on-input-focus        (:on-input-focus handlers)
       :on-clear-click        (:on-clear-click handlers)
       :on-trigger-click      (:on-trigger-click handlers)}]
     (when @(:portal-container context)
       (js/ReactDOM.createPortal
         (reagent/as-element
           [view/dropdown-popup
            {:expanded?        expanded?
             :options-with-id  options-with-id
             :on-click         (:on-option-click handlers)
             :label-id         label-id
             :dropdown-id      (:dropdown-id context)
             :selected-value   selected-value
             :active-option-id (:option-id active-option)
             :register-ref     (:register-option-ref context)
             :popup-ref        #(reset! (:popup-ref context) %)
             :lang             lang
             :data-test-id     data-test-id}])
         @(:portal-container context)))]))
