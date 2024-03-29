(ns ataru.virkailija.application.tutu-payment.tutu-payment-view
  (:require [re-frame.core :refer [subscribe dispatch]]
            [cljs-time.format :as format]
            [clojure.string :as string]
            [ataru.virkailija.application.tutu-payment.tutu-payment-handlers]
            [ataru.virkailija.application.tutu-payment.tutu-payment-subs]
            [ataru.virkailija.application.view.virkailija-application-icons :as icons]
            [ataru.virkailija.date-time-picker :as date-time-picker]))

(def date-formatter (format/formatters :date))

(def fi-formatter (format/formatter "dd.MM.yyyy"))

(defn- iso-date-str->date [date-str]
  (when-not (string/blank? date-str)
    (try
      (format/parse-local date-formatter date-str)
      (catch js/Error _))))

(defn- format-date [iso-date-str]
  (when-let [date (iso-date-str->date iso-date-str)]
    (format/unparse fi-formatter date)))

(defn- date-picker [application-key]
  (let [value     (subscribe [:tutu-payment/duedate-input application-key])
        on-change (fn [value]
                    (dispatch [:tutu-payment/set-duedate application-key value]))]
    [date-time-picker/date-picker
     (str "application-handling__tutu-payment-duedate-input-"
          application-key)
     "application-handling__tutu-payment-duedate-input"
     @value
     (if (= "" @value) @(subscribe [:editor/virkailija-translation :required]) "")
     on-change]))

(defn- decision-payment-note [application-key]
  (let [value       (subscribe [:tutu-payment/note-input application-key])]
    (fn []
      [:div.application-handling__review-row.application-handling__review-row--notes-row
       [:textarea.application-handling__review-note-input
        {:type      "text"
         :value     @value
         :on-change #(dispatch [:tutu-payment/set-note-input
                                application-key
                                (.. % -target -value)])}]])))

(defn- amount-input
  [application-key placeholder disabled?]
  (let [amount @(subscribe [:tutu-payment/amount-input application-key])]
    [(if disabled?
       :div.question-search-search-input.question-search-search-input--disabled
       :div.question-search-search-input)
     [:input.question-search-search-input__input
      {:type        "text"
       :value       amount
       :placeholder placeholder
       :pattern     "[0-9]{1,4}"
       :title       @(subscribe [:editor/virkailija-translation :tutu-amount-input-placeholder])
       :disabled    disabled?
       :on-change   #(dispatch [:tutu-payment/set-amount
                                application-key
                                (.. % -target -value)])}]
     [:span (str "€")]
     ]))

(defn- single-payment-status-row [header payment]
  (let [status     (keyword (:status payment))
        icon       (case (keyword status)
                     :active  icons/tutu-payment-outstanding
                     :paid    icons/tutu-payment-paid
                     :overdue icons/tutu-payment-overdue
                     nil)
        label      (if (or (empty? payment) (nil? status))
                     @(subscribe [:editor/virkailija-translation :tutu-invoice-notfound])
                     (case (keyword status)
                       :active  @(subscribe [:editor/virkailija-translation :tutu-payment-active])
                       :paid    (str @(subscribe [:editor/virkailija-translation :tutu-payment-paid]) " " (format-date (:paid_at payment)))
                       :overdue (str @(subscribe [:editor/virkailija-translation :tutu-payment-overdue]) " " (format-date (:due_date payment)))
                       @(subscribe [:editor/virkailija-translation :tutu-payment-unknown])))]
    [:<>
     [:div header]
     [:div
      (when icon [icon])
      (str label)
      (when (= :paid (keyword (:status payment)))
        [:a
         {:href (str "/lomake-editori/api/maksut/kuitti/" (:order_id payment))
          :download (str (:order_id payment) ".html")
          :title @(subscribe [:editor/virkailija-translation :tutu-payment-download-receipt])}
         [:i.application-handling__tutu-receipt-icon.zmdi.zmdi-download.zmdi-hc-lg]])
      ]
     ]
    ))

(defn- resend-processing-invoice-button []
  (let [loading?          (subscribe [:state-query [:request-handles :resend-processing-invoice]])
        can-edit?         (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])]
    [:button.application-handling__tutu-payment-send-button.application-handling__button
     {:on-click #(dispatch [:tutu-payment/resend-processing-invoice])
      :disabled (or @loading? (not @can-edit?))
      :class    (str (if (and (not @loading?) @can-edit?)
                       "application-handling__send-information-request-button--enabled"
                       "application-handling__send-information-request-button--disabled")
                     (if (not @can-edit?)
                       " application-handling__send-information-request-button--cursor-default"
                       " application-handling__send-information-request-button--cursor-pointer"))}
     [:div (if @loading?
             [:span [:i.zmdi.zmdi-spinner.spin]]
             @(subscribe [:editor/virkailija-translation :tutu-kasittelymaksu-button]))]]))

