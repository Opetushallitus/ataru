(ns ataru.application-common.components.dropdown-view
  "dropdown-component/dropdown-komponentin puhtaat, esitykselliset osat: ei
  re-frame-dispatchia, ei DOM-kuuntelijoita, ei viewport-tilaa — kaikki
  tarvittava tulee propseina/argumentteina. ataru.application-common.
  components.dropdown-render (dropdown-componentin renderöintilogiikka) on
  tämän namespacen ainoa käyttäjä."
  (:require [clojure.string :as string]
            [schema.core :as s]
            [schema-tools.core :as st]
            [ataru.translations.translation-util :as translations]))

(s/defschema SelectOptionProps
  {:value s/Str
   :label s/Str})

;; ---------------------------------------------------------------------
;; Vaihtoehtolistan puhtaat apufunktiot
;; ---------------------------------------------------------------------

(defn get-filtered-options-with-id
  "Suodattaa vaihtoehdot query:llä (tapauksesta riippumaton osamerkkijonohaku
  labelista) ja lisää kullekin option-id:n, jota käytetään DOM-id:nä, 
  aria-activedescendant-viittauksissa, sekä optioneiden key-attribuuttina."
  [dropdown-id options query]
  (let [options-with-id (map (fn [option-props]
                                (assoc option-props
                                       :option-id
                                       (str dropdown-id "-option-" (:value option-props))))
                              options)]
    (vec (if (string/blank? query)
           options-with-id
           (let [query-lower (string/lower-case query)]
             (filter (fn [{:keys [label]}]
                       (string/includes? (string/lower-case label) query-lower))
                     options-with-id))))))

(defn find-selected-index [options-with-id selected-value]
  (when-not (string/blank? selected-value)
    (->> options-with-id
         (keep-indexed (fn [idx {:keys [value]}]
                          (when (= value selected-value) idx)))
         first)))

;; query on nil kunnes käyttäjä muokkaa kenttää (myös heti avattaessa, ks.
;; open-popup) — silloin näytetään nykyinen valinta, mutta listaa ei silti
;; suodateta (blank query = ei suodatusta). Vasta kun käyttäjä kirjoittaa
;; jotain (myös tyhjäksi asti poistaen, jolloin query on "" eikä nil),
;; kenttä näyttää kirjoitetun haun.
(defn compute-input-value [{:keys [expanded? query button-label]}]
  (if (and expanded? (some? query))
    query
    (or button-label "")))

;; ---------------------------------------------------------------------
;; Esityskomponentit
;; ---------------------------------------------------------------------

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
    ;; Ilman tätä hiiren/kosketuksen painallus siirtää fokuksen
    ;; syötekentästä nappiin ennen kuin click ehtii tapahtua.
    :on-mouse-down (fn [e] (.preventDefault e))
    :on-click      (fn dropdown-clear-button-on-click [e]
                     (.stopPropagation e)
                     (on-click))}
   [:i.zmdi.zmdi-close {:aria-hidden true}]])

(s/defn dropdown-list-option
  [{:keys [value
           label
           on-click
           option-id
           selected-value
           active?
           register-ref]} :- (st/assoc
                               SelectOptionProps
                               :on-click s/Any
                               :option-id s/Str
                               :selected-value (s/maybe s/Str)
                               :active? s/Bool
                               :register-ref s/Any)]
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
      ;; Testien (ks. playwright-utils.ts/resolveOptionLabel) ja muun
      ;; ulkopuolisen tarkastelun koti raa'alle arvolle — vrt.
      ;; dropdown-field/data-selected-option-value.
      :data-value    value
      :tab-index     "-1"}
     [:span.a-dropdown-list__option-label label]]))

;; Yksi Popup, jonka sisällä joko List (Item per vaihtoehto) tai Empty-tila.
;;
;; aria-activedescendant EI ole tässä listboxissa, vaan syötekentässä (ks.
;; dropdown-field) — ARIA-yhdistelmäruutumallissa se kuuluu sille
;; elementille, jolla on todellinen näppäimistöfokus, ei sille jota se
;; osoittaa.
;; Sijainti ja koko (ks. dropdown-geometry/make-sync-popup-geometry!) asetetaan
;; suoraan DOM:iin popup-refin kautta, koska popup renderöidään Reactin
;; portaalilla document.bodyyn eikä siis voi enää saada niitä ilmaiseksi
;; CSS:llä kutsujansa suhteen — tämä komponentti ei siis ota niihin kantaa.
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
                              :lang                                 s/Keyword
                              :data-test-id                         (s/maybe s/Str)}]
  (let [listbox-id (str dropdown-id "-listbox")]
    [:div.a-dropdown-popup
     {:ref          popup-ref
      :data-test-id (str data-test-id "-list")
      :tab-index    "-1"
      :class        (when-not expanded?
                      "a-dropdown-popup--collapsed")}
     ;; id pysyy aina samana kuin dropdown-fieldin aria-controls, myös silloin
     ;; kun suodatus ei löydä yhtään vaihtoehtoa — muuten aria-controls
     ;; osoittaisi olemattomaan elementtiin eikä esim. testien tai
     ;; ruudunlukijan id-pohjainen haku löytäisi tätä ollenkaan.
     [:ul.a-dropdown-list
      {:id              listbox-id
       :aria-labelledby label-id
       :tab-index        "-1"
       :role            "listbox"}
      (if (empty? options-with-id)
        [:li.a-dropdown-empty
         (translations/get-hakija-translation :no-dropdown-search-hits lang)]
        (map (fn [option-props]
               ^{:key (:option-id option-props)}
               [dropdown-list-option (merge option-props
                                            {:on-click       on-click
                                             :selected-value selected-value
                                             :active?        (= active-option-id (:option-id option-props))
                                             :register-ref   register-ref})])
             options-with-id))]]))

