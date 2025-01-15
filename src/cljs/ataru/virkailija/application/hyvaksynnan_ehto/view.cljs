(ns ataru.virkailija.application.hyvaksynnan-ehto.view
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [reagent.dom :as r-dom]
            [ataru.util :as util]
            ataru.virkailija.application.hyvaksynnan-ehto.subs
            clojure.string))

(defn ehdollisesti-hyvaksyttavissa
  [application-key hakukohde-oids]
  (let [ehdollisesti-hyvaksyttavissa? @(re-frame/subscribe [:hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa? application-key])
        disabled?                     @(re-frame/subscribe [:hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa-disabled? application-key hakukohde-oids])
        checked?                      (= ehdollisesti-hyvaksyttavissa? :hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa)]
    [:div.hyvaksynnan-ehto-ehdollisesti-hyvaksyttavissa
     [:input.hyvaksynnan-ehto-ehdollisesti-hyvaksyttavissa__checkbox
      {:id        (str "hyvaksynnan-ehto-ehdollisesti-hyvaksyttavissa-" application-key)
       :type      "checkbox"
       :disabled  disabled?
       :checked   checked?
       :on-change (fn [_]
                    (re-frame/dispatch [:hyvaksynnan-ehto/set-ehdollisesti-hyvaksyttavissa
                                        application-key
                                        hakukohde-oids
                                        (not checked?)])
                    (when checked?
                      (re-frame/dispatch [:hyvaksynnan-ehto/delete-ehto-hakukohteissa
                                          application-key
                                          hakukohde-oids])))}]
     [:label.hyvaksynnan-ehto-ehdollisesti-hyvaksyttavissa__label
      {:for (str "hyvaksynnan-ehto-ehdollisesti-hyvaksyttavissa-" application-key)}
      @(re-frame/subscribe [:editor/virkailija-translation :ehdollisesti-hyvaksyttavissa])]]))

(defn ehto-koodi
  [_ _]
  (let [open?  (r/atom false)
        koodit (re-frame/subscribe [:hyvaksynnan-ehto/ehto-koodit])]
    (fn [application-key hakukohde-oids]
      (let [disabled?            @(re-frame/subscribe
                                   [:hyvaksynnan-ehto/ehto-text-disabled? application-key])
            selected-koodi       @(re-frame/subscribe
                                   [:hyvaksynnan-ehto/selected-ehto-koodi application-key])
            selected-koodi-label @(re-frame/subscribe
                                   [:hyvaksynnan-ehto/selected-ehto-koodi-label application-key])]
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
                                                        hakukohde-oids
                                                        koodi])
                                    (re-frame/dispatch [:hyvaksynnan-ehto/debounced-save-ehto-hakukohteissa
                                                        application-key
                                                        hakukohde-oids])))}
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
  [_ _ selected? _ _ _]
  (let [focus? (r/atom selected?)]
    (r/create-class
     {:component-did-update
      (fn [component]
        (when @focus?
          (.focus (r-dom/dom-node component))))
      :reagent-render
      (fn [application-key lang selected? lang-to lang-left lang-right]
        (reset! focus? selected?)
        [:button.hyvaksynnan-ehto-texts__text-language
         {:id            (str "hyvaksynnan-ehto-texts__text-language-"
                              application-key
                              "-language-" (name lang))
          :class         (when selected?
                           " hyvaksynnan-ehto-texts__text-language--selected")
          :role          "tab"
          :aria-controls (str "hyvaksynnan-ehto-texts__text-language-"
                              application-key
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
                             (= (r-dom/dom-node component)
                                (.-activeElement js/document))]]
          (if (and (= [[false false] [true true]] @previous-states)
                   (= [true false] current-state))
            (do (.focus (r-dom/dom-node component))
                (reset! previous-states [[true true] (first @previous-states)]))
            (reset! previous-states [current-state (first @previous-states)]))))
      :reagent-render
      (fn [application-key hakukohde-oids selected-lang]
        (let [disabled? @(re-frame/subscribe [:hyvaksynnan-ehto/ehto-text-disabled? application-key])]
          (reset! enabled? (not disabled?))
          [:textarea.hyvaksynnan-ehto-texts__textarea
           {:id              (str "hyvaksynnan-ehto-texts__text-language-"
                                  application-key
                                  "-text-" (name selected-lang))
            :role            "tabpanel"
            :aria-labelledby (str "hyvaksynnan-ehto-texts__text-language-"
                                  application-key
                                  "-language-" (name selected-lang))
            :disabled        disabled?
            :rows            3
            :value           @(re-frame/subscribe [:hyvaksynnan-ehto/ehto-text
                                                   application-key
                                                   selected-lang])
            :on-change       (fn [e]
                               (re-frame/dispatch [:hyvaksynnan-ehto/set-ehto-text
                                                   application-key
                                                   hakukohde-oids
                                                   selected-lang
                                                   (.-value (.-target e))])
                               (re-frame/dispatch [:hyvaksynnan-ehto/debounced-save-ehto-hakukohteissa
                                                   application-key
                                                   hakukohde-oids]))}]))})))

