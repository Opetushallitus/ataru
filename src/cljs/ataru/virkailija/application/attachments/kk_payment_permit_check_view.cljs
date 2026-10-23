(ns ataru.virkailija.application.attachments.kk-payment-permit-check-view
  (:require [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.virkailija.application.attachments.kk-payment-permit-check-handlers :as handlers]
            [ataru.virkailija.date-time-picker :as date-time-picker]
            [cljs-time.format :as f]
            [re-frame.core :as re-frame]))

(def ^:private migri-permit-service-url "https://miso1.sovellukset.migri.fi")

(defn- translation [key]
  @(re-frame/subscribe [:editor/virkailija-translation key]))

(defn- field-id [application-key attachment-key field]
  (str "kk-payment-permit-check-" (name field) "-" application-key "-" (name attachment-key)))

(defn- set-field [application-key attachment-key field value save]
  (re-frame/dispatch [:kk-payment-permit-check/set-field application-key attachment-key field value save]))

(defn- migri-link []
  [:a {:href   migri-permit-service-url
       :target "_blank"
       :rel    "noopener noreferrer"}
   (translation :kk-payment-permit-check-migri-service)
   [:span.kk-payment-permit-check__visually-hidden
    (str " " (translation :kk-payment-permit-check-opens-in-new-tab))]])

; Same as the notifications of the application heading
(defn- notification [content]
  [:div.application__message-display.application__message-display--notification.kk-payment-permit-check__notification
   [:div.application__message-display--exclamation [:i.zmdi.zmdi-alert-triangle {:aria-hidden true}]]
   [:div.application__message-display--details
    (into [:div] content)]])

(defn- radio [{:keys [id name checked? disabled? on-change label]}]
  [:div.kk-payment-permit-check__radio
   [:input {:id        id
            :type      "radio"
            :name      name
            :checked   checked?
            :disabled  disabled?
            :on-change on-change}]
   [:label {:for id} label]])

; Shown after the label of a set field, as a selection of radio buttons cannot be removed otherwise. As the button
; disappears, the focus is moved to the first radio button of the field.
(defn- clear-button [application-key attachment-key check disabled? field label-key focus-id]
  (when (and (not disabled?) (some? (get check field)))
    [:button.kk-payment-permit-check__clear
     {:type       "button"
      :aria-label @(re-frame/subscribe [:editor/virkailija-translation :kk-payment-permit-check-clear
                                        (translation label-key)])
      :on-click   (fn []
                    (re-frame/dispatch [:kk-payment-permit-check/clear-field application-key attachment-key field])
                    (some-> (.getElementById js/document focus-id) .focus))}
     [:i.material-icons-outlined {:aria-hidden true} "edit_off"]]))

; The legend contains the clear button, so the field is named by the label text alone
(defn- legend [name label clear-button]
  [:legend.kk-payment-permit-check__label
   [:span {:id (str name "-label")} label]
   clear-button])

(defn- found-in-migri [application-key attachment-key check disabled?]
  (let [name (field-id application-key attachment-key :found-in-migri)]
    [:fieldset.kk-payment-permit-check__field {:aria-labelledby (str name "-label")}
     [legend name
      (translation :kk-payment-permit-check-found-in-migri)
      [clear-button application-key attachment-key check disabled? :found-in-migri
       :kk-payment-permit-check-found-in-migri (str name "-" true)]]
     [:div.kk-payment-permit-check__radios--inline
      (for [[value label-key] [[true :kk-payment-permit-check-yes] [false :kk-payment-permit-check-no]]]
        ^{:key (str value)}
        [radio {:id        (str name "-" value)
                :name      name
                :checked?  (= value (:found-in-migri check))
                :disabled? disabled?
                :on-change #(set-field application-key attachment-key :found-in-migri value :immediate)
                :label     (translation label-key)}])]]))

(defn- label-text [label-key required?]
  (str (translation label-key) (when required? " *")))

(defn- finnish-date [iso-date]
  (f/unparse-local-date (f/formatter "d.M.yyyy") (f/parse-local-date (f/formatter "yyyy-MM-dd") iso-date)))

