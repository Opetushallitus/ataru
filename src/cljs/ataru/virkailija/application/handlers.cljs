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
    (if (not= application-id (get-in db [:application :selected-id]))
      (do (dispatch [:application/fetch-application application-id])
        (-> db
            (assoc-in [:application :selected-id] application-id)
            (assoc-in [:application :selected-application] nil)))
      db)))

(register-handler
  :application/fetch-applications
  (fn [db [_ form-key]]
    (ajax/http
      :get
      (str "/lomake-editori/api/applications/list?formKey=" form-key)
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
  (if (not= (:id current) (:id prev))
    false
    ;timestamp instances for same timestamp fetched via ajax are not equal :(
    (not= (dissoc current :created-time) (dissoc prev :created-time))))

(defn start-application-review-autosave [db]
  (assoc-in
    db
    [:application :review-autosave]
    (autosave/interval-loop {:subscribe-path [:application :review]
                             :changed-predicate review-autosave-predicate
                             :handler (fn [current _]
                                        (ajax/http
                                          :put
                                          "/lomake-editori/api/applications/review"
                                          nil
                                          :override-args {:params (select-keys current [:id :notes :state])}))})))

(register-handler
  :application/fetch-application
  (fn [db [_ application-id]]
    (when-let [autosave (get-in db [:application :review-autosave])]
      (autosave/stop-autosave! autosave))
    (ajax/http
      :get
      (str "/lomake-editori/api/applications/" application-id)
      (fn [db application-response]
        (-> db
          (update-application-details application-response)
          (start-application-review-autosave))))
    (assoc db [:application :review-autosave] nil)))