(defn- ehto-text
  [application-key hakukohde-oids selected-lang]
  [:div.hyvaksynnan-ehto-texts__textarea-container
   [ehto-text-textarea application-key hakukohde-oids selected-lang]
   (when @(re-frame/subscribe [:hyvaksynnan-ehto/requests-in-flight? application-key])
     [:div.hyvaksynnan-ehto-texts__spinner-overlay
      [:i.zmdi.zmdi-hc-3x.zmdi-spinner.spin]])])

(defn ehto-texts
  [_ _]
  (let [langs (r/atom [:fi :sv :en])]
    (fn [application-key hakukohde-oids]
      (let [selected-lang (first @langs)]
        [:div.hyvaksynnan-ehto-texts
         (into
          [:div.hyvaksynnan-ehto-texts__languages
           {:role "tablist"}]
          (mapv (fn [lang]
                  [text-language
                   application-key
                   lang
                   (= lang selected-lang)
                   #(swap! langs lang-to lang)
                   #(swap! langs lang-left)
                   #(swap! langs lang-right)])
                [:fi :sv :en]))
         [ehto-text application-key hakukohde-oids selected-lang]]))))

(defn- ehto-valintatapajonoissa
  [application-key hakukohde-oids]
  (let [lang  @(re-frame/subscribe [:editor/virkailija-lang])
        ehdot @(re-frame/subscribe [:hyvaksynnan-ehto/valintatapajonoissa application-key hakukohde-oids])]
    (if @(re-frame/subscribe [:hyvaksynnan-ehto/show-single-ehto-valintatapajonoissa? application-key hakukohde-oids])
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

(defn- ehdollisesti-hyvaksyttavissa-monta-arvoa []
  [:div.ehdollisesti-hyvaksyttavissa-monta-arvoa
   [:i.zmdi.zmdi-check-all.hyvaksynnan-ehto--multiple-values-check-mark]
   [:span (str @(re-frame/subscribe [:editor/virkailija-translation :ehdollisuus])
               ": "
               @(re-frame/subscribe [:editor/virkailija-translation :multiple-values]))]])

(defn hyvaksynnan-ehto
  [application-key hakukohde-oids]
  (let [ehdollisesti-hyvaksyttavissa? @(re-frame/subscribe [:hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa? application-key])
        multiple-values?              (= ehdollisesti-hyvaksyttavissa? :hyvaksynnan-ehto/monta-arvoa)]
    (into
      [:div.hyvaksynnan-ehto
       {:class (when multiple-values?
                 "hyvaksynnan-ehto--grayed-out-text")}
       (if multiple-values?
         [ehdollisesti-hyvaksyttavissa-monta-arvoa]
         [ehdollisesti-hyvaksyttavissa application-key hakukohde-oids])]
      (if @(re-frame/subscribe [:hyvaksynnan-ehto/show-ehto-valintatapajonoissa? application-key hakukohde-oids])
        [[ehto-valintatapajonoissa application-key hakukohde-oids]]
        [[:div.hyvaksynnan-ehto-error
          (let [errors     @(re-frame/subscribe [:hyvaksynnan-ehto/errors application-key])
                in-flight? @(re-frame/subscribe [:hyvaksynnan-ehto/requests-in-flight? application-key])]
            (when (and (seq errors) (not in-flight?))
              [:span.hyvaksynnan-ehto-error__text
               @(re-frame/subscribe [:editor/virkailija-translation :operation-failed])]))]
         (when @(re-frame/subscribe [:hyvaksynnan-ehto/show-ehto-koodi? application-key hakukohde-oids])
           [ehto-koodi application-key hakukohde-oids])
         (when @(re-frame/subscribe [:hyvaksynnan-ehto/show-ehto-texts? application-key hakukohde-oids])
           [ehto-texts application-key hakukohde-oids])]))))
