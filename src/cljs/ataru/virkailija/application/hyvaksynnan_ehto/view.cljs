(ns ataru.virkailija.application.hyvaksynnan-ehto.view
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [ataru.util :as util]
            ataru.virkailija.application.hyvaksynnan-ehto.subs))

(defn ehdollisesti-hyvaksyttavissa
  [application-key hakukohde-oid]
  (let [disabled? @(re-frame/subscribe [:hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa-disabled?
                                        application-key
                                        hakukohde-oid])
        checked?  @(re-frame/subscribe [:hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa?
                                        application-key
                                        hakukohde-oid])]
    [:div.hyvaksynnan-ehto-ehdollisesti-hyvaksyttavissa
     [:input.hyvaksynnan-ehto-ehdollisesti-hyvaksyttavissa__checkbox
      {:id        (str "hyvaksynnan-ehto-ehdollisesti-hyvaksyttavissa-" application-key "-" hakukohde-oid)
       :type      "checkbox"
       :disabled  disabled?
       :checked   checked?
       :on-change (fn [_]
                    (re-frame/dispatch [:hyvaksynnan-ehto/set-ehdollisesti-hyvaksyttavissa
                                        application-key
                                        hakukohde-oid
                                        (not checked?)])
                    (when checked?
                      (re-frame/dispatch [:hyvaksynnan-ehto/delete-ehto-hakukohteessa
                                          application-key
                                          hakukohde-oid])))}]
     [:label.hyvaksynnan-ehto-ehdollisesti-hyvaksyttavissa__label
      {:for (str "hyvaksynnan-ehto-ehdollisesti-hyvaksyttavissa-" application-key "-" hakukohde-oid)}
      @(re-frame/subscribe [:editor/virkailija-translation :ehdollisesti-hyvaksyttavissa])]]))

