(ns ataru.virkailija.date-time-picker
  (:require [cljs-time.format :as f]
            [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [re-frame.core :as re-frame]))

(defn- iso-date-string->finnish-date-string [date-s]
  (if (= "" date-s)
    ""
    (try
      (->> date-s
           (f/parse (f/formatter "yyyy-MM-dd"))
           (f/unparse (f/formatter "d.M.yyyy")))
      (catch js/Error e
        nil))))

(defn- finnish-date-string->iso-date-string [date-s]
  (cond (= "" date-s)
        ""
        (nil? (re-matches #"\d{1,2}\.\d{1,2}\.\d{4}" date-s))
        nil
        :else
        (try
          (->> date-s
               (f/parse (f/formatter "d.M.yyyy"))
               (f/unparse (f/formatter "yyyy-MM-dd")))
          (catch js/Error e
            nil))))

(defn- iso-time-string->finnish-time-string [time-s]
  (if (= "" time-s)
    ""
    (try
      (->> time-s
           (f/parse (f/formatter "HH:mm"))
           (f/unparse (f/formatter "H.mm")))
      (catch js/Error e
        nil))))

(defn- finnish-time-string->iso-time-string [time-s]
  (cond (= "" time-s)
        ""
        (nil? (re-matches #"\d{1,2}\.\d{2}" time-s))
        nil
        :else
        (try
          (->> time-s
               (f/parse (f/formatter "H.mm"))
               (f/unparse (f/formatter "HH:mm")))
          (catch js/Error e
            nil))))

(defn date-picker
  [id class value invalid on-change]
  (let [supports-date?           (reagent/atom nil)
        input-value              (reagent/atom "")
        valid?                   (reagent/atom true)
        invalid-text             (reagent/atom invalid)
        invalid-date-format-i18n (re-frame/subscribe [:editor/virkailija-translation :invalid-date-format])]
    (reagent/create-class
     {:component-did-mount
      (fn [component]
        (let [dom-node (reagent-dom/dom-node component)]
          (.setCustomValidity
           dom-node
           (if (not @valid?)
             @invalid-date-format-i18n
             @invalid-text))
          (reset! supports-date? (= "date" (.-type dom-node)))))
      :component-did-update
      (fn [component]
        (.setCustomValidity
         (reagent-dom/dom-node component)
         (if (not @valid?)
           @invalid-date-format-i18n
           @invalid-text)))
      :reagent-render
      (fn [id class value invalid on-change]
        (reset! invalid-text invalid)
        [:input
         {:id          id
          :class       class
          :type        "date"
          :placeholder "p.k.vvvv"
          :size        8
          :value       (case @supports-date?
                         nil   ""
                         true  (if @valid? value @input-value)
                         false (if @valid? (iso-date-string->finnish-date-string value) @input-value))
          :on-change   (fn [e]
                         (reset! input-value (.-value (.-target e)))
                         (if-let [value (cond-> @input-value
                                                (not= "date" (.-type (.-target e)))
                                                finnish-date-string->iso-date-string)]
                           (do (reset! valid? true)
                               (on-change value))
                           (reset! valid? false)))}])})))

(defn time-picker
  [id class value invalid on-change]
  (let [supports-time?           (reagent/atom nil)
        input-value              (reagent/atom "")
        valid?                   (reagent/atom true)
        invalid-text             (reagent/atom invalid)
        invalid-date-format-i18n (re-frame/subscribe [:editor/virkailija-translation :invalid-date-format])]
    (reagent/create-class
     {:component-did-mount
      (fn [component]
        (let [dom-node (reagent-dom/dom-node component)]
          (.setCustomValidity
           dom-node
           (if (not @valid?)
             @invalid-date-format-i18n
             @invalid-text))
          (reset! supports-time? (= "time" (.-type dom-node)))))
      :component-did-update
      (fn [component]
        (.setCustomValidity
         (reagent-dom/dom-node component)
         (if (not @valid?)
           @invalid-date-format-i18n
           @invalid-text)))
      :reagent-render
      (fn [id class value invalid on-change]
        (reset! invalid-text invalid)
        [:input
         {:id          id
          :class       class
          :type        "time"
          :placeholder "h.mm"
          :size        4
          :value       (case @supports-time?
                         nil   ""
                         true  (if @valid? value @input-value)
                         false (if @valid? (iso-time-string->finnish-time-string value) @input-value))
          :on-change   (fn [e]
                         (reset! input-value (.-value (.-target e)))
                         (if-let [value (cond-> @input-value
                                                (not= "time" (.-type (.-target e)))
                                                finnish-time-string->iso-time-string)]
                           (do (reset! valid? true)
                               (on-change value))
                           (reset! valid? false)))}])})))
