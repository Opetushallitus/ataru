(ns ataru.kk-application-payment.kk-application-payment-permit-check-inheritance
  "Copies virkailija's residence permit check to the applicant's later applications for the same starting term.

   A later application gets the check, the attachment reviews and the reviewed payment obligation of an application
   whose payment obligation has been reviewed. The main conditions (see inherit-permit-checks! and inheritance-for
   for all) are that the later application
   - has the same document option,
   - was submitted on a day the permit card is valid, and
   - has the permit attachment. The files are not compared, as the applicant may take new pictures of the permit card
     for each application.

   If the check found the applicant not obligated to pay and the payment of the application it is copied from is not
   required, the check is copied also when the attachment is missing (see missing-attachment-source-candidates). The
   missing attachment then does not make the applicant obligated, as long as the inherited check is unchanged and the
   payment obligation is reviewed (see kk-application-payment/attachments-invalid-and-deadline-passed?). If the permit
   was found in Migri and the basis is noted, the check decides the reviewed payment obligation regardless of the
   attachments (see kk-application-payment/permit-check-payment-obligation).

   A check is copied only once, and later changes in the original application do not change the copy or its effect on
   the payment."
  (:require [ataru.applications.application-store :as application-store]
            [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment-permit-check-store :as store]
            [ataru.log.audit-log :as audit-log]
            [ataru.time :as time]
            [ataru.util :as util]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [taoensso.timbre :as log]))

(def inherited-event-type "kk-payment-permit-check-inherited")

(def ^:private audit-log-session {:cronjob "inherit-kk-application-payment-permit-checks"})

(defn- answer-value
  [answers attachment-key]
  (:value (get answers (keyword attachment-key))))

(defn delivered?
  "Whether the applicant has delivered the attachment."
  [answers attachment-key]
  (boolean (some #(and (string? %) (not (string/blank? %)))
                 (flatten [(answer-value answers attachment-key)]))))

(defn- obligation-reviews
  [application-key]
  (filter #(= "kk-application-payment-obligation" (:requirement %))
          (application-store/get-application-hakukohde-reviews application-key)))

