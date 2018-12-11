(ns ataru.email.application-email-confirmation
  "Application-specific email confirmation init logic"
  (:require [ataru.applications.application-store :as application-store]
            [ataru.background-job.email-job :as email-job]
            [ataru.background-job.job :as job]
            [ataru.config.core :refer [config]]
            [ataru.email.email-store :as email-store]
            [ataru.tarjonta-service.hakuaika :refer [millis->localized-date-time]]
            [ataru.forms.form-store :as forms]
            [ataru.tarjonta-service.hakukohde :as hakukohde]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.hakuaika :as tarjonta-hakuaika]
            [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
            [ataru.translations.texts :refer [email-default-texts]]
            [ataru.util :as util]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
            [clojure.string :as string]
            [markdown.core :as md]
            [medley.core :refer [find-first]]
            [selmer.parser :as selmer]
            [taoensso.timbre :as log])
  (:import [org.owasp.html HtmlPolicyBuilder ElementPolicy]))

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
                                            :attachments-without-answer [{:label "Liite 1"
                                                                          :deadline (get (millis->localized-date-time 1540295947420) (keyword lang))}
                                                                         {:label "Liite 2"
                                                                          :deadline ""}
                                                                         {:label "Liite 3"
                                                                          :deadline ""}]
                                            :application-url "https://opintopolku.fi/hakemus/01234567890abcdefghijklmn"
                                            :application-oid "1.2.246.562.11.00000000000000000000"
                                            :content         (->safe-html content)
                                            :content-ending  (->safe-html content-ending)}))})

(defn get-email-templates
  [form-key]
  (as-> (email-store/get-email-templates form-key) x
        (add-blank-templates x)
        (map #(preview-submit-email (:lang %) (:subject %) (:content %) (:content-ending %)) x)))

(defn- attachment-with-deadline [application lang field]
  (let [attachment {:label (-> field :label lang)}]
    (assoc attachment :deadline (-> field :params :deadline-label lang))))

(defn- create-email [koodisto-cache tarjonta-service organization-service ohjausparametrit-service subject template-name application-id]
  (let [application                     (application-store/get-application application-id)
        tarjonta-info                   (tarjonta-parser/parse-tarjonta-info-by-haku
                                          koodisto-cache
                                          tarjonta-service
                                          organization-service
                                          ohjausparametrit-service
                                          (:haku application)
                                          (:hakukohde application))
        answers-by-key                  (-> application :answers util/answers-by-key)
        form                            (-> (forms/fetch-by-id (:form application))
                                            (hakukohde/populate-attachment-deadlines tarjonta-info))
        lang                            (keyword (:lang application))
        attachment-keys-without-answers (->> (application-store/get-application-attachment-reviews (:key application))
                                             (map :attachment-key)
                                             (filter #(not (contains? answers-by-key (keyword %))))
                                             set)
        attachments-without-answer      (->> form
                                             :content
                                             util/flatten-form-fields
                                             (filter #(contains? attachment-keys-without-answers (:id %)))
                                             (map #(attachment-with-deadline application lang %)))
        email-template                  (find-first #(= (:lang application) (:lang %)) (get-email-templates (:key form)))
        content                         (-> email-template
                                            :content
                                            (->safe-html))
        content-ending                  (-> email-template
                                            :content-ending
                                            (->safe-html))
        recipient                       (->> (:answers application)
                                             (filter #(= "email" (:key %)))
                                             first
                                             :value)
        subject                         (if subject (subject lang) (email-template :subject))
        application-url                 (modify-link (:secret application))
        body                            (selmer/render-file
                                          (template-name lang)
                                          {:hakukohteet                (hakukohde-names tarjonta-service
                                                                         lang
                                                                         application)
                                           :application-url            application-url
                                           :application-oid            (:key application)
                                           :content                    content
                                           :content-ending             content-ending
                                           :attachments-without-answer attachments-without-answer})]
    {:from       from-address
     :recipients [recipient]
     :subject    subject
     :body       body}))

(defn- create-submit-email [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application-id]
  (create-email koodisto-cache tarjonta-service
                organization-service
                ohjausparametrit-service
                nil
                submit-email-template-filename
                application-id))

(defn preview-submit-emails [previews]
  (map
   #(let [lang           (:lang %)
          subject        (:subject %)
          content        (:content %)
          content-ending (:content-ending %)]
      (preview-submit-email lang subject content content-ending)) previews))


(defn- create-edit-email [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application-id]
  (create-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                edit-email-subjects
                #(str "templates/email_edit_confirmation_template_"
                      (name %)
                      ".html")
                application-id))

(defn- create-refresh-secret-email
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application-id]
  (create-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                refresh-secret-email-subjects
                #(str "templates/email_refresh_secret_template_" (name %) ".html")
                application-id))

(defn start-email-job [job-runner email]
  (let [job-id (job/start-job job-runner
                              (:type email-job/job-definition)
                              email)]
    (log/info "Started application confirmation email job (to viestintäpalvelu) with job id" job-id ":")
    (log/info email)))

(defn start-email-submit-confirmation-job
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (start-email-job job-runner (create-submit-email koodisto-cache tarjonta-service
                                                   organization-service
                                                   ohjausparametrit-service
                                                   application-id)))

(defn start-email-edit-confirmation-job
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (start-email-job job-runner (create-edit-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                                                 application-id)))

(defn start-email-refresh-secret-confirmation-job
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (start-email-job job-runner (create-refresh-secret-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                                                           application-id)))

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
