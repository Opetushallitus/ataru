(ns ataru.application-common.components.dropdown-keyboard
  "Näppäimistönavigointi pudotusvalikon syötekentässä."
  (:require [ataru.application-common.components.dropdown-actions :as actions]))

(defn make-on-input-key-down
  [{:keys [dropdown-id expanded? active-index selected-index last-option-index
           active-option open-popup move-active-to set-active-index on-option-click]}]
  (fn on-input-key-down [e]
    (cond
      (= "Escape" (.-key e))
      (do (.preventDefault e)
          (actions/collapse-dropdown {:dropdown-id dropdown-id}))

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