(defn ehto-koodi
  [_ _]
  (let [open?  (r/atom false)
        koodit (re-frame/subscribe [:hyvaksynnan-ehto/ehto-koodit])]
    (fn [application-key hakukohde-oid]
      (let [disabled?            @(re-frame/subscribe
                                   [:hyvaksynnan-ehto/ehto-text-disabled? application-key])
            selected-koodi       @(re-frame/subscribe
                                   [:hyvaksynnan-ehto/selected-ehto-koodi
                                    application-key
                                    hakukohde-oid])
            selected-koodi-label @(re-frame/subscribe
                                   [:hyvaksynnan-ehto/selected-ehto-koodi-label
                                    application-key
                                    hakukohde-oid])]
        [:div.hyvaksynnan-ehto-ehto-koodi-dropdown
         (if (and (not disabled?)
                  (or @open? (nil? selected-koodi)))
           (into
            [:div.application-handling__review-state-list.application-handling__review-state-list--opened]
            (mapv (fn [[koodi label]]
                    [:div.application-handling__review-state-row
                     {:key      (str "hyvaksynnan-ehto-koodi-" (name koodi))
                      :class    (when (= selected-koodi koodi)
                                  " application-handling__review-state-row--selected")
                      :on-click (fn [_]
                                  (reset! open? false)
                                  (when (not (= selected-koodi koodi))
                                    (re-frame/dispatch [:hyvaksynnan-ehto/set-ehto-koodi
                                                        application-key
                                                        hakukohde-oid
                                                        koodi])
                                    (re-frame/dispatch [:hyvaksynnan-ehto/debounced-save-ehto-hakukohteessa
                                                        application-key
                                                        hakukohde-oid])))}
                     [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
                      (if (= selected-koodi koodi)
                        [:i.zmdi.zmdi-check.zmdi-hc-stack-1x]
                        [:i.zmdi.zmdi-hc-stack-1x])]
                     label])
                  @koodit))
           [:div.application-handling__review-state-list.application-handling__review-state-list--closed
            [:div.application-handling__review-state-row.application-handling__review-state-row--selected
             (if disabled?
               {:class " application-handling__review-state-row--disabled"}
               {:on-click #(reset! open? true)})
             [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
              [:i.zmdi.zmdi-check.zmdi-hc-stack-1x]]
             selected-koodi-label]])]))))

(defn- lang-left
  [langs]
  (vec (cons (last langs) (butlast langs))))

(defn- lang-right
  [langs]
  (conj (vec (rest langs)) (first langs)))

(defn- lang-to
  [langs lang]
  (if (= lang (first langs))
    langs
    (lang-to (lang-right langs) lang)))

(defn- text-language
  [_ _ _ selected? _ _ _]
  (let [focus? (r/atom selected?)]
    (r/create-class
     {:component-did-update
      (fn [component]
        (when @focus?
          (.focus (r/dom-node component))))
      :reagent-render
      (fn [application-key hakukohde-oid lang selected? lang-to lang-left lang-right]
        (reset! focus? selected?)
        [:button.hyvaksynnan-ehto-texts__text-language
         {:id            (str "hyvaksynnan-ehto-texts__text-language-"
                              application-key "-" hakukohde-oid
                              "-language-" (name lang))
          :class         (when selected?
                           " hyvaksynnan-ehto-texts__text-language--selected")
          :role          "tab"
          :aria-controls (str "hyvaksynnan-ehto-texts__text-language-"
                              application-key "-" hakukohde-oid
                              "-text-" (name lang))
          :aria-selected selected?
          :tabIndex      (if selected? 0 -1)
          :on-click      (fn [_] (lang-to))
          :on-key-down   (fn [e]
                           (cond (= "ArrowLeft" (.-key e))
                                 (do (.preventDefault e)
                                     (lang-left))
                                 (= "ArrowRight" (.-key e))
                                 (do (.preventDefault e)
                                     (lang-right))))}
         (clojure.string/upper-case (name lang))])})))

(defn- ehto-text-textarea
  [_ _ _]
  (let [enabled?        (r/atom nil)
        previous-states (r/atom [[false false] [false false]])]
    (r/create-class
     {:component-did-update
      (fn [component]
        (let [current-state [@enabled?
                             (= (r/dom-node component)
                                (.-activeElement js/document))]]
          (if (and (= [[false false] [true true]] @previous-states)
                   (= [true false] current-state))
            (do (.focus (r/dom-node component))
                (reset! previous-states [[true true] (first @previous-states)]))
            (reset! previous-states [current-state (first @previous-states)]))))
      :reagent-render
      (fn [application-key hakukohde-oid selected-lang]
        (let [disabled? @(re-frame/subscribe [:hyvaksynnan-ehto/ehto-text-disabled? application-key])]
          (reset! enabled? (not disabled?))
          [:textarea.hyvaksynnan-ehto-texts__textarea
           {:id              (str "hyvaksynnan-ehto-texts__text-language-"
                                  application-key "-" hakukohde-oid
                                  "-text-" (name selected-lang))
            :role            "tabpanel"
            :aria-labelledby (str "hyvaksynnan-ehto-texts__text-language-"
                                  application-key "-" hakukohde-oid
                                  "-language-" (name selected-lang))
            :disabled        disabled?
            :rows            3
            :value           @(re-frame/subscribe [:hyvaksynnan-ehto/ehto-text
                                                   application-key
                                                   hakukohde-oid
                                                   selected-lang])
            :on-change       (fn [e]
                               (re-frame/dispatch [:hyvaksynnan-ehto/set-ehto-text
                                                   application-key
                                                   hakukohde-oid
                                                   selected-lang
                                                   (.-value (.-target e))])
                               (re-frame/dispatch [:hyvaksynnan-ehto/debounced-save-ehto-hakukohteessa
                                                   application-key
                                                   hakukohde-oid]))}]))})))

(defn- ehto-text
  [application-key hakukohde-oid selected-lang]
  [:div.hyvaksynnan-ehto-texts__textarea-container
   [ehto-text-textarea application-key hakukohde-oid selected-lang]
   (when @(re-frame/subscribe [:hyvaksynnan-ehto/requests-in-flight? application-key])
     [:div.hyvaksynnan-ehto-texts__spinner-overlay
      [:i.zmdi.zmdi-hc-3x.zmdi-spinner.spin]])])

(defn ehto-texts
  [_ _]
  (let [langs (r/atom [:fi :sv :en])]
    (fn [application-key hakukohde-oid]
      (let [selected-lang (first @langs)]
        [:div.hyvaksynnan-ehto-texts
         (into
          [:div.hyvaksynnan-ehto-texts__languages
           {:role "tablist"}]
          (mapv (fn [lang]
                  [text-language
                   application-key
                   hakukohde-oid
                   lang
                   (= lang selected-lang)
                   #(swap! langs lang-to lang)
                   #(swap! langs lang-left)
                   #(swap! langs lang-right)])
                [:fi :sv :en]))
         [ehto-text application-key hakukohde-oid selected-lang]]))))

(defn- ehto-valintatapajonoissa
  [application-key hakukohde-oid]
  (let [lang  @(re-frame/subscribe [:editor/virkailija-lang])
        ehdot @(re-frame/subscribe [:hyvaksynnan-ehto/valintatapajonoissa
                                    application-key
                                    hakukohde-oid])]
    (if @(re-frame/subscribe [:hyvaksynnan-ehto/show-single-ehto-valintatapajonoissa?
                              application-key
                              hakukohde-oid])
      [:div.hyvaksynnan-ehto-valintatapajonoissa
       [:p.hyvaksynnan-ehto-valintatapajonoissa__ehto-text
        (util/non-blank-val (second (first ehdot)) [lang :fi :sv :en])]]
      (into
       [:dl.hyvaksynnan-ehto-valintatapajonoissa]
       (mapcat (fn [[name ehto-text]]
                 [[:dt.hyvaksynnan-ehto-valintatapajonoissa__jono-name
                   name]
                  [:dd.hyvaksynnan-ehto-valintatapajonoissa__ehto-text
                   (util/non-blank-val ehto-text [lang :fi :sv :en])]])
               ehdot)))))

(defn hyvaksynnan-ehto
  [application-key hakukohde-oid]
  (into
   [:div.hyvaksynnan-ehto
    [ehdollisesti-hyvaksyttavissa application-key hakukohde-oid]]
   (if @(re-frame/subscribe [:hyvaksynnan-ehto/show-ehto-valintatapajonoissa?
                             application-key
                             hakukohde-oid])
     [[ehto-valintatapajonoissa application-key hakukohde-oid]]
     [[:div.hyvaksynnan-ehto-error
       (let [errors @(re-frame/subscribe [:hyvaksynnan-ehto/errors application-key])]
         (when (seq errors)
           [:span.hyvaksynnan-ehto-error__text
            @(re-frame/subscribe [:editor/virkailija-translation :operation-failed])]))]
      (when @(re-frame/subscribe [:hyvaksynnan-ehto/show-ehto-koodi?
                                  application-key
                                  hakukohde-oid])
        [ehto-koodi application-key hakukohde-oid])
      (when @(re-frame/subscribe [:hyvaksynnan-ehto/show-ehto-texts?
                                  application-key
                                  hakukohde-oid])
        [ehto-texts application-key hakukohde-oid])])))
