(ns ataru.hakija.application-handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [ataru.hakija.application-validators :as validator]
            [ataru.hakija.hakija-ajax :refer [get post]]
            [ataru.hakija.rules :as rules]
            [cljs.core.match :refer-macros [match]]
            [ataru.hakija.application :refer [create-initial-answers
                                              create-application-to-submit
                                              extract-wrapper-sections]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn initialize-db [_ _]
  {:form nil
   :application {:answers {}}})

(defn get-form [db [_ form-id]]
  (get
    (str "/hakemus/api/form/" form-id)
    :application/handle-form)
  db)

(register-handler
  :application/get-form
  get-form)

(defn handle-submit [db _]
  (assoc-in db [:application :submit-status] :submitted))

(register-handler
  :application/handle-submit-response
  handle-submit)

(defn submit-application [db _]
  (post "/hakemus/api/application"
        (create-application-to-submit (:application db) (:form db) "fi")
        :application/handle-submit-response)
  (assoc-in db [:application :submit-status] :submitting))

(register-handler
  :application/submit-form
  submit-application)

(defn handle-form [db [_ form]]
  (-> db
    (assoc :form form)
    (assoc :application {:answers (create-initial-answers form)})
    (assoc :wrapper-sections (extract-wrapper-sections form))))

(register-handler
  :flasher
  (fn [db [_ flash]]
    (assoc db :flasher flash)))

(register-handler
  :application/handle-form
  handle-form)

(register-handler
  :application/initialize-db
  initialize-db)

(defn set-application-field [db [_ key values]]
  (let [path                [:application :answers key]
        current-answer-data (get-in db path)]
    (assoc-in db path (merge current-answer-data values))))

(register-handler
  :application/set-application-field
  set-application-field)

(defn default-error-handler [db [_ response]]
  (assoc db :error {:message "Tapahtui virhe" :detail (str response)}))

(defn application-run-rule [db rule]
  (if (not-empty rule)
    (rules/run-rule rule db)
    (rules/run-all-rules db)))

(register-handler
  :application/run-rule
  (fn [db [_ rule]]
    (if (#{:submitting :submitted} (-> db :application :submit-status))
      db
      (application-run-rule db rule))))

(register-handler
  :application/default-handle-error
  default-error-handler)

(register-handler
 :application/default-http-ok-handler
 (fn [db _] db))

(register-handler
  :state-update
  (fn [db [_ f]]
    (or (f db)
        db)))

(register-handler
  :application/handle-postal-code-input
  (fn [db [_ postal-office-name]]
    (-> db
        (update-in [:application :ui :postal-office] assoc :disabled? true)
        (update-in [:application :answers :postal-office] merge {:value (:fi postal-office-name) :valid true}))))

(register-handler
  :application/handle-postal-code-error
  (fn [db _]
    (-> db
        (update-in [:application :answers :postal-office] merge {:value "" :valid false}))))

(register-handler
  :application/toggle-multiple-choice-option
  (fn [db [_ multiple-choice-id idx option-value validators]]
    (let [db    (-> db
                    (assoc-in [:application :answers multiple-choice-id :options idx :value] option-value)
                    (update-in [:application :answers multiple-choice-id :options idx :selected] not))
          value (->> (get-in db [:application :answers multiple-choice-id :options])
                     (vals)
                     (filter :selected)
                     (map :value)
                     (clojure.string/join ", "))
          valid (if (not-empty validators)
                  (every? true? (map #(validator/validate % value) validators))
                  true)]
      (-> db
          (assoc-in [:application :answers multiple-choice-id :value] value)
          (assoc-in [:application :answers multiple-choice-id :valid] valid)))))
