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

(re-frame/reg-sub
  :application/application-valinnat-loading-state
  (fn [db _]
    (let [application-key (-> db :application :selected-key)
          valinnat   (get-in db [:application :valinnat-by-application-key application-key])
          loading         (get-in db [:request-handles :fetch-application-valinnat])]
      (if (:error valinnat)
        :error
        (if loading
          :loading
          (if (= {} valinnat)
            :not-found
            :loaded))))))

(re-frame/reg-sub
  :application/application-valinnat
  (fn [db _]
    (let [application-key (-> db :application :selected-key)]
      (get-in db [:application :valinnat-by-application-key application-key]))))

(re-frame/reg-sub
  :application/harkinnanvaraisuus-loading-state
  (fn [db _]
    (let [application-key (-> db :application :selected-key)
          harkinnanvaraisuus   (get-in db [:application :harkinnanvarainen-pohjakoulutus-by-application-key application-key])
          loading (get-in db [:request-handles :fetch-applicant-harkinnanvaraisuus])]
      (cond
        (:error harkinnanvaraisuus)
        :error

        loading
        :loading

        :else
        :loaded))))

(re-frame/reg-sub
  :application/harkinnanvarainen-pohjakoulutus?
  (fn [db _]
    (let [application-key (-> db :application :selected-key)]
      (get-in db [:application :harkinnanvarainen-pohjakoulutus-by-application-key application-key]))))

(re-frame/reg-sub
  :application/harkinnanvarainen-application-but-not-according-to-koski?
  (fn [db _]
    (let [application-key (-> db :application :selected-key)]
      (get-in db [:application :harkinnanvarainen-application-but-not-according-to-koski? application-key]))))

(re-frame/reg-sub
  :application/yksilollistetty-matikka-aikka?
  (fn [db _]
    (let [application-key (-> db :application :selected-key)]
      (get-in db [:application :yksilollistetty-matikka-aikka-by-application-key application-key]))))

(def grade-order ["PK_AI" "PK_A1" "PK_A12" "PK_A2" "PK_A22"
                 "PK_B1" "PK_B2" "PK_B22" "PK_B23"
                 "PK_MA" "PK_BI" "PK_GE" "PK_FY" "PK_KE"
                 "PK_TE" "PK_KT" "PK_HI" "PK_YH" "PK_MU"
                 "PK_KU" "PK_KS" "PK_LI" "PK_KO"])

(re-frame/reg-sub
  :application/grades
  (fn [_]
    [(re-frame/subscribe [:application/pohjakoulutus-for-valinnat])])
  (fn [[pohjakoulutus]]
    (let [grades (:arvosanat pohjakoulutus)
          grade-comporator (fn [grade-a grade-b]
                             (< (.indexOf grade-order (:key grade-a))
                                (.indexOf grade-order (:key grade-b))))]
      (sort grade-comporator grades))))