(defn- send-decision-invoice-button [application-key decision-pay-status]
  (let [filled?           (subscribe [:tutu-payment/inputs-filled? application-key])
        loading?          (subscribe [:state-query [:request-handles :send-decision-invoice]])
        can-edit?         (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])]
    [:button.application-handling__tutu-payment-send-button.application-handling__button
     {:on-click #(dispatch [:tutu-payment/send-decision-invoice application-key])
      :disabled (or @loading? (not @filled?) (not @can-edit?))
      :class    (if (and @filled? @can-edit? (not @loading?))
                  "application-handling__send-information-request-button--enabled application-handling__send-information-request-button--cursor-pointer"
                  "application-handling__send-information-request-button--disabled application-handling__send-information-request-button--cursor-default")
      }
     [:div (cond
             @loading? [:span [:i.zmdi.zmdi-spinner.spin]]
             (= :active decision-pay-status) @(subscribe [:editor/virkailija-translation :tutu-maksupyynto-again-button])
             :else @(subscribe [:editor/virkailija-translation :tutu-maksupyynto-send-button]))]]))

(defn application-tutu-payment-status [payments]
  (let [loading?             @(subscribe [:state-query [:request-handles :fetch-payments]])
        email                @(subscribe [:state-query [:application :selected-application-and-form :application :answers :email :value]])
        application-key      @(subscribe [:state-query [:application :review :application-key]])
        processing-state     @(subscribe [:state-query [:application :review :hakukohde-reviews :form :processing-state]])
        {:keys [processing decision]} payments

        processing-pay-status (keyword (:status processing))
        decision-pay-status  (keyword (:status decision))
        state                (or (keyword processing-state) :unprocessed)
        amount-label         (case state
                               :unprocessed @(subscribe [:editor/virkailija-translation :tutu-amount-label])
                               :processing-fee-paid @(subscribe [:editor/virkailija-translation :tutu-amount-label])
                               :processing @(subscribe [:editor/virkailija-translation :tutu-amount-label])
                               :decision-fee-outstanding @(subscribe [:editor/virkailija-translation :tutu-amount-label])
                               :decision-fee-paid @(subscribe [:editor/virkailija-translation :tutu-total-paid-label])
                               nil)
        amount-value         (case state
                               :unprocessed (:amount processing)
                               :processing-fee-paid (:amount processing)
                               :processing :input
                               :decision-fee-outstanding :input
                               :decision-fee-paid (str (:amount processing) " + " (:amount decision))
                               nil)
        due-label         (case state
                            :unprocessed @(subscribe [:editor/virkailija-translation :tutu-due-label])
                            :processing-fee-paid nil
                            :processing @(subscribe [:editor/virkailija-translation :tutu-due-label])
                            :decision-fee-outstanding @(subscribe [:editor/virkailija-translation :tutu-due-label])
                            :decision-fee-paid nil
                            nil)
        due-value         (case state
                            :unprocessed (format-date (:due_date processing))
                            :processing-fee-paid nil
                            :processing :input
                            :decision-fee-outstanding (format-date (:due_date decision))
                            :decision-fee-paid nil
                            nil)]

    [:div.application-handling__tutu-payment-maksupyynto-box
     [:span.application-handling__tutu-payment--span-2
      [:b @(subscribe [:editor/virkailija-translation :tutu-maksupyynto-header])]]

     (if loading?
       [:div.application-handling__tutu-payment-maksupyynto-spinner
        [:i.zmdi.zmdi-spinner.spin]]
       [:<>
        [single-payment-status-row @(subscribe [:editor/virkailija-translation :tutu-processing-header]) (:processing payments)]
        (when-let [p (:decision payments)]
          [single-payment-status-row @(subscribe [:editor/virkailija-translation :tutu-decision-header]) p])

        [:div @(subscribe [:editor/virkailija-translation :tutu-maksupyynto-recipient])]
        [:div email]

        (when (and amount-label amount-value)
          [:<>
           [:div (str amount-label ":")]
           [:div (cond
                   (string? amount-value) (str amount-value " €")
                   (number? amount-value) (str amount-value " €")
                   (= amount-value :input) [amount-input application-key @(subscribe [:editor/virkailija-translation :tutu-maksupyynto-amount]) false])]
           ])

        (when (and due-label due-value)
          [:<>
           [:div (str due-label ":")]
           [:div (cond
                   (number? due-value) (str due-value)
                   (string? due-value) due-value
                   (= due-value :input) [date-picker application-key])]])

        (when (= :active processing-pay-status)
          [resend-processing-invoice-button])

        (when (cond
                (= :paid decision-pay-status) false
                (= :overdue decision-pay-status) false
                (#{:processing :decision-fee-outstanding} state) true)
          [:<>
           [:div @(subscribe [:editor/virkailija-translation :tutu-maksupyynto-message])]
           [decision-payment-note application-key]

           [send-decision-invoice-button application-key decision-pay-status]
           ])])
     ]))