(ns ataru.email.application-email
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.field-deadline :as field-deadline]
            [ataru.config.core :refer [config]]
            [ataru.email.email-store :as email-store]
            [ataru.email.email-util :as email-util]
            [ataru.forms.form-store :as forms]
            [ataru.tarjonta-service.hakukohde :as hakukohde]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.translations.texts :refer [email-default-texts tutu-decision-email]]
            [ataru.util :as util]
            [ataru.date :as date]
            [clj-time.core :as t]
            [clojure.set]
            [markdown.core :as md]
            [medley.core :refer [find-first]]
            [selmer.parser :as selmer]
            [ataru.hakukohde.liitteet :as liitteet]
            [clojure.string :as string]
            [ataru.koodisto.koodisto :as koodisto])
  (:import [org.owasp.html HtmlPolicyBuilder ElementPolicy]))

(def languages #{:fi :sv :en})
(def languages-map {:fi nil :sv nil :en nil})

(def edit-email-subjects
  {:fi "Opintopolku: Muutokset hakemukseesi on tallennettu"
   :sv "Studieinfo: Dina ändringar har lagrats i din ansökan"
   :en "Studyinfo: The changes to your application have been saved"})

(def refresh-secret-email-subjects
  {:fi "Opintopolku: uusi hakemuslinkkisi"
   :sv "Studieinfo: ny länk till din ansökan"
   :en "Studyinfo: your new application link"})

(def application-number-prefix
  {:fi "Hakemusnumero"
   :sv "Ansökningsnummer"
   :en "Application number"})

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
        (.allowElements hpb (->string-array "p" "span" "div" "h1" "h2" "h3" "h4" "h5" "ul" "ol" "li" "br" "strong" "em"))
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

(defn- escape-full-urls
  [content]
  (let [full-urls (re-seq #"\w+:\/\/\S*" content)]
    (reduce
      #(string/replace-first %1 %2 (string/escape %2 {\_ "\\_"}))
      content
      full-urls)))

(defn- markdown->html
  [content]
  (-> content
      (escape-full-urls)
      (md/md-to-html-string)))

(defn ->safe-html
  [content]
  (when content
    (.sanitize html-policy (markdown->html content))))

(defn- hakukohde-names [tarjonta-info lang application]
  (when (:haku application)
    (let [tarjonta-hakukohteet (util/group-by-first :oid (:hakukohteet tarjonta-info))
          hakukohteet          (keep #(get tarjonta-hakukohteet %) (:hakukohde application))]
      (when-let [missing-oids (seq (clojure.set/difference
                                     (set (:hakukohde application))
                                     (set (map :oid hakukohteet))))]
        (throw (new RuntimeException
                    (str "Hakukohteet " (string/join ", " missing-oids)
                         " not found"))))
      (map-indexed (fn [i {:keys [name tarjoaja-name]}]
                     (str (when (:prioritize-hakukohteet tarjonta-info)
                            (str (inc i) ". "))
                          (util/non-blank-val name [lang :fi :sv :en])
                          " - "
                          (util/non-blank-val tarjoaja-name [lang :fi :sv :en])))
                   hakukohteet))))

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
                  :content-ending (get template :content_ending (get-in email-default-texts [:email-submit-confirmation-template :without-application-period lang]))
                  :signature      (get template :signature (get-in email-default-texts [:email-submit-confirmation-template :signature lang]))}))
             x)))


(defn preview-submit-email
  [lang subject content content-ending signature]
  {:from           email-util/from-address
   :subject        subject
   :content        content
   :content-ending content-ending
   :lang           lang
   :signature      signature
   :body           (-> (submit-email-template-filename lang)
                       (selmer/render-file {:lang            lang
                                            :hakukohteet     ["Hakukohde 1" "Hakukohde 2" "Hakukohde 3"]
                                            :attachments-without-answer [{:label "Liite 1"
                                                                          :deadline (get (hakuaika/date-time->localized-date-time (t/date-time 2000 12 31)) (keyword lang))}
                                                                         {:label "Liite 2"
                                                                          :deadline ""}
                                                                         {:label "Liite 3"
                                                                          :deadline ""}]
                                            :application-url "https://opintopolku.fi/hakemus/01234567890abcdefghijklmn"
                                            :application-oid "1.2.246.562.11.00000000000000000000"
                                            :content         (->safe-html content)
                                            :content-ending  (->safe-html content-ending)
                                            :signature       (->safe-html signature)}))})

