(ns ataru.hakija.application-handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [ataru.hakija.application-validators :as validator]
            [ataru.cljs-util :as util]
            [ataru.hakija.hakija-ajax :as ajax]
            [ataru.hakija.rules :as rules]
            [cljs.core.match :refer-macros [match]]
            [ataru.hakija.application :refer [create-initial-answers
                                              create-application-to-submit
                                              extract-wrapper-sections]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn initialize-db [_ _]
  {:form nil
   :application {:answers {}}})

(defn get-latest-form-by-key [db [_ form-key]]
  (ajax/get
    (str "/hakemus/api/form/" form-key)
    :application/handle-form)
  db)

(register-handler
  :application/get-latest-form-by-key
  get-latest-form-by-key)

(defn handle-submit [db _]
  (assoc-in db [:application :submit-status] :submitted))

(register-handler
  :application/handle-submit-response
  handle-submit)

(defn submit-application [db _]
  (ajax/post "/hakemus/api/application"
        (create-application-to-submit (:application db) (:form db) (get-in db [:form :selected-language]))
        :application/handle-submit-response)
  (assoc-in db [:application :submit-status] :submitting))

(register-handler
  :application/submit-form
  submit-application)

(def ^:private lang-pattern #"/(\w{2})$")

(defn- get-lang-from-path [supported-langs]
  ((set supported-langs)
   (some->> (util/get-path)
     (re-find lang-pattern)
     second
     keyword)))

(defn- set-form-language [form & [lang]]
  (let [supported-langs (:languages form)
        lang            (or lang
                          (get-lang-from-path supported-langs)
                          (first supported-langs))]
    (assoc form :selected-language lang)))

(defn- languages->kwd [form]
  (update form :languages
    (fn [languages]
      (mapv keyword languages))))

(defn handle-form [db [_ form]]
  (let [form (-> (languages->kwd form)
                 (set-form-language))]
    (-> db
        (assoc :form form)
        (assoc :application {:answers (create-initial-answers form)})
        (assoc :wrapper-sections (extract-wrapper-sections form)))))

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

(defn set-application-field [db [_ key idx values]]
  (let [path                [:application :answers key]
        current-answer-data (get-in db path)]
    (assoc-in db path (merge current-answer-data values))))

(register-handler
  :application/set-application-field
  set-application-field)

(register-handler
  :application/set-repeatable-application-field
  (fn [db [_ key idx {:keys [value valid] :as values}]]
    (let [path                [:application :answers key :values]]
      (if (and
            (zero? idx)
            (empty? value)
            (= 1 (count (get-in db path))))
        (assoc-in db path [])
        (update-in db path (fnil assoc []) idx values)))))

(register-handler
  :application/remove-repeatable-application-field-value
  (fn [db [_ key idx]]
    (update-in db [:application :answers key :values]
      (fn [values]
        (vec
          (filter identity (map-indexed #(if (not= %1 idx) %2) values)))))))

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
