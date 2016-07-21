(ns ataru.virkailija.application.handlers
  (:require [ataru.virkailija.virkailija-ajax :as ajax]
            [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler register-sub]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]))

(register-handler
  :application/select-application
  (fn [db [_ application-key]]
    (assoc-in db [:application :selected] application-key)))

(register-handler
  :application/fetch-applications
  (fn [db [_ form-id]]
    (ajax/http
      :get
      (str "/lomake-editori/api/forms/applications/" form-id)
      (fn [db form-and-applications]
        (update db :application
                merge form-and-applications)))
    db))
