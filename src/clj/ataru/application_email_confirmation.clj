(ns ataru.application-email-confirmation
  "Application-specific email confirmation init logic"
  (:require
    [taoensso.timbre :as log]
    [selmer.parser :as selmer]
    [ataru.applications.application-store :as application-store]
    [ataru.background-job.job :as job]
    [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
    [ataru.background-job.email-job :as email-job]
    [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
    [ataru.config.core :refer [config]]
    [markdown.core :as md]
    [clojure.string :as string])
  (:import
    [org.owasp.html HtmlPolicyBuilder]))

(def submit-email-subjects
  {:fi "Opintopolku: hakemuksesi on vastaanotettu"
   :sv "Studieinfo: Din ansökan har mottagits"
   :en "Studyinfo: Your application has been received"})

(def edit-email-subjects
  {:fi "Opintopolku: Muutokset hakemukseesi on tallennettu"
   :sv "Studieinfo: Dina ändringar har lagrats i din ansökan"
   :en "Studyinfo: The changes to your application have been saved"})

(def refresh-secret-email-subjects
  {:fi "Opintopolku: uusi hakemuslinkkisi"
   :sv "Studieinfo: ny länk till din ansökan"
   :en "Studyinfo: your new application link"})

(def from-address "no-reply@opintopolku.fi")

(defn- ->string-array
  [& elements]
  (into-array String elements))

(def html-policy
  (as-> (HtmlPolicyBuilder.) hpb
        (.allowElements hpb (->string-array "a" "p" "div" "h1" "h2" "h3" "h4" "h5" "ul" "ol" "li"))
        (.allowUrlProtocols hpb (->string-array "http" "https"))
        (.onElements (.allowAttributes hpb (->string-array "href" "target")) (->string-array "a"))
        (.toFactory hpb)))

(defn- add-link-target-prop
  [text state]
  [(string/replace text #"<a href=([^>]+)>" "<a target=\"_blank\" href=$1>") state])

(defn- submit-email-template-filename
  [lang]
  (str "templates/email_submit_confirmation_template_" (name lang) ".html"))

(defn- modify-link [secret]
  (-> config
      (get-in [:public-config :applicant :service_url])
      (str "/hakemus?modify=" secret)))

(defn- hakukohde-names [tarjonta-service lang application]
  (when-let [haku-oid (:haku application)]
    (let [priority? (:usePriority (tarjonta-service/get-haku tarjonta-service haku-oid))]
      (->> (:hakukohde application)
           (map #(tarjonta-service/get-hakukohde-name tarjonta-service %))
           (map-indexed #(cond->> (some %2 [lang :fi :sv :en])
                                  priority? (str (inc %1) ". ")))))))

(defn- create-email [tarjonta-service subject template-name application-id]
  (let [application     (application-store/get-application application-id)
        lang            (keyword (:lang application))
        subject         (subject lang)
        recipient       (->> (:answers application)
                             (filter #(= "email" (:key %)))
                             first
                             :value)
        application-url (modify-link (:secret application))
        body            (selmer/render-file
                          (template-name lang)
                          {:hakukohteet (hakukohde-names tarjonta-service
                                                         lang
                                                         application)
                           :application-url application-url
                           :application-oid (:key application)})]
    {:from       from-address
     :recipients [recipient]
     :subject    subject
     :body       body}))

(defn- create-submit-email [tarjonta-service application-id]
  (create-email tarjonta-service
                submit-email-subjects
                submit-email-template-filename
                application-id))

(defn- sanitize-content
  [content]
  (.sanitize
    html-policy
    (md/md-to-html-string content :custom-transformers [add-link-target-prop])))

(defn preview-submit-email
  [lang content]
  {:from    from-address
   :subject (lang submit-email-subjects)
   :body    (selmer/render-file
              (submit-email-template-filename lang)
              {:lang            lang
               :hakukohteet     ["Hakukohde 1" "Hakukohde 2" "Hakukohde 3"]
               :application-url "https://example.com/muokkaus-linkki-esimerkki"
               :application-oid "1.2.246.562.11.00000000000000000000"
               :content         (sanitize-content content)})})

(defn- create-edit-email [tarjonta-service application-id]
  (create-email tarjonta-service
                edit-email-subjects
                #(str "templates/email_edit_confirmation_template_"
                      (name %)
                      ".html")
                application-id))

(defn- create-refresh-secret-email
  [tarjonta-service application-id]
  (create-email tarjonta-service
                refresh-secret-email-subjects
                #(str "templates/email_refresh_secret_template_" (name %) ".html")
                application-id))

(defn start-email-job [email]
                      (let [job-type (:type email-job/job-definition)
                            job-id   (job/start-job
                                       hakija-jobs/job-definitions
                                       job-type
                                       email)]
                        (log/info "Started application confirmation email job (to viestintäpalvelu) with job id" job-id ":")
                        (log/info email)))

(defn start-email-submit-confirmation-job [tarjonta-service application-id]
                                          (start-email-job (create-submit-email tarjonta-service application-id)))

(defn start-email-edit-confirmation-job [tarjonta-service application-id]
                                        (start-email-job (create-edit-email tarjonta-service application-id)))

(defn start-email-refresh-secret-confirmation-job
  [tarjonta-service application-id]
  (start-email-job (create-refresh-secret-email tarjonta-service application-id)))
