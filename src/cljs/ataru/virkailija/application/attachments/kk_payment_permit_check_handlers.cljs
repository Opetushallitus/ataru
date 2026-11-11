(ns ataru.virkailija.application.attachments.kk-payment-permit-check-handlers
  (:require [ataru.application-common.fx :refer [http]]
            [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.virkailija.temporal :as temporal]
            [cljs-time.core :as t]
            [cljs-time.format :as f]
            [clojure.string :as string]
            [re-frame.core :as re-frame]))

(def ^:private body-keys
  [:found-in-migri :permit-valid-until :payment-obligation-basis :payment-obligation-other-reason])

(defn permit-valid-until-range
  "Lupakortin aikaisin ja myöhäisin sallittu voimassaolopäivä yyyy-MM-dd-merkkijonoina."
  []
  (let [today     (t/today)
        formatter (f/formatter "yyyy-MM-dd")]
    [(f/unparse-local-date formatter (t/minus today (t/years payment-module/kk-payment-permit-valid-until-years-back)))
     (f/unparse-local-date formatter (t/plus today (t/years payment-module/kk-payment-permit-valid-until-years-ahead)))]))

(defn- permit-valid-until-in-range?
  [permit-valid-until]
  (let [[earliest latest] (permit-valid-until-range)]
    (or (nil? permit-valid-until)
        (<= (compare earliest permit-valid-until) 0 (compare latest permit-valid-until)))))

(defn- permit-valid-until-valid?
  "Tallennetun päivämäärän saa pitää, vaikka se olisi sittemmin jäänyt rajojen ulkopuolelle. Vain muutetun
   päivämäärän on oltava rajojen sisällä."
  [check saved]
  (or (permit-valid-until-in-range? (:permit-valid-until check))
      (= (:permit-valid-until check) (:permit-valid-until saved))))

(defn- check-path [application-key attachment-key]
  [:kk-payment-permit-check application-key (name attachment-key)])

; Normalisoidaan kuten palvelimella, jotta pelkkä välilyönti ei ole muutos
(defn- body [check]
  (-> (into {} (map (fn [k] [k (get check k)]) body-keys))
      (update :payment-obligation-other-reason
              #(when (= "obligated-other" (:payment-obligation-basis check))
                 (some-> % string/trim not-empty)))))

(defn- save-dispatch [application-key attachment-key timeout]
  {:id       [:kk-payment-permit-check/save application-key (name attachment-key)]
   :timeout  timeout
   :dispatch [:kk-payment-permit-check/save application-key attachment-key]})

; Tallentamattomat muutokset säilyvät, kun hakemus haetaan uudelleen
(defn set-checks [db application-key checks]
  (update-in db
             [:kk-payment-permit-check application-key]
             (fn [current]
               (let [stored (into {} (map (juxt :attachment-key identity) checks))]
                 (into {}
                       (for [attachment-key (distinct (concat (keys stored) (keys current)))
                             :let [{:keys [check saved] :as entry} (get current attachment-key)
                                   unsaved?                         (and entry
                                                                         (or (:saving? entry)
                                                                             (:pending? entry)
                                                                             (not= (body check) (body saved))))]
                             :when (or (contains? stored attachment-key) unsaved?)]
                         [attachment-key (assoc entry
                                                :check (if unsaved? check (get stored attachment-key))
                                                :saved (get stored attachment-key))]))))))

; save on :immediate, :debounced (esim. päivämäärää kirjoitettaessa) tai :none (tekstikenttä tallennetaan, kun
; siitä poistutaan)
(re-frame/reg-event-fx
  :kk-payment-permit-check/set-field
  (fn [{db :db} [_ application-key attachment-key field value save]]
    (cond-> {:db (assoc-in db (conj (check-path application-key attachment-key) :check field) value)}
            (not= :none save)
            (assoc :dispatch-debounced (save-dispatch application-key
                                                      attachment-key
                                                      (if (= :debounced save) 1000 0))))))

; Maksuvelvollisuuden muu syy tyhjennetään perusteen mukana, jotta se ei palaa, kun peruste valitaan uudelleen
(re-frame/reg-event-fx
  :kk-payment-permit-check/clear-field
  (fn [{db :db} [_ application-key attachment-key field]]
    {:db                 (update-in db
                                    (conj (check-path application-key attachment-key) :check)
                                    #(cond-> (assoc % field nil)
                                             (= :payment-obligation-basis field)
                                             (assoc :payment-obligation-other-reason nil)))
     :dispatch-debounced (save-dispatch application-key attachment-key 0)}))

