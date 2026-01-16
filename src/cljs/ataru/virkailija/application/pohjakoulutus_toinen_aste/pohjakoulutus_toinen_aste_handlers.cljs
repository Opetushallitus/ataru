(ns ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-handlers
  (:require [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-yksilollistetty-matikka-aikka-types
                                                                                   pohjakoulutus-harkinnanvarainen-types]]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util :as hutil]
            [ataru.tarjonta.haku :as haku]
            [ataru.virkailija.application.application-selectors :refer [selected-application-answers]]
            [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
  :application/fetch-applicant-pohjakoulutus
  (fn [_ [_ application-key]]
    {:http {:method              :get
            :path                (str "/lomake-editori/api/suorituspalvelu/hakemus/" application-key "/avainarvot")
            :handler-or-dispatch :application/handle-fetch-applicant-pohjakoulutus-response
            :handler-args        application-key
            :override-args       {:error-handler #(re-frame/dispatch [:application/handle-fetch-applicant-pohjakoulutus-error application-key])}
            :id                  :fetch-applicant-pohjakoulutus}}))

(re-frame/reg-event-db
  :application/handle-fetch-applicant-pohjakoulutus-response
  (fn [db [_ response application-key]]
    (-> db
      (assoc-in [:application :pohjakoulutus-by-application-key application-key] response))))

(re-frame/reg-event-db
  :application/handle-fetch-applicant-pohjakoulutus-error
  (fn [db [_ application-key]]
    (-> db
      (assoc-in [:application :pohjakoulutus-by-application-key application-key :error] true))))

(re-frame/reg-event-fx
  :application/fetch-application-valinnat
  (fn [_ [_ haku-oid application-key]]
    {:http {:method               :get
            :path                 (str "/lomake-editori/api/tulos-service/haku/" haku-oid "/hakemus/" application-key)
            :handler-or-dispatch  :application/handle-fetch-application-valinnat-response
            :handler-args         application-key
            :override-args       {:error-handler #(re-frame/dispatch [:application/handle-fetch-application-valinnat-error application-key %])}
            :id                   :fetch-applicant-valinnat}}))

(re-frame/reg-event-db
  :application/handle-fetch-application-valinnat-response
  (fn [db [_ response application-key]]
    (-> db
        (assoc-in [:application :valinnat-by-application-key application-key] response))))

(re-frame/reg-event-db
  :application/handle-fetch-application-valinnat-error
  (fn [db [_ application-key response]]
    (let [error (-> response
                    :response
                    :error
                    (case
                      "Valinnan tulokset kesken" :valinnan-tulokset-kesken
                      true))]
      (-> db
          (assoc-in [:application :valinnat-by-application-key application-key :error] error)))))

(re-frame/reg-event-fx
  :application/fetch-applicant-harkinnanvaraisuus
  (fn [_ [_ application-key]]
    {:http {:method              :get
            :path                (str "/lomake-editori/api/valintalaskentakoostepalvelu/harkinnanvaraisuus/hakemus/" application-key)
            :handler-or-dispatch :application/handle-fetch-applicant-harkinnanvaraisuus-response
            :handler-args        application-key
            :override-args       {:error-handler #(re-frame/dispatch [:application/handle-fetch-applicant-harkinnanvaraisuus-error application-key])}
            :id                  :fetch-applicant-harkinnanvaraisuus}}))

(re-frame/reg-event-db
  :application/handle-fetch-applicant-harkinnanvaraisuus-response
  (fn [db [_ response application-key]]
    (let [answers (selected-application-answers db)
          has-harkinnanvaraisuus-reason-in-group (fn [resp group]
                                                   (->> resp
                                                        (map :harkinnanvaraisuudenSyy)
                                                        (some (fn [harkinnanvaraisuus]
                                                                (some #{harkinnanvaraisuus} group)) )
                                                        boolean))
          yksilollistetty-matikka-aikka? (has-harkinnanvaraisuus-reason-in-group
                                           response harkinnanvaraisuus-yksilollistetty-matikka-aikka-types)
          harkinnanvarainen-pohjakoulutus? (has-harkinnanvaraisuus-reason-in-group
                                             response pohjakoulutus-harkinnanvarainen-types)
          pick-value-fn (fn [answers question]
                          (:value (question answers)))
          harkinnanvarainen-application-but-not-according-to-koski? (and (not harkinnanvarainen-pohjakoulutus?)
                                                                         (hutil/get-common-harkinnanvaraisuus-reason answers pick-value-fn))]
    (-> db
        (assoc-in [:application :harkinnanvarainen-pohjakoulutus-by-application-key application-key]
                  harkinnanvarainen-pohjakoulutus?)
        (assoc-in [:application :yksilollistetty-matikka-aikka-by-application-key application-key]
                  yksilollistetty-matikka-aikka?)
        (assoc-in [:application :harkinnanvarainen-application-but-not-according-to-koski? application-key]
                  harkinnanvarainen-application-but-not-according-to-koski?)))))

(re-frame/reg-event-db
  :application/handle-fetch-applicant-harkinnanvaraisuus-error
  (fn [db [_ application-key]]
    (-> db
        (assoc-in [:application :harkinnanvarainen-pohjakoulutus-by-application-key application-key :error] true))))

(defn create-fetch-applicant-pohjakoulutus-event-if-toisen-asteen-yhteishaku
  [application]
  (when (haku/toisen-asteen-yhteishaku? (:tarjonta application))
    [[:application/fetch-applicant-pohjakoulutus (:key application)]
     [:application/fetch-applicant-harkinnanvaraisuus (:key application)]
     [:application/fetch-application-valinnat (:haku application) (:key application)]]))