(defn- reviewed-reviews
  [reviews]
  (filter #(= "reviewed" (:state %)) reviews))

(defn- submitted-date
  [application]
  (.toLocalDate (time/to-time-zone (:submitted application) (time/time-zone-for-id "Europe/Helsinki"))))

(defn not-obligated?
  "A check that does not state that the applicant is obligated to pay."
  [check]
  (contains? #{nil "not-obligated"} (:payment-obligation-basis check)))

(defn- valid-at-submission?
  [check application]
  (when-let [valid-until (some-> (:permit-valid-until check) java.time.LocalDate/parse)]
    (not (.isBefore valid-until (submitted-date application)))))

(defn- source-candidates
  "Checks of the attachment in the applications submitted before the target application, whose payment obligation
   has been reviewed."
  [applications target attachment-key]
  (for [source applications
        :when (time/before? (:submitted source) (:submitted target))
        ; Read from the database, as the source may have inherited a check earlier in the same run
        :when (seq (reviewed-reviews (obligation-reviews (:key source))))
        :let [check (store/get-permit-check (:key source) attachment-key)]
        :when (and (some? check) (valid-at-submission? check target))]
    {:application source
     :check       check}))

(defn- invalid-attachments?
  [application-key]
  (some #(and (contains? payment-module/kk-application-payment-exempt-attachment-keys (:attachment-key %))
              (contains? payment-module/kk-application-payment-invalid-attachment-states (:state %)))
        (application-store/get-application-attachment-reviews application-key false)))

(defn- missing-attachment-source-candidates
  "Sources for an application without the residence permit attachment. The check of the source found the applicant
   not obligated to pay, and the payment of the source is not required at the start of the run. The source must not
   have attachments marked missing or invalid, as they may make it obligated later in the same run. The attachments
   do not matter when the permit was found in Migri and the basis is noted (see
   kk-application-payment/permit-check-payment-obligation)."
  [applications not-required-application-keys target attachment-key]
  (->> (source-candidates (filter (comp not-required-application-keys :key) applications) target attachment-key)
       (filter (comp not-obligated? :check))
       (remove (fn [{:keys [application check]}]
                 (and (not (and (true? (:found-in-migri check))
                                (= "not-obligated" (:payment-obligation-basis check))))
                      (invalid-attachments? (:key application)))))))

(defn- latest-source
  [candidates]
  (last (sort-by (comp time/to-instant :checked-time :check) candidates)))

; A check is inherited only once, and not at all after the virkailija has checked the attachment in the application
; or the check has been reset because of a new attachment, so that the new attachment gets reviewed
(def ^:private handled-event-types
  #{inherited-event-type "kk-payment-permit-check-updated" "kk-payment-permit-check-reset"})

(defn- handled-before?
  [application-key attachment-key]
  (some #(and (contains? handled-event-types (:event-type %))
              (= attachment-key (:review-key %)))
        (application-store/get-application-events application-key)))

(defn- decided-after-check?
  "A payment obligation the virkailija has decided on (e.g. reviewed or sent to Migri review) in the target
   application after the original check is not overridden by the inheritance."
  [target-obligation-reviews check]
  (some #(and (not= "unreviewed" (:state %))
              (time/after? (:modified-time %) (:checked-time check)))
        target-obligation-reviews))

(defn- latest-attachment-reviews
  "The latest review state of each attachment of the document option in the source application, when the attachment
   has been reviewed."
  [source-key attachment-keys]
  (->> (application-store/get-application-attachment-reviews source-key false)
       (filter #(contains? attachment-keys (:attachment-key %)))
       (group-by :attachment-key)
       (map (fn [[attachment-key reviews]]
              [attachment-key (:state (last (sort-by (comp time/to-instant :modified-time) reviews)))]))
       (remove (comp #{"not-checked"} second))
       (into {})))

(defn- reviewed-after-check?
  "An attachment the virkailija has reviewed in the target application after the original check keeps its review."
  [attachment-review check]
  (and (not= "not-checked" (:state attachment-review))
       (time/after? (:modified-time attachment-review) (:checked-time check))))

(defn- inheritance-for
  "The inheritance to do for the target application or nil."
  [applications not-required-application-keys target]
  (let [answers        (util/application-answers-by-key target)
        option         (:value (get answers (keyword payment-module/kk-application-payment-choice-key)))
        attachment-key (get payment-module/kk-application-payment-residence-permit-attachment-key-by-option option)]
    (when (and (some? attachment-key)
               (nil? (store/get-permit-check (:key target) attachment-key))
               (not (handled-before? (:key target) attachment-key)))
      (when-let [{:keys [application check]} (latest-source
                                               (if (delivered? answers attachment-key)
                                                 (source-candidates applications target attachment-key)
                                                 (missing-attachment-source-candidates
                                                   applications not-required-application-keys target
                                                   attachment-key)))]
        (let [target-obligation-reviews (obligation-reviews (:key target))]
          (if (decided-after-check? target-obligation-reviews check)
            (log/info "Not inheriting kk application payment permit check of attachment" attachment-key
                      "from application" (:key application) "to application" (:key target)
                      "as the payment obligation has been decided on after the check")
            {:target            target
             :source            application
             :check             check
             :attachment-key    attachment-key
             :attachment-states (->> (get payment-module/kk-application-payment-attachment-keys-by-option option)
                                     (filter (partial delivered? answers))
                                     set
                                     (latest-attachment-reviews (:key application)))}))))))

(defn- inherit!
  "Copies the check, the attachment reviews and the reviewed payment obligation in a transaction. Returns the
   inherited check."
  [{:keys [target source check attachment-key attachment-states]} audit-logger]
  (let [target-key (:key target)
        now        (time/now)]
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (when-let [inherited (store/insert-inherited-permit-check!
                             connection
                             {:application_key                 target-key
                              :attachment_key                  attachment-key
                              :found_in_migri                  (:found-in-migri check)
                              :permit_valid_until              (:permit-valid-until check)
                              :payment_obligation_basis        (:payment-obligation-basis check)
                              :payment_obligation_other_reason (:payment-obligation-other-reason check)
                              :virkailija_oid                  (:virkailija-oid check)
                              :organization_oid                (:organization-oid check)
                              :checked_time                    (:checked-time check)
                              ; A check inherited further refers to the application it was originally made in
                              :inherited_from_application_key  (or (:inherited-from-application-key check)
                                                                   (:key source))})]
        (log/info "Inheriting kk application payment permit check of attachment" attachment-key
                  "from application" (:key source) "to application" target-key)
        (application-store/add-application-event-in-tx connection
                                                       {:application-key target-key
                                                        :event-type      inherited-event-type
                                                        :review-key      attachment-key}
                                                       nil)
        (doseq [{:keys [hakukohde] review-attachment-key :attachment-key :as attachment-review}
                (application-store/get-application-attachment-reviews target-key false)
                :let [state (get attachment-states review-attachment-key)]
                :when (and (some? state) (not (reviewed-after-check? attachment-review check)))]
          (application-store/save-inherited-attachment-review-in-tx connection target-key hakukohde
                                                                    review-attachment-key state now
                                                                    audit-log-session audit-logger))
        (doseq [hakukohde (:hakukohde target)]
          (application-store/save-inherited-application-hakukohde-review-in-tx connection target-key hakukohde
                                                                               "kk-application-payment-obligation"
                                                                               "reviewed" now audit-log-session
                                                                               audit-logger))
        inherited))))

(defn inherit-permit-checks!
  "Inherits the checks for the given latest applications of the applicant to the same starting term. The
   applications are handled in submission order, so that a check inherited in the run can be inherited further.
   Applications whose payment can no longer change (final-application-keys) do not inherit, but their checks are
   inherited. For a missing attachment, the source's payment must not be required at the start of the run
   (not-required-application-keys), also when the source has inherited its check earlier in the same run. Returns the
   keys of the applications that inherited a check."
  [audit-logger applications final-application-keys not-required-application-keys]
  (let [applications (sort-by (comp time/to-instant :submitted) applications)]
    (->> applications
         (remove (comp final-application-keys :key))
         (keep (fn [target]
                 (when-let [inheritance (inheritance-for applications not-required-application-keys target)]
                   (when-let [inherited (inherit! inheritance audit-logger)]
                     (audit-log/log audit-logger
                                    {:new       inherited
                                     :id        {:applicationOid (:key target)
                                                 :attachmentKey  (:attachment-key inheritance)}
                                     :session   audit-log-session
                                     :operation audit-log/operation-new})
                     (:key target)))))
         doall)))
