(ns ataru.forms.form-feedback-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.excel-export :as excel-export]
            [ataru.applications.feedback-excel :as feedback-excel]
            [ataru.forms.form-access-control :as form-access-control]
            [ataru.forms.form-store :as form-store]
            [ataru.log.audit-log :as audit-log]
            [ataru.util :as util]))

(defn- feedback-filename [form lang]
  (excel-export/create-filename
    (str "palautteet_" (or (util/non-blank-val (:name form) [lang :fi :sv :en])
                           (:key form)))))

(defn get-feedback-excel
  "Palauttaa lomakkeen palautteet Excel-tiedostona, jos käyttäjällä on oikeus
   nähdä lomake. Muuten palauttaa virheen {:error :not-found}, jos lomaketta
   ei löydy, tai {:error :forbidden}, jos käyttäjällä ei ole siihen oikeutta."
  [form-key lang session tarjonta-service organization-service audit-logger]
  (let [form (form-store/fetch-by-key form-key)]
    (cond
      (nil? form)
      {:error :not-found}

      (not (form-access-control/form-visible-to-user? form session tarjonta-service organization-service))
      {:error :forbidden}

      :else
      (let [feedback (application-store/get-application-feedback-by-form-key form-key)]
        (audit-log/log audit-logger
                       {:new       {:form-key       form-key
                                    :feedback-count (count feedback)}
                        :id        {:formKey form-key}
                        :session   session
                        :operation audit-log/operation-read})
        {:filename (feedback-filename form lang)
         :body     (feedback-excel/export-feedback feedback lang)}))))
