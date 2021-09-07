(ns ataru.email.email-util)

(def from-address "no-reply@opintopolku.fi")

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