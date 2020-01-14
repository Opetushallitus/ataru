(ns ataru.virkailija.application.attachments.liitepyynto-information-request-view
  (:require [ataru.virkailija.date-time-picker :as date-time-picker]
            ataru.virkailija.application.attachments.liitepyynto-information-request-subs
            ataru.virkailija.application.attachments.liitepyynto-information-request-handlers
            [re-frame.core :as re-frame]))

(defn send-toggle [application-key liitepyynto-key]
  (let [on?      @(re-frame/subscribe
                   [:liitepyynto-information-request/deadline-toggle-on?
                    application-key
                    liitepyynto-key])
        on-click (fn [_]
                   (re-frame/dispatch
                    [:liitepyynto-information-request/set-deadline-toggle
                     application-key
                     liitepyynto-key
                     (not on?)]))]
    [:div.send-liitepyynto-information-request-toggle
     {:id           (str "liitepyynto-information-request-toggle-"
                         application-key "-"
                         (name liitepyynto-key))
      :role         "switch"
      :aria-checked on?
      :tabIndex     0
      :on-click     on-click
      :on-key-press (fn [e]
                      (when (= " " (.-key e))
                        (.preventDefault e)
                        (on-click nil)))}
     [:div.send-liitepyynto-information-request-toggle__slider
      {:class (if on?
                " send-liitepyynto-information-request-toggle__slider--on"
                " send-liitepyynto-information-request-toggle__slider--off")}
      [:div.send-liitepyynto-information-request-toggle__divider]]]))

(defn send-toggle-lable [application-key liitepyynto-key]
  [:label.send-liitepyynto-information-request-toggle__label
   {:for (str "liitepyynto-information-request-toggle-"
              application-key "-"
              (name liitepyynto-key))}
   @(re-frame/subscribe [:editor/virkailija-translation :liitepyynto-deadline])])

(defn deadline-date-input [application-key liitepyynto-key]
  (let [value     (re-frame/subscribe
                    [:liitepyynto-information-request/deadline-date
                     application-key
                     liitepyynto-key])
        on-change (fn [value]
                    (re-frame/dispatch
                      [:liitepyynto-information-request/set-deadline-date
                       application-key
                       liitepyynto-key
                       value]))]
    [date-time-picker/date-picker
     (str "liitepyynto-deadline__date-input-"
          application-key "-" (name liitepyynto-key))
     "liitepyynto-deadline__date-input"
     @value
     (if (= "" @value) @(re-frame/subscribe [:editor/virkailija-translation :required]) "")
     on-change]))

(defn deadline-time-input [application-key liitepyynto-key]
  (let [value     (re-frame/subscribe
                    [:liitepyynto-information-request/deadline-time
                     application-key
                     liitepyynto-key])
        on-change (fn [value]
                    (re-frame/dispatch
                      [:liitepyynto-information-request/set-deadline-time
                       application-key
                       liitepyynto-key
                       value])
                    (re-frame/dispatch
                      [:liitepyynto-information-request/debounced-save-deadline
                       application-key
                       liitepyynto-key]))]
    [date-time-picker/time-picker
     (str "liitepyynto-deadline__time-input-"
          application-key "-" (name liitepyynto-key))
     "liitepyynto-deadline__time-input"
     @value
     (if (= "" @value) @(re-frame/subscribe [:editor/virkailija-translation :required]) "")
     on-change]))

(defn deadline-date-label [application-key liitepyynto-key]
  [:label.liitepyynto-deadline__date-label
   {:for (str "liitepyynto-deadline__date-input-"
              application-key "-" (name liitepyynto-key))}
   @(re-frame/subscribe [:editor/virkailija-translation :liitepyynto-deadline-date])])

(defn deadline-time-label [application-key liitepyynto-key]
  [:label.liitepyynto-deadline__time-label
   {:for (str "liitepyynto-deadline__time-input-"
              application-key "-" (name liitepyynto-key))}
   @(re-frame/subscribe [:editor/virkailija-translation :liitepyynto-deadline-time])])

(defn deadline-error []
  [:span.liitepyynto-deadline__error
   @(re-frame/subscribe [:editor/virkailija-translation :liitepyynto-deadline-error])])
