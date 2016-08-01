(ns ataru.virkailija.application.handlers
  (:require [ataru.virkailija.virkailija-ajax :as ajax]
            [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler register-sub]]
            [reagent.ratom :refer-macros [reaction]]
            [ataru.virkailija.autosave :as autosave]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]))

(register-handler
  :application/select-application
  (fn [db [_ application-id]]
    (dispatch [:application/fetch-application application-id])
    (-> db
        (assoc-in [:application :selected-id] application-id)
        (assoc-in [:application :selected-application] nil))))

(register-handler
  :application/fetch-applications
  (fn [db [_ form-id]]
    (ajax/http
      :get
      (str "/lomake-editori/api/applications/list?formId=" form-id)
      (fn [db aplications-response]
        (assoc-in db [:application :applications] (:applications aplications-response))))
    db))

(defn answers-indexed
  "Convert the rest api version of application to a version which application
  readonly-rendering can use (answers are indexed with key in a map)"
  [application]
  (let [answers    (:answers application)
        answer-map (into {} (map (fn [answer] [(keyword (:key answer)) answer])) answers)]
    (assoc application :answers answer-map)))

(defn update-application-details [db application-response]
  (-> db
      (assoc-in [:application :selected-application] (answers-indexed (:application application-response)))
      (assoc-in [:application :events] (:events application-response))
      (assoc-in [:application :review] (:review application-response))))

(defn review-autosave-predicate [current prev]
  (if (or (= current {}) (= prev {})) false ; Initial value before fetching
      (not= current prev)))

(defn start-application-review-autosave [db]
  (assoc-in
    db
    [:application :review-autosave]
    (autosave/interval-loop {:subscribe-path [:application :review]
                             :changed-predicate review-autosave-predicate
                             :handler (fn [current prev]
                                        (println "autosave current and prev:")
                                        (.log js/console current)
                                        (.log js/console prev))})))

(register-handler
  :application/fetch-application
  (fn [db [_ application-id]]
    (ajax/http
      :get
      (str "/lomake-editori/api/applications/" application-id)
      (fn [db application-response]
        (-> db
          (update-application-details application-response)
          (start-application-review-autosave))))
    db))
