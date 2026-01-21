(ns ataru.kk-application-payment.utils
  (:require [ataru.component-data.kk-application-payment-module :refer [kk-application-payment-module
                                                                        kk-application-payment-wrapper-key]]
            [ataru.component-data.person-info-module :refer [person-info-module]]
            [ataru.config.core :refer [config]]
            [ataru.email.email-util :as email-util]
            [ataru.translations.translation-util :as translations]
            [ataru.util :as util]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [selmer.parser :as selmer]
            [taoensso.timbre :as log]
            [clojure.string :as s]))

(def payment-config
  (get config :kk-application-payments))

(def first-application-payment-hakuaika-start
  "Application payments are only charged from admissions starting on certain day"
  (time/from-time-zone (time/date-time
                         (get payment-config :start-year 2025)
                         (get payment-config :start-month 1)
                         (get payment-config :start-day 1))
                       (time/time-zone-for-id "Europe/Helsinki")))

(def haku-update-grace-days
  "Number of days payment statuses related to haku should still be checked and updated after hakuaika has ended"
  180)

(defn alkamiskausi-ja-vuosi
  [alkamiskausi alkamisvuosi lang]
  (let [translations (translations/get-translations lang)]
    (case (-> alkamiskausi (s/split #"#") first)
      "kausi_k" (str (:alkamiskausi-kevat translations) " " alkamisvuosi)
      "kausi_s" (str (:alkamiskausi-syksy translations) " " alkamisvuosi))))

(defn payment-email [lang email data {:keys [template-path subject-key subject-suffix]}]
  (let [template-path    template-path
        translations     (translations/get-translations lang)
        emails           (list email)
        subject-prefix   (if subject-suffix
                           (str (subject-key translations) " " subject-suffix)
                           (subject-key translations))
        subject          (email-util/enrich-subject-with-application-key-and-limit-length subject-prefix (:application-key data) lang)
        body             (selmer/render-file template-path
                                             (merge data translations))]
    (when (not-empty emails)
      {:from       "no-reply@opintopolku.fi"
       :recipients emails
       :subject    subject
       :body       body
       :masks      (if-let [url (:payment-url data)]
                     [{:secret url
                       :mask   "https://maksulinkki-piilotettu.opintopolku.fi/"}]
                     [])
       :metadata   (email-util/->metadata (:application-key data) (:person-oid data))
       :privileges (email-util/->hakemus-privileges (:organization-oids data))})))

(defn get-application-language
  [application]
  (-> application (get :lang "fi") keyword))

(defn get-application-email
  [application]
  (->> (get application :answers)
       (filter #(= (:key %) "email"))
       first
       :value))

(defn time-is-before-some-hakuaika-grace-period?
  "Returns true if time 'now' is before specified grace days for one or more hakuaikas, for given haku"
  [haku grace-days now]
  (log/info (str "time-is-before-some-hakuaika-grace-period? Haku: " haku))
  (log/info (str "time-is-before-some-hakuaika-grace-period? grace-days: " grace-days))
  (log/info (str "time-is-before-some-hakuaika-grace-period? now: " now))
  (let [hakuajat-end-with-nils                   (if-let [hakuajat (:hakuajat haku)]
                                                  (map :end hakuajat)
                                                  [(coerce/from-long (get-in haku [:hakuaika :end]))])
        hakuajat-end                             (filter some? hakuajat-end-with-nils)
        end-times-with-grace-period             (map
                                                  #(time/plus % (time/days grace-days))
                                                  hakuajat-end)]
    (if (empty? end-times-with-grace-period)
      (do (log/warn (str "Kk-haku (" (:oid haku) ") has no hakuaikas with end period: Haku: " haku))
          false)
      (boolean
        (some #(not (time/before? % now)) end-times-with-grace-period)))))

(defn haku-active-for-updating
  "Check whether valid haku is recent enough that payments related to its applications may still need updating.
   Returns all hakus that have their last application end date max grace days before today."
  [haku]
  (time-is-before-some-hakuaika-grace-period?
    haku (+ haku-update-grace-days 1) (time/now)))

(defn has-payment-module? [form]
  (->> (:content form)
       (map :id)
       (some #(= kk-application-payment-wrapper-key %))
       boolean))

(defn inject-payment-module-to-form [form]
  (let [sections (:content form)
        update-fn (fn[section]
                    (if (= :person-info (keyword (:module section)))
                      (person-info-module :onr-kk-application-payment)
                      section))
        payment-section (kk-application-payment-module)
        index-of-person-info-module (util/first-index-of #(= :person-info (keyword %)) (map :module sections))
        index-to-insert (if (<= 0 index-of-person-info-module)
                          (+ index-of-person-info-module 1)
                          2)
        updated-content (map update-fn sections)
        ; lisätään maksumoduuli hakukohde ja henkilötieto-osioiden jälkeen:
        updated-content (concat (take index-to-insert updated-content) [payment-section] (drop index-to-insert updated-content))]
    (assoc form :content updated-content)))

(defn update-payment-module-in-form
  [form update-payment update-person-info]
  (let [sections (:content form)
        payment-section (kk-application-payment-module)
        update-fn (fn[section]
                    (cond
                      (and update-payment (= (:id section) kk-application-payment-wrapper-key))
                      payment-section

                      (and update-person-info (= :person-info (keyword (:module section))))
                      (person-info-module :onr-kk-application-payment)

                      :else
                      section))
        updated-content (map update-fn sections)]
    (assoc form :content updated-content)))