(defn- permit-valid-until [application-key attachment-key check disabled? required?]
  (let [id                (field-id application-key attachment-key :permit-valid-until)
        [earliest latest] (handlers/permit-valid-until-range)
        valid?            @(re-frame/subscribe [:kk-payment-permit-check/permit-valid-until-valid?
                                                application-key attachment-key])]
    [:div.kk-payment-permit-check__field
     [:label.kk-payment-permit-check__label {:for id} (label-text :kk-payment-permit-check-valid-until required?)]
     [date-time-picker/date-picker
      id
      "kk-payment-permit-check__date-input"
      (or (:permit-valid-until check) "")
      ""
      (fn [value]
        (set-field application-key attachment-key :permit-valid-until (not-empty value) :debounced))
      {:disabled?        disabled?
       :min              earliest
       :max              latest
       :aria-required    required?
       :aria-invalid     (not valid?)}]
     (when-not valid?
       [:div.kk-payment-permit-check__error {:role "alert"}
        @(re-frame/subscribe [:editor/virkailija-translation :kk-payment-permit-check-valid-until-out-of-range
                              (finnish-date earliest) (finnish-date latest)])])]))

(defn- payment-obligation-basis [application-key attachment-key check disabled? required?]
  (let [name (field-id application-key attachment-key :payment-obligation-basis)]
    [:fieldset.kk-payment-permit-check__field {:aria-labelledby (str name "-label")}
     [legend name
      (label-text :kk-payment-permit-check-obligation required?)
      [clear-button application-key attachment-key check disabled? :payment-obligation-basis
       :kk-payment-permit-check-obligation (str name "-" (first payment-module/kk-payment-obligation-bases))]]
     (for [value payment-module/kk-payment-obligation-bases]
       ^{:key value}
       [radio {:id        (str name "-" value)
               :name      name
               :checked?  (= value (:payment-obligation-basis check))
               :disabled? disabled?
               :on-change #(set-field application-key attachment-key :payment-obligation-basis value :immediate)
               :label     (translation (keyword (str "kk-payment-permit-check-" value)))}])
     (when (= "obligated-other" (:payment-obligation-basis check))
       [:div.kk-payment-permit-check__other-reason-container
        [:input.kk-payment-permit-check__other-reason
         {:type        "text"
          :aria-label  (translation :kk-payment-permit-check-other-reason)
          :placeholder (translation :kk-payment-permit-check-other-reason)
          :value       (or (:payment-obligation-other-reason check) "")
          :max-length  payment-module/kk-payment-permit-check-other-reason-max-length
          :disabled    disabled?
          :on-change   #(set-field application-key
                                   attachment-key
                                   :payment-obligation-other-reason
                                   (.. % -target -value)
                                   :none)
          :on-blur     #(re-frame/dispatch [:kk-payment-permit-check/save-now application-key attachment-key])}]])]))

(defn permit-check-notification
  "Guidance on checking a kk application payment residence permit, depending on whether the applicant has delivered
   the attachment."
  [attachment-delivered?]
  (if attachment-delivered?
    [notification [(str (translation :kk-payment-permit-check-attachment-delivered) " ")
                   [migri-link]
                   "."]]
    [notification [(str (translation :kk-payment-permit-check-attachment-missing) " ")
                   [migri-link]
                   (str " " (translation :kk-payment-permit-check-attachment-missing-suffix))]]))

(defn permit-check
  "Virkailija's notes on a kk application payment residence permit attachment."
  [application-key attachment-key attachment-checked? can-edit?]
  (let [check       @(re-frame/subscribe [:kk-payment-permit-check/check application-key attachment-key])
        disabled?   (not can-edit?)
        add-info?   (or (true? (:found-in-migri check)) attachment-checked?)
        info-added? (and (some? (:permit-valid-until check))
                         @(re-frame/subscribe [:kk-payment-permit-check/permit-valid-until-valid?
                                               application-key attachment-key])
                         (some? (:payment-obligation-basis check)))]
    [:div.kk-payment-permit-check
     [found-in-migri application-key attachment-key check disabled?]
     (when (and add-info? (not info-added?))
       [notification [(translation :kk-payment-permit-check-add-info)]])
     [permit-valid-until application-key attachment-key check disabled? add-info?]
     [payment-obligation-basis application-key attachment-key check disabled? add-info?]
     (when @(re-frame/subscribe [:kk-payment-permit-check/save-error? application-key attachment-key])
       [:div.kk-payment-permit-check__error {:role "alert"}
        (translation :kk-payment-permit-check-save-error)
        [:button.kk-payment-permit-check__retry
         {:type     "button"
          :on-click #(re-frame/dispatch [:kk-payment-permit-check/save-now application-key attachment-key])}
         (translation :kk-payment-permit-check-retry)]])]))
