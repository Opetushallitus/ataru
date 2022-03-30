(ns ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :application/pohjakoulutus-for-valinnat-loading-state
  (fn [db _]
    (let [application-key (-> db :application :selected-key)
          pohjakoulutus   (get-in db [:application :pohjakoulutus-by-application-key application-key])
          loading         (get-in db [:request-handles :fetch-applicant-pohjakoulutus])]
      (if (:error pohjakoulutus)
        :error
        (if loading
          :loading
          (if (= {} pohjakoulutus)
            :not-found
            :loaded))))))

(re-frame/reg-sub
  :application/pohjakoulutus-for-valinnat
  (fn [db _]
    (let [application-key (-> db :application :selected-key)]
      (get-in db [:application :pohjakoulutus-by-application-key application-key]))))
