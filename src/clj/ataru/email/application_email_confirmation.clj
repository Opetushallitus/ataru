(ns ataru.email.application-email-confirmation
  "Application-specific email confirmation init logic"
  (:require
    [taoensso.timbre :as log]
    [selmer.parser :as selmer]
    [ataru.applications.application-store :as application-store]
    [ataru.email.email-store :as email-store]
    [ataru.background-job.job :as job]
    [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
    [ataru.background-job.email-job :as email-job]
    [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
    [ataru.config.core :refer [config]]
    [markdown.core :as md]
    [clojure.string :as string]
    [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
    [ataru.util :as util]
    [ataru.translations.texts :refer [email-default-texts]]
    [medley.core :refer [find-first]])
  (:import
    [org.owasp.html HtmlPolicyBuilder ElementPolicy]))

(def languages #{:fi :sv :en})
(def languages-map {:fi nil :sv nil :en nil})

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

(def add-style-to-links
  (proxy [ElementPolicy] []
    (apply [element-name attrs]
      (doto attrs
        (.add "target")
        (.add "_blank")
        (.add "style")
        (.add "color: #0093C4;"))
      element-name)))

(def html-policy
  (as-> (HtmlPolicyBuilder.) hpb
        (.allowElements hpb (->string-array "p" "span" "div" "h1" "h2" "h3" "h4" "h5" "ul" "ol" "li" "br"))
        (.allowElements hpb add-style-to-links (->string-array "a"))
        (.allowUrlProtocols hpb (->string-array "http" "https"))
        (.onElements (.allowAttributes hpb (->string-array "href" "target")) (->string-array "a"))
        (.toFactory hpb)))

(defn- submit-email-template-filename
  [lang]
  (str "templates/email_submit_confirmation_template_" (name lang) ".html"))

(defn- modify-link [secret]
  (-> config
      (get-in [:public-config :applicant :service_url])
      (str "/hakemus?modify=" secret)))

(defn- ->safe-html
  [content]
  (.sanitize html-policy (md/md-to-html-string content)))

(defn- hakukohde-names [tarjonta-service lang application]
  (when-let [haku-oid (:haku application)]
    (let [priority? (:usePriority (tarjonta-service/get-haku tarjonta-service haku-oid))]
      (->> (:hakukohde application)
           (map #(tarjonta-service/get-hakukohde-name tarjonta-service %))
           (map-indexed #(cond->> (some %2 [lang :fi :sv :en])
                                  priority? (str (inc %1) ". ")))))))

(defn- create-email [tarjonta-service subject template-name application-id]
  (let [application     (application-store/get-application application-id)
        form-key        (-> [(:key application)]
                            (application-store/get-applications-by-keys)
                            (first)
                            (:form-key))
        lang            (keyword (:lang application))
        subject         (subject lang)
        content         (-> (find-first #(= (:lang application) (:lang %)) (email-store/get-email-templates form-key))
                            :content
                            (->safe-html))
        recipient       (->> (:answers application)
                             (filter #(= "email" (:key %)))
                             first
                             :value)
        application-url (modify-link (:secret application))
        body            (selmer/render-file
                          (template-name lang)
                          {:hakukohteet     (hakukohde-names tarjonta-service
                                                             lang
                                                             application)
                           :application-url application-url
                           :application-oid (:key application)
                           :content         content})]
    {:from       from-address
     :recipients [recipient]
     :subject    subject
     :body       body}))

(defn- create-submit-email [tarjonta-service application-id]
  (create-email tarjonta-service
                submit-email-subjects
                submit-email-template-filename
                application-id))

(defn preview-submit-email
  [lang subject content content-ending]
  {:from           from-address
   :subject        subject
   :content        content
   :content-ending content-ending
   :lang           lang
   :body           (-> (submit-email-template-filename lang)
                       (selmer/render-file {:lang            lang
                                            :hakukohteet     ["Hakukohde 1" "Hakukohde 2" "Hakukohde 3"]
                                            :application-url "https://opintopolku.fi/hakemus/01234567890abcdefghijklmn"
                                            :application-oid "1.2.246.562.11.00000000000000000000"
                                            :content         (->safe-html content)
                                            :content-ending  (->safe-html content-ending)}))})

(defn preview-submit-emails [previews]
  (map
   #(let [lang           (:lang %)
          subject        (:subject %)
          content        (:content %)
          content-ending (:content-ending %)]
      (preview-submit-email lang subject content content-ending)) previews))


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

(defn- add-blank-templates [templates]
  (as-> templates x
        (util/group-by-first (comp keyword :lang) x)
        (merge languages-map x)
        (map (fn [el]
                 (let [lang     (first el)
                       template (second el)]
                   {:lang           (name lang)
                    :subject        (get template :subject (get-in email-default-texts [:email-submit-confirmation-template :submit-email-subjects lang]))
                    :content        (get template :content "")
                    :content-ending (get template :content_ending (get-in email-default-texts [:email-submit-confirmation-template :without-application-period lang]))}))
             x)))

(defn get-email-templates
  [form-key]
  (as-> (email-store/get-email-templates form-key) x
        (add-blank-templates x)
        (map #(preview-submit-email (:lang %) (:subject %) (:content %) (:content-ending %)) x)))

(defn store-email-templates
  [form-key session templates]
  (let [stored-templates (mapv #(email-store/create-or-update-email-template
                                  form-key
                                  (:lang %)
                                  (-> session :identity :oid)
                                  (:subject %)
                                  (:content %)
                                  (:content-ending %))
                           templates)]
    (map #(preview-submit-email (:lang %) (:subject %) (:content %) (:content_ending %)) stored-templates)))
