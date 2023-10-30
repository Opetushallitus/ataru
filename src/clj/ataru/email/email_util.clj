(ns ataru.email.email-util
  (:require [ataru.translations.texts :refer [email-default-texts email-link-section-texts]]
            [clojure.string :as string]
            [ataru.config.core :refer [config]]
            [taoensso.timbre :as log]))

(def from-address "no-reply@opintopolku.fi")

(def subject-max-length 255)

(defn enrich-subject-with-application-key-and-limit-length [prefix application-key lang]
  (if application-key
    (let [label (get-in email-default-texts [:hakemusnumero (or lang :fi)])
          postfix (str "(" label ": " application-key ")")
          over-length (max 0
                           (- (count (string/join " " [prefix postfix])) subject-max-length))
          trimmed-prefix (subs prefix
                               0 (- (count prefix) over-length))]
      (string/join " " [trimmed-prefix postfix]))
    prefix))

(defn- filter-template-params
  [template-params guardian?]
  (cond-> template-params
    guardian? (dissoc :application-url :content-ending)))

(defn- make-email-for-applicant-or-guardian
  [email-data render-file-fn guardian?]
  (when (seq (:recipients email-data))
    (let [template-params (filter-template-params (:template-params email-data) guardian?)
          body (render-file-fn template-params)]
      {:from (:from email-data)
       :recipients (:recipients email-data)
       :subject (:subject email-data)
       :body body})))

(defn make-email-data
  [recipients subject template-params]
  {:from from-address
   :recipients recipients
   :subject subject
   :template-params template-params})

(defn render-emails-for-applicant-and-guardian
  [applicant-email-data guardian-email-data render-file-fn]
  (let [email (make-email-for-applicant-or-guardian applicant-email-data render-file-fn false)
        guardian-email (make-email-for-applicant-or-guardian guardian-email-data render-file-fn true)]
    (filter (comp not nil?) [email guardian-email])))

(defn- oma-opintopolku-link []
  (-> config
      (get-in [:public-config :applicant :service_url])
      (str "/oma-opintopolku/")))
(defn- modify-link [secret]
  (-> config
      (get-in [:public-config :applicant :service_url])
      (str "/hakemus?modify=" secret)))

(defn get-application-url-and-text [form application lang]
  (let [form-allows-ht? (boolean (get-in form [:properties :allow-hakeminen-tunnistautuneena]))
        strong-auth? (= (:tunnistautuminen application) "strong")]
    (log/info "get application url and text " (:properties form) ", " form-allows-ht? " - " strong-auth?)
    (if form-allows-ht?
      (if strong-auth?
        {:oma-opintopolku-link (oma-opintopolku-link)}
        {:application-url (modify-link (:secret application))
         :application-url-text (get-in email-link-section-texts [:none lang])})
      {:application-url (modify-link (:secret application))})))