;; Kentän oma osa (syötekenttä + tyhjennysnappi + avausnappi) — erillään
;; dropdown-popupista samasta syystä kuin popup on omansa: kutsujan
;; (dropdown-render/render-dropdown) oma hiccup pysyy lyhyenä ja tämän
;; osan omat propsit selkeästi nimettyinä.
(s/defn dropdown-field
  [{:keys [input-ref
           field-ref
           id
           value
           unselected-label
           unselected-label-icon
           disabled?
           required?
           invalid?
           data-test-id
           aria-labelledby
           aria-label
           expanded?
           listbox-id
           active-option-id
           selected-value
           clearable?
           lang
           on-input-click
           on-input-change
           on-input-key-down
           on-input-focus
           on-clear-click
           on-trigger-click]} :- {:input-ref                               s/Any
                                  (s/optional-key :field-ref)              s/Any
                                  (s/optional-key :id)                     (s/maybe s/Str)
                                  :value                                   s/Str
                                  :unselected-label                        s/Str
                                  (s/optional-key :unselected-label-icon)  s/Any
                                  :disabled?                               s/Bool
                                  :required?                               s/Bool
                                  :invalid?                                s/Bool
                                  (s/optional-key :data-test-id)           (s/maybe s/Str)
                                  (s/optional-key :aria-labelledby)        (s/maybe s/Str)
                                  (s/optional-key :aria-label)             (s/maybe s/Str)
                                  :expanded?                               s/Bool
                                  :listbox-id                              s/Str
                                  (s/optional-key :active-option-id)       (s/maybe s/Str)
                                  :selected-value                          (s/maybe s/Str)
                                  :clearable?                              s/Bool
                                  :lang                                    s/Keyword
                                  :on-input-click                          s/Any
                                  :on-input-change                         s/Any
                                  :on-input-key-down                       s/Any
                                  :on-input-focus                          s/Any
                                  :on-clear-click                          s/Any
                                  :on-trigger-click                        s/Any}]
  [:div.a-dropdown-field
   {:ref field-ref}
   (when (seq unselected-label-icon)
     [:span.a-dropdown-field__icon unselected-label-icon])
   [:input.a-dropdown-input
    {:ref                   input-ref
     :id                    id
     :type                  "text"
     :value                 value
     :placeholder           unselected-label
     :disabled              disabled?
     :required              required?
     :aria-invalid          invalid?
     :autoComplete          "off"
     :data-test-id          data-test-id
     ;; Kentän NÄKYVÄ value on valitun vaihtoehdon label (haku-/näyttöteksti),
     ;; ei sen taustalla oleva raaka arvo (esim. koodistokoodi) — sille ei
     ;; muuten ole mitään kotia DOM:issa, koska vaihtoehtolista (ks. dropdown-
     ;; popup) renderöityy vasta kentän ensimmäisen avaamisen jälkeen eikä
     ;; sen aria-selected-tilaan siis voi luottaa. Puhtaasti tarkastelua
     ;; varten, ei ARIA:a eikä käyttöliittymälogiikkaa.
     :data-selected-option-value selected-value
     :role                  "combobox"
     ;; label-id on kutsujan (dropdown-render/render-dropdown) keksimä
     ;; id, johon ei ole olemassa vastaavaa DOM-elementtiä ellei kutsuja
     ;; anna omaa aria-labelledbytä (ks. hakija/dropdown_component.cljs/
     ;; dropdown). Osoittaminen olemattomaan id:hen jättäisi kentän ilman
     ;; saavutettavaa nimeä, joten sitä käytetään oletuksena vain kun
     ;; kutsuja ei ole antanut myöskään aria-labeliä (ks. hakija-dropdown).
     ;; Tämä ratkaisu on jo tehty kutsujassa — tänne tulee valmiiksi
     ;; ratkaistu arvo.
     :aria-labelledby       aria-labelledby
     :aria-label            aria-label
     :aria-expanded         expanded?
     :aria-haspopup         "listbox"
     :aria-controls         listbox-id
     :aria-autocomplete     "list"
     :aria-activedescendant active-option-id
     :on-click              on-input-click
     :on-change             on-input-change
     :on-key-down           on-input-key-down
     :on-focus              on-input-focus}]
   (when (and (not disabled?) clearable? (not (string/blank? selected-value)))
     [dropdown-clear-button
      {:lang     lang
       :on-click on-clear-click}])
   [:button.a-dropdown-trigger
    {:type        "button"
     :tab-index   "-1"
     :aria-hidden true
     :disabled    disabled?
     :on-click    on-trigger-click}
    [dropdown-caret {:expanded? expanded?}]]])