(defn get-email-templates
  [form-key]
  (as-> (email-store/get-email-templates form-key) x
        (add-blank-templates x)
        (map #(preview-submit-email (:lang %) (:subject %) (:content %) (:content-ending %) (:signature %)) x)))

(defn- attachment-with-deadline [_ lang field]
  (let [attachment {:label (-> field
                               :label
                               (util/from-multi-lang lang))}]
    (assoc attachment :deadline (-> field :params :deadline-label lang))))

(defn- attachment-with-info-from-kouta
  [get-attachment-type lang field hakukohde-oid hakukohteet]
  (let [hakukohde            (first (filter #(= (:oid %) hakukohde-oid) hakukohteet))
        attachment-type      (get-in field [:params :attachment-type])
        attachment-type-text (util/from-multi-lang (get-attachment-type attachment-type) lang)
        attachment           (liitteet/attachment-for-hakukohde attachment-type hakukohde)
        address              (->safe-html (liitteet/attachment-address-with-hakukohde lang attachment hakukohde))
        default-deadline     (-> field :params :deadline-label (get lang))
        deadline             (or (liitteet/attachment-deadline lang attachment hakukohde) default-deadline)
        info-text            (-> field :params :info-text :value (get lang))]
    {:attachment-type attachment-type-text :label address :deadline deadline :info-text info-text}))

(defn- create-attachment-info-utilizing-kouta
  [get-attachment-type answers-by-key flat-form-fields lang hakukohteet]
  (let [find-original-field (fn [original-field-oid] (first (filter #(= (:id %) original-field-oid) flat-form-fields)))]
    (->> answers-by-key
         (keep (fn [[_ val]]
                 (let [original-question (or (:original-question val) (:original-followup val))
                       hakukohde-oid     (or (:duplikoitu-kysymys-hakukohde-oid val) (:duplikoitu-followup-hakukohde-oid val))]
                   (when (and (= "attachment" (:fieldType val)) original-question)
                     {:original-field original-question :hakukohde-oid hakukohde-oid}))))
         (filter #(get-in (find-original-field (:original-field %)) [:params :fetch-info-from-kouta?]))
         (map #(attachment-with-info-from-kouta get-attachment-type lang (find-original-field (:original-field %)) (:hakukohde-oid %) hakukohteet))
         (group-by :attachment-type))))

(defn- get-application
  [application-id]
  (application-store/get-application application-id))

(defn- get-tarjonta-info
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application]
    (:tarjonta
      (tarjonta-parser/parse-tarjonta-info-by-haku
        koodisto-cache
        tarjonta-service
        organization-service
        ohjausparametrit-service
        (:haku application)
        (:hakukohde application))))

(defn- get-attachment-type-fn
  [koodisto-cache]
  (fn [attachment-type]
    (koodisto/get-attachment-type-label koodisto-cache attachment-type)))

(defn- enrich-subject-with-application-key [prefix application-key lang]
  (if application-key
    (let [postfix (str "(" (get-in email-default-texts [:hakemusnumero (or lang :fi)]) ": " application-key ")")]
      (string/join " " [prefix postfix]))
    prefix))

(defn create-emails
  [subject template-name application tarjonta-info raw-form application-attachment-reviews email-template get-attachment-type guardian? payment-url]
   (let [now                             (t/now)
         answers-by-key                  (-> application :answers util/answers-by-key)
         hakukohteet                     (:hakukohteet tarjonta-info)
         hakuajat                        (hakuaika/index-hakuajat hakukohteet)
         field-deadlines                 (->> (:key application)
                                              field-deadline/get-field-deadlines
                                              (map #(dissoc % :last-modified))
                                              (util/group-by-first :field-id))
         form                            (hakukohde/populate-attachment-deadlines raw-form now hakuajat field-deadlines)
         flat-form-fields                (util/flatten-form-fields (:content form))
         lang                            (keyword (:lang application))
         attachment-keys-without-answers (->> application-attachment-reviews
                                              (map :attachment-key)
                                              (filter #(or (not (contains? answers-by-key (keyword %)))
                                                           (or (empty? (:value ((keyword %) answers-by-key)))
                                                               (and (coll? (:value ((keyword %) answers-by-key)))
                                                                    (boolean (not-empty (filter empty? (:value ((keyword %) answers-by-key)))))))))
                                              set)
         attachments-without-answer      (->> flat-form-fields
                                              (filter #(and (contains? attachment-keys-without-answers (:id %))
                                                            (not (:per-hakukohde %))))
                                              (map #(attachment-with-deadline application lang %)))
         attachments-info-from-kouta     (create-attachment-info-utilizing-kouta get-attachment-type answers-by-key flat-form-fields lang hakukohteet)
         content                         (-> email-template
                                             :content
                                             (->safe-html))
         content-ending                  (-> email-template
                                             :content-ending
                                             (->safe-html))
         signature                       (-> email-template
                                             :signature
                                             (->safe-html))
         minor?                          (date/minor? (get-in answers-by-key [:birth-date :value]))
         applier-recipients              (->> (:answers application)
                                              (filter #(= "email" (:key %)))
                                              (filter #(not (string/blank? (:value %))))
                                              (map :value))
         guardian-recipients             (when (and minor? guardian?)
                                           (->> (:answers application)
                                                (filter (fn [answer]
                                                          (#{"guardian-email" "guardian-email-secondary"} (:key answer))))
                                                (mapcat :value)
                                                (filter (comp not clojure.string/blank?))))
         subject-prefix                  (if subject (subject lang) (email-template :subject))
         subject                         (enrich-subject-with-application-key subject-prefix (:key application) lang)
         application-url                 (modify-link (:secret application))
         template-params                 {:hakukohteet                (hakukohde-names tarjonta-info lang application)
                                          :application-oid            (:key application)
                                          :application-url            application-url
                                          :payment-url                payment-url
                                          :content                    content
                                          :content-ending             content-ending
                                          :attachments-without-answer attachments-without-answer
                                          :kouta-attachments-by-type  attachments-info-from-kouta
                                          :signature                  signature}
         applicant-email-data            (email-util/make-email-data applier-recipients subject template-params)
         guardian-email-data             (email-util/make-email-data guardian-recipients subject template-params)
         render-file-fn                  (fn [template-params]
                                           (selmer/render-file (template-name lang) template-params))]
     (email-util/render-emails-for-applicant-and-guardian
       applicant-email-data
       guardian-email-data
       render-file-fn)))

(defn- create-emails-by-gathering-data
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service subject template-name application-id guardian? payment-url]
  (let [application                     (get-application application-id)
        tarjonta-info                   (get-tarjonta-info koodisto-cache tarjonta-service organization-service ohjausparametrit-service application)
        raw-form                        (forms/fetch-by-id (:form application))
        application-attachment-reviews  (application-store/get-application-attachment-reviews (:key application))
        email-template                  (find-first #(= (:lang application) (:lang %)) (get-email-templates (:key raw-form)))
        get-attachment-type             (get-attachment-type-fn koodisto-cache)]
    (create-emails subject template-name application tarjonta-info raw-form application-attachment-reviews email-template get-attachment-type guardian? payment-url)))

(defn create-submit-email [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application-id guardian? payment-url]
  (create-emails-by-gathering-data koodisto-cache
                 tarjonta-service organization-service ohjausparametrit-service
                 nil
                 submit-email-template-filename
                 application-id
                 guardian?
                 payment-url))

(defn create-edit-email [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application-id guardian?]
  (create-emails-by-gathering-data koodisto-cache
                 tarjonta-service organization-service ohjausparametrit-service
                 edit-email-subjects
                 #(str "templates/email_edit_confirmation_template_"
                       (name %)
                       ".html")
                 application-id
                 guardian?
                 nil))

(defn create-refresh-secret-email
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application-id]
  (create-emails-by-gathering-data koodisto-cache
                 tarjonta-service organization-service ohjausparametrit-service
                 refresh-secret-email-subjects
                 #(str "templates/email_refresh_secret_template_" (name %) ".html")
                 application-id
                 false
                 nil))

(defn create-tutu-decision-email
  [application-id message payment-url]
  (let [application                     (application-store/get-application application-id)
        template-name                   (fn [_] "templates/tutu_decision_email_template.html")
        lang                            (keyword (:lang application))
        applier-recipients              (->> (:answers application)
                                             (filter #(= "email" (:key %)))
                                             (map :value))
        translations                    (reduce-kv #(assoc %1 %2 (get %3 lang))
                                                   {}
                                                   tutu-decision-email)
        template-params                 (merge
                                         {:application-oid            (:key application)
                                          :payment-url                payment-url
                                          :message                    (->safe-html message)
                                          :decision-info-email        "recognition@oph.fi"}
                                         translations)
        subject                         (str (:subject-prefix translations) ": " (:header translations))
        applicant-email-data            (email-util/make-email-data applier-recipients subject template-params)
        render-file-fn                  (fn [template-params]
                                          (selmer/render-file (template-name lang) template-params))]
    (email-util/render-emails-for-applicant-and-guardian applicant-email-data nil render-file-fn)))

