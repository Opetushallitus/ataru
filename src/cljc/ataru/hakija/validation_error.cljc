(ns ataru.hakija.validation-error
  (:require [clojure.string :as string]
            [ataru.translations.texts :as texts]
            [ataru.number :refer [gte lte numeric-matcher]]))

(defn ssn-applied-error
  [preferred-name]
  {:fi [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Hei " preferred-name "!")
              "Hei!")]
        [:p "Tässä haussa voit lähettää vain yhden (1) hakemuksen. "
         [:strong "Olet jo lähettänyt hakemuksen"]
         " tähän hakuun ja siksi et voi lähettää toista hakemusta.
         Jos lähetät useampia hakemuksia, viimeisin jätetty hakemus
         jää voimaan ja aiemmin lähettämäsi hakemukset perutaan."]
        [:p "Jos haluat "
         [:strong "muuttaa hakemustasi"]
         ", voit tehdä muokkaukset sähköpostiisi saapuneen hakemuksen muokkauslinkin kautta
         tai vaihtoehtoisesti kirjautumalla Oma Opintopolku -palveluun."]
        [:p "Ongelmatilanteissa ole yhteydessä hakemaasi oppilaitokseen."]]
   :sv [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Hej " preferred-name "!")
              "Hej!")]
        [:p "I denna ansökan kan du skicka in endast en (1) ansökan."
         [:strong "Du redan har skickat en ansökning"]
         " i denna ansökan och därför kan du inte skicka en annan
          ansökning. Om du skickar in flera beaktas endast den som
          du skickat in senast och alla tidigare ansökningar raderas."]
        [:p "Om du vill "
         [:strong "ändra din ansökning"]
         " kan du under ansökningstiden göra det via en länk i e-postmeddelandet
         som du får som bekräftelse över din ansökan eller genom att logga in i tjänsten Min Studieinfo."]
        [:p "Vid eventuella problemsituationer kontakta den läroanstalt du
         söker till."]]
   :en [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Dear " preferred-name ",")
              "Dear applicant,")]
        [:p "You can only submit one (1) application form in this application."
         [:strong "You have already submitted an application"]
         " to this admission and therefore cannot submit another
          application. If you submit several applications, only the latest one
          will be taken into consideration and all others will be deleted."]
        [:p "If you want to, you can "
         [:strong "make changes"]
         " to your application during the application period by using
         the link in the confirmation email or by logging in to My Studyinfo."]
        [:p "If you have any problems, please contact the educational
         institution."]]})

(defn person-info-validation-error [msg-key]
  (when (some? msg-key)
    (when-let [texts (get texts/person-info-module-validation-error-texts msg-key)]
      {:fi [:div.application__person-info-validation-error-dialog {:class msg-key}
            [:p (:fi texts)]]
       :sv [:div.application__person-info-validation-error-dialog {:class msg-key}
            [:p (:sv texts)]]
       :en [:div.application__person-info-validation-error-dialog {:class msg-key}
            [:p (:en texts)]]})))

(defn- numeric-validation-error-key
  [value params]
  (let [[_ _ integer-part _ _ decimal-part] (re-matches numeric-matcher value)
        decimal-places (:decimals params)
        min-value (:min-value params)
        max-value (:max-value params)]
    (cond
      (not integer-part)
      :not-a-number

      (and decimal-part
           (not decimal-places))
      :not-an-integer

      (and decimal-part
           (> (count decimal-part)
              decimal-places))
      :too-many-decimals

      (and (some? max-value)
           (some? min-value)
           (or (gte value max-value)
               (lte value min-value)))
      :not-in-range

      (and (some? max-value) (gte value max-value))
      :too-big

      (and (some? min-value) (lte value min-value))
      :too-small

      :else
      :not-a-number)))

(defn- numeric-validation-error [{:keys [field-descriptor value]}]
  (let [params                      (-> field-descriptor :params)
        error-key (numeric-validation-error-key value params)
        texts (texts/numeric-validation-error-texts error-key params)]
    {:fi [:div.application__person-info-validation-error-dialog {:class error-key}
          [:p (:fi texts)]]
     :sv [:div.application__person-info-validation-error-dialog {:class error-key}
          [:p (:sv texts)]]
     :en [:div.application__person-info-validation-error-dialog {:class error-key}
          [:p (:en texts)]]}))

(defn validation-error [msg-key params]
  (cond
    (= msg-key :numeric) (numeric-validation-error params)
    :else (person-info-validation-error msg-key)))