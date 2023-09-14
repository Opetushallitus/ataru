(ns ataru.virkailija.dropdown
  (:require [reagent.core :as reagent]))

(defn multi-option
  [_ _ _]
  (let [open? (reagent/atom false)]
    (fn [label options on-change]
      (let [checked-count (count (filter first options))
            some-checked? (< 0 checked-count)]
        [:div.multi-option-dropdown__container
         [:div.multi-option-dropdown__dropdown
          {:class       (when some-checked?
                          "multi-option-dropdown__dropdown--highlighted")
           :tab-index   0
           :on-click    #(swap! open? not)
           :on-key-down (fn [e]
                          (cond (or (= " " (.-key e))
                                    (= "Enter" (.-key e)))
                                (do (.preventDefault e)
                                    (swap! open? not))
                                (and @open?
                                     (= "Escape" (.-key e)))
                                (do (.preventDefault e)
                                    (reset! open? false))))}
          (when some-checked?
            [:div.multi-option-dropdown__dropdown-count checked-count])
          [:div.multi-option-dropdown__dropdown-label label]
          [:i.multi-option-dropdown__dropdown-caret
           {:class (str (if @open?
                          "zmdi zmdi-caret-up"
                          "zmdi zmdi-caret-down")
                        (when some-checked?
                          " multi-option-dropdown__dropdown-caret--highlighted"))}]]
         (when @open?
           (into [:ul.multi-option-dropdown__options
                  (map-indexed (fn [i [checked? label on-change-argument]]
                                 ^{:key (str "option-" i)}
                                 [:li.multi-option-dropdown__option
                                  [:label.multi-option-dropdown__option-label
                                   [:input.multi-option-dropdown__option-checkbox
                                    {:type        "checkbox"
                                     :checked     checked?
                                     :on-change   #(on-change on-change-argument)
                                     :on-key-down (fn [e]
                                                    (when (and @open?
                                                               (= "Escape" (.-key e)))
                                                      (.preventDefault e)
                                                      (reset! open? false)))}]
                                   label]])
                               options)]))]))))