(re-frame/reg-event-fx
  :kk-payment-permit-check/save-now
  (fn [_ [_ application-key attachment-key]]
    {:dispatch-debounced (save-dispatch application-key attachment-key 0)}))

; Sallittujen rajojen ulkopuolista voimassaolopäivää ei tallenneta, vaan näkymä näyttää sallitun välin. Mitään ei
; lähetetä, jos merkinnät eivät ole muuttuneet edellisen tallennuksen jälkeen, ja tallennus odottaa edellisen
; valmistumista, jotta pyynnöt eivät ohita toisiaan.
(re-frame/reg-event-fx
  :kk-payment-permit-check/save
  (fn [{db :db} [_ application-key attachment-key]]
    (let [path                          (check-path application-key attachment-key)
          {:keys [check saved saving?]} (get-in db path)
          to-save                       (cond-> (body check)
                                                (not (permit-valid-until-valid? check saved))
                                                (assoc :permit-valid-until (:permit-valid-until saved)))]
      (cond
        saving?
        {:db (assoc-in db (conj path :pending?) true)}

        (not= to-save (body saved))
        {:db                          (update-in db path assoc :saving? true :pending? false)
         :kk-payment-permit-check/put {:application-key application-key
                                       :attachment-key  attachment-key
                                       :body            to-save}}))))

(def ^:private saving-flash {:loading? true :message "Tietoja tallennetaan"})
(def ^:private saved-flash {:loading? false :message "Kaikki muutokset tallennettu"})
(def ^:private save-failed-flash {:loading? false :message "Virhe tallennettaessa." :error-type :server-error})

(defn- save-finished [db application-key attachment-key flash]
  (let [path (check-path application-key attachment-key)]
    {:db         (update-in db path assoc :saving? false :pending? false)
     :dispatch-n (cond-> [[:flasher flash]]
                         (get-in db (conj path :pending?))
                         (conj [:kk-payment-permit-check/save application-key attachment-key]))}))

(re-frame/reg-event-fx
  :kk-payment-permit-check/saved
  (fn [{db :db} [_ application-key attachment-key response]]
    (let [{:keys [check events]} (:body response)]
      (save-finished (cond-> (update-in db (check-path application-key attachment-key) #(-> %
                                                                                            (dissoc :error?)
                                                                                            (assoc :saved check)))
                             (= application-key (get-in db [:application :selected-key]))
                             (assoc-in [:application :events] (temporal/parse-times events)))
                     application-key
                     attachment-key
                     saved-flash))))

(re-frame/reg-event-fx
  :kk-payment-permit-check/save-failed
  (fn [{db :db} [_ application-key attachment-key]]
    (save-finished (assoc-in db (conj (check-path application-key attachment-key) :error?) true)
                   application-key
                   attachment-key
                   save-failed-flash)))

(re-frame/reg-fx
  :kk-payment-permit-check/put
  (fn [{:keys [application-key attachment-key body]}]
    (re-frame/dispatch [:flasher saving-flash])
    (http (aget js/config "virkailija-caller-id")
          {:method        :put
           :url           (str "/lomake-editori/api/applications/" application-key
                               "/kk-payment-permit-check/" (name attachment-key))
           :post-data     body
           :handler       [:kk-payment-permit-check/saved application-key attachment-key]
           :error-handler [:kk-payment-permit-check/save-failed application-key attachment-key]})))

(re-frame/reg-sub
  :kk-payment-permit-check/check
  (fn [db [_ application-key attachment-key]]
    (get-in db (conj (check-path application-key attachment-key) :check))))

; Vain tallennettu tarkistus näytetään perittynä. Virkailijan omat muutokset korvaavat perityn tarkistuksen, kun ne
; tallennetaan
(re-frame/reg-sub
  :kk-payment-permit-check/original-check
  (fn [db [_ application-key attachment-key]]
    (get-in db (conj (check-path application-key attachment-key) :saved :original-check))))

(re-frame/reg-sub
  :kk-payment-permit-check/save-error?
  (fn [db [_ application-key attachment-key]]
    (get-in db (conj (check-path application-key attachment-key) :error?) false)))

(re-frame/reg-sub
  :kk-payment-permit-check/permit-valid-until-valid?
  (fn [db [_ application-key attachment-key]]
    (let [{:keys [check saved]} (get-in db (check-path application-key attachment-key))]
      (permit-valid-until-valid? check saved))))
