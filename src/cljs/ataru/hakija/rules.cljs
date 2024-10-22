(ns ataru.hakija.rules
  (:require [ataru.hakija.hakija-ajax :as ajax]
            [ataru.hakija.pohjakoulutusristiriita :as pohjakoulutusristiriita]
            [ataru.preferred-name :as pn]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            [ataru.hakija.arvosanat.valinnainen-oppiaine-koodi :as vok]
            [ataru.date :as date]
            [ataru.hakija.demo :as demo]
            [clojure.string :as string]
            [ataru.hakija.ssn :as ssn]
            [ataru.hakija.form-tools :as form-tools]
            [ataru.translations.texts :as texts])
  (:require-macros [cljs.core.match :refer [match]]))

(defn- update-value [current-value update-fn]
  (if (vector? current-value)
    [(update-value (first current-value) update-fn)]
    (update-fn current-value)))

(defn- blank-value? [value]
  (or (and (string? value)
           (string/blank? value))
      (and (vector? value)
           (-> value first blank-value?))))

(defn- set-empty-validity
  [a cannot-view? valid?]
  (if (and (blank-value? (:value a))
           (not cannot-view?))
    (-> a
        (assoc :valid valid?)
        (update :values update-value #(assoc % :valid valid?)))
    a))

(defn- hide-field
  ([db id]
   (hide-field db id ""))
  ([db id value]
   (if-let [_ (some #(when (= id (keyword (:id %))) %)
                    (:flat-form-content db))]
     (as-> db db'
           (assoc-in db' [:application :ui id :visible?] false)
           (cond-> db'
                   (-> db' :application :answers id)
                   (update-in [:application :answers id]
                              (fn [answer]
                                (-> answer
                                    (assoc :valid true)
                                    (update :value update-value (constantly value))
                                    (update :values update-value (constantly {:value value :valid true})))))))
     db)))

(defn- show-field
  ([db id]
   (show-field db id false))
  ([db id valid?]
   (if-let [field (some #(when (= id (keyword (:id %))) %)
                        (:flat-form-content db))]
     (as-> db db'
           (assoc-in db' [:application :ui id :visible?] true)
           (let [cannot-view? (and (get-in db [:application :editing?])
                                   (:cannot-view field))]
             (cond-> db'
                     (-> db' :application :answers id)
                     (update-in [:application :answers id] set-empty-validity cannot-view? valid?))))
     db)))

(defn- toggle-require-field
  [db id required?]
  (if-let [field (form-tools/get-field-from-content db id)]
    (let [remove-required (fn [validators] (filter #(not= "required" %) validators))
          add-required (fn [validators] (conj validators "required"))
          fn-to-use (if required?
                      add-required
                      remove-required)
          modified-validators (-> (:validators field)
                                  (fn-to-use)
                                  (distinct))
          updated-field (assoc field :validators modified-validators)
          answer (get-in db [:application :answers (keyword id)])
          set-validity-if-blank (fn [db]
                                  (cond
                                    (and required? (blank-value? (:value answer)))
                                    (assoc-in db [:application :answers (keyword id) :valid] false)

                                    (and (not required?) (blank-value? (:value answer)))
                                    (assoc-in db [:application :answers (keyword id) :valid] true)

                                    :else
                                    db))]
        (-> db
              (form-tools/update-field-in-db updated-field)
              (set-validity-if-blank)))
    db))

(defn- have-finnish-ssn
  ^{:dependencies [:nationality]}
  [db]
  (let [values (get-in db [:application :answers :nationality :values])]
    (if (empty? (filter (fn [[v & _]] (= (:value v) finland-country-code)) values))
      (-> db
          (update-in [:application :answers :have-finnish-ssn]
                     (fn [a]
                       (if (= "" (:value a))
                         (-> a
                             (merge {:valid true
                                     :value "true"})
                             (update :values merge {:valid true
                                                    :value "true"}))
                         a)))
          (assoc-in [:application :ui :have-finnish-ssn :visible?] true))
      (hide-field db :have-finnish-ssn "true"))))

(defn- ssn
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (show-field db :ssn (demo/demo? db))
      (hide-field db :ssn))))

(defn- optional-email
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (if (and
        (some? (form-tools/get-field-from-flat-form-content db "onr-2nd"))
        (not (demo/demo? db)))
    (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])
          is-required-needed (not= "true" have-finnish-ssn)]
      (toggle-require-field db "email" is-required-needed))
    db))

(defn- parse-birth-date-from-ssn
  [ssn]
  (let [century-sign (nth ssn 6)
        day          (subs ssn 0 2)
        month        (subs ssn 2 4)
        year         (subs ssn 4 6)
        century      (case century-sign
                       "+" "18"
                       "-" "19"
                       "A" "20")]
    (str day "." month "." century year)))

(defn- parse-gender-from-ssn
  [ssn]
  (if (zero? (mod (js/parseInt (nth ssn 9)) 2))
    "2"                                                     ;; based on koodisto-values
    "1"))

(defn- birth-date-and-gender
  ^{:dependencies [:have-finnish-ssn :ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])
        ssn              (get-in db [:application :answers :ssn])
        cannot-view?     (and (get-in db [:application :editing?])
                              (->> (:flat-form-content db)
                                   (filter #(= "ssn" (:id %)))
                                   first
                                   :cannot-view))
        demo?            (demo/demo? db)
        dob-from-session (get-in db [:oppija-session :fields :birth-date :value])]
    (if (= "true" have-finnish-ssn)
      (let [[birth-date gender] (cond (and (:valid ssn)
                                           (not-empty (:value ssn)))
                                      [(ssn/parse-birth-date-from-ssn demo? (:value ssn))
                                       (ssn/parse-gender-from-ssn demo? (:value ssn))]
                                      cannot-view?
                                      [(get-in db [:application :answers :birth-date :value])
                                       (get-in db [:application :answers :gender :value])]
                                      (not (string/blank? dob-from-session))
                                      [dob-from-session ""]
                                      :else
                                      ["" ""])]
        (-> db
            (hide-field :birth-date birth-date)
            (hide-field :gender gender)))
      (-> db
          (show-field :birth-date)
          (show-field :gender)))))

(defn- birth-date
  ^{:dependencies [:have-finnish-ssn :ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])
        ssn              (get-in db [:application :answers :ssn])
        cannot-view?     (and (get-in db [:application :editing?])
                              (->> (:flat-form-content db)
                                   (filter #(= "ssn" (:id %)))
                                   first
                                   :cannot-view))
        demo?            (demo/demo? db)
        dob-from-session (get-in db [:oppija-session :fields :birth-date :value])]
    (if (= "true" have-finnish-ssn)
      (let [birth-date (cond (and (:valid ssn)
                                  (not-empty (:value ssn)))
                             (ssn/parse-birth-date-from-ssn demo? (:value ssn))
                             cannot-view?
                             (get-in db [:application :answers :birth-date :value])
                             (not (string/blank? dob-from-session))
                             dob-from-session
                             :else
                             "")]
        (hide-field db :birth-date birth-date))
      (show-field db :birth-date))))

(defn- passport-number
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (hide-field db :passport-number)
      (show-field db :passport-number true))))

(defn- national-id-number
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (hide-field db :national-id-number)
      (show-field db :national-id-number true))))

(defn- birthplace
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (hide-field db :birthplace)
      (show-field db :birthplace))))

(defn- guardian-contact
  ^{:dependencies [:birth-date]}
  [db]
  (let [fields [:guardian-contact-information
                :guardian-contact-minor-secondary
                :guardian-firstname
                :guardian-lastname
                :guardian-phone
                :guardian-email
                :guardian-firstname-secondary
                :guardian-lastname-secondary
                :guardian-phone-secondary
                :guardian-email-secondary]
        editing? (get-in db [:application :editing?])
        minor? (if editing?
                 (get-in db [:application :person :minor])
                 (date/minor? (get-in db [:application :answers :birth-date :value])))
        all-empty (every? (fn [field]
                            (let [value (get-in db [:application :answers field :value])]
                              (cond
                                (vector? value) (every? string/blank? value)
                                :else (string/blank? value)))) fields)]
    (if (or (not all-empty) minor?)
      (reduce (fn [db' field] (show-field db' field true)) db fields)
      (reduce (fn [db' field] (hide-field db' field)) db fields))))

(defn- toggle-birth-date-based-fields [db]
  (guardian-contact db))

(defn swap-ssn-birthdate-based-on-nationality
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      passport-number
      national-id-number
      birthplace
      birth-date-and-gender
      toggle-birth-date-based-fields))

(defn- update-gender-and-birth-date-based-on-ssn
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      passport-number
      national-id-number
      birthplace
      birth-date-and-gender
      toggle-birth-date-based-fields))

(defn- toggle-ssn-based-fields
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      optional-email
      passport-number
      national-id-number
      birthplace
      birth-date-and-gender
      toggle-birth-date-based-fields))

(defn- toggle-ssn-based-fields-without-gender
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      optional-email
      passport-number
      national-id-number
      birthplace
      birth-date
      toggle-birth-date-based-fields))

(defn- postal-office
  ^{:dependencies [:country-of-residence :postal-code]}
  [db]
  (let [answers     (-> db :application :answers)
        country     (-> answers :country-of-residence :value)
        is-finland? (or (= country finland-country-code)
                        (string/blank? country))
        postal-code (-> answers :postal-code)
        auto-input? (and is-finland?
                         (= 5 (count (:value postal-code))))]
    (when auto-input?
      (ajax/http {:method        :get
                  :url           (str "/hakemus/api/postal-codes/" (:value postal-code))
                  :handler       [:application/handle-postal-code-input]
                  :error-handler [:application/handle-postal-code-error]}))
    (-> db
        (update-in [:application :answers :postal-office]
                   merge {:valid (not is-finland?)
                          :value ""})
        (update-in [:application :answers :postal-office :values]
                   merge {:valid (not is-finland?)
                          :value ""})
        (assoc-in [:application :ui :postal-office :visible?] is-finland?)
        (assoc-in [:application :ui :postal-office :disabled?] auto-input?))))

(defn- home-town-and-city
  ^{:dependencies [:country-of-residence]}
  [db]
  (let [country     (get-in db [:application :answers :country-of-residence :value])
        is-finland? (or (= country finland-country-code)
                        (string/blank? country))
        home-town-from-session (or (get-in db [:oppija-session :fields :home-town :value]) "")]
    (if is-finland?
      (-> db
          (show-field :home-town)
          (hide-field :city))
      (-> db
          (hide-field :home-town home-town-from-session)
          (show-field :city)))))

(defn- select-postal-office-based-on-postal-code
  [db _]
  (postal-office db))

(defn- prefill-preferred-first-name
  [db _]
  (let [answers        (-> db :application :answers)
        first-name     (-> answers :first-name :value (string/trim) (string/split #" ") first)
        preferred-name (-> answers :preferred-name :value)]
    (cond
      (and first-name (string/blank? preferred-name))
      (-> db
          (update-in [:application :answers :preferred-name] merge
                     {:value first-name
                      :valid true
                      :errors []})
          (update-in [:application :answers :preferred-name :values] merge
                     {:valid true
                      :value first-name}))

      (or (and first-name (not (string/blank? preferred-name)))
          (and (string/blank? first-name) (string/blank? preferred-name)))
      (-> db
          (update-in [:application :answers :preferred-name] merge
                     (let [valid? (pn/main-first-name? {:value preferred-name :answers-by-key answers})]
                       {:valid valid?
                        :errors (if valid? [] [(texts/person-info-validation-error :main-first-name)])}))
          (update-in [:application :answers :preferred-name :values] merge
                     {:valid (pn/main-first-name? {:value preferred-name :answers-by-key answers})}))
      :else db)))

(defn- change-country-of-residence
  [db _]
  (-> db
      home-town-and-city
      postal-office))

(defn- pohjakoulutusristiriita
  [db _]
  (if (empty? (pohjakoulutusristiriita/hakukohteet-wo-applicable-base-education db))
    (assoc-in db [:application :ui :pohjakoulutusristiriita :visible?] false)
    (assoc-in db [:application :ui :pohjakoulutusristiriita :visible?] true)))

(defn- swedish-nationality? [db]
  (-> db
      :application
      :answers
      :language
      :value
      (= "SV")))

(defn- arvosana-rivi-dropdown-without-answer? [db answer-key]
  (let [value (-> db
                  :application
                  :answers
                  answer-key
                  :value)]
    (and (vector? value)
         (-> value first vector?)
         (-> value first first (= "")))))

(defn- hide-kieli-oppiaine-row [db oppiaine]
  (let [arvosana  (keyword (str "arvosana-" (name oppiaine)))
        oppimaara (keyword (str "oppimaara-kieli-" (name oppiaine))) ; oppimäärä viittaa tässä varsinaiseen kieleen, jota opiskellaan
        hide      (fn hide [db]
                    (-> db
                        (hide-field oppiaine)
                        (hide-field arvosana)
                        (hide-field oppimaara)))]
    (cond-> db
            (and (arvosana-rivi-dropdown-without-answer? db arvosana)
                 (arvosana-rivi-dropdown-without-answer? db oppimaara))
            hide)))

(defn- arvosanat-module-visible?
  [db]
  (get-in db [:application :ui :arvosanat-peruskoulu :visible?]))

(defn- toggle-arvosanat-module-aidinkieli-ja-kirjallisuus-oppiaineet
  [db _]
  (if (arvosanat-module-visible? db)
    (if (swedish-nationality? db)
      (-> db
        (show-field :A2)
        (show-field :oppimaara-kieli-A2)
        (show-field :arvosana-A2)
        (hide-kieli-oppiaine-row :B1))
      (-> db
        (show-field :B1)
        (show-field :oppimaara-kieli-B1)
        (show-field :arvosana-B1)
        (hide-kieli-oppiaine-row :A2)))
    db))

(defn- set-oppiaine-valinnainen-kieli-value
  [db _]
  (let [last-idx (-> db :application :ui :oppiaineen-arvosanat-valinnaiset-kielet :count dec)]
    (when (>= last-idx 0)
      (reduce (fn [db' answer-key]
                (update-in
                  db'
                  [:application :answers answer-key]
                  (fn [answer]
                    (as-> answer answer'
                          (update answer'
                                  :values
                                  (fn [values]
                                    (->> values
                                         (map-indexed
                                           (fn [values-idx values']
                                             (mapv (fn [value]
                                                     (let [oppiaineen-koodi               (some-> db :application :answers :oppiaine-valinnainen-kieli :values (get values-idx) first :value (subs vok/valinnainen-kieli-id-oppiaine-koodi-idx))
                                                           last-valinnainen-oppiaine-row? (= values-idx last-idx)
                                                           value-not-blank?               (-> value :value string/blank? not)
                                                           valid?                         (match [last-valinnainen-oppiaine-row? value-not-blank? oppiaineen-koodi answer-key]
                                                                                                 [true _ _ _]
                                                                                                 true

                                                                                                 [false false "a" :oppimaara-kieli-valinnainen-kieli]
                                                                                                 true

                                                                                                 [false false (_ :guard #(not= % "a")) :oppimaara-a-valinnainen-kieli]
                                                                                                 true

                                                                                                 [false true _ _]
                                                                                                 true

                                                                                                 :else
                                                                                                 false)]
                                                       (assoc value :valid valid?)))
                                                   values')))
                                         (into []))))
                          (assoc answer'
                                 :valid
                                 (every? (comp true? :valid first)
                                         (:values answer')))))))
              db
              [:oppimaara-a-valinnainen-kieli
               :oppimaara-kieli-valinnainen-kieli
               :oppiaine-valinnainen-kieli
               :arvosana-valinnainen-kieli]))))

(defn- find-followup-ids-by-parent-id
  [db parent-id]
  (let [followup-ids (mapv :id (filter #(= parent-id (:followup-of %)) (:flat-form-content db)))
        nested-ids (flatten (map #(find-followup-ids-by-parent-id db %) followup-ids))]
    (concat followup-ids nested-ids)))

(defn- show-followups-of-property-options
  [db _]
  (let [property-field-ids       (map :id (filter #(= "formPropertyField" (:fieldClass %)) (:flat-form-content db)))
        all-followup-ids-beneath (flatten (map #(find-followup-ids-by-parent-id db %) property-field-ids))
        is-explicitly-hidden?     (fn [id] (get-in db [:flat-form-content (keyword id) :params :hidden] false))]
    (reduce
      (fn [db' followup-id] (assoc-in db' [:application :ui (keyword followup-id) :visible?]
                                      (not (is-explicitly-hidden? followup-id))))
      db
      all-followup-ids-beneath)))

(defn- hakija-rule-to-fn [rule]
  (case rule
    :prefill-preferred-first-name
    prefill-preferred-first-name
    :swap-ssn-birthdate-based-on-nationality
    swap-ssn-birthdate-based-on-nationality
    :update-gender-and-birth-date-based-on-ssn
    update-gender-and-birth-date-based-on-ssn
    :select-postal-office-based-on-postal-code
    select-postal-office-based-on-postal-code
    :toggle-ssn-based-fields
    toggle-ssn-based-fields
    :toggle-birthdate-based-fields
    toggle-birth-date-based-fields
    :toggle-ssn-based-fields-without-gender
    toggle-ssn-based-fields-without-gender
    :change-country-of-residence
    change-country-of-residence
    :pohjakoulutusristiriita
    pohjakoulutusristiriita
    :toggle-arvosanat-module-aidinkieli-ja-kirjallisuus-oppiaineet
    toggle-arvosanat-module-aidinkieli-ja-kirjallisuus-oppiaineet
    :set-oppiaine-valinnainen-kieli-value
    set-oppiaine-valinnainen-kieli-value
    :show-followups-of-property-options
    show-followups-of-property-options))

(defn run-rules
  ([db rules]
   (run-rules db rules hakija-rule-to-fn))
  ([db rules rule-to-fn]
   {:pre  [(map? db) (or (empty? rules) (map? rules))]
    :post [map?]}
   (reduce-kv (fn [db rule arg]
                (or ((rule-to-fn rule) db arg) db))
              db
              rules)))

(defn run-all-rules
  [db flat-form-content]
  (->> flat-form-content
       (map :rules)
       (remove empty?)
       (reduce run-rules db)))

(defn run-pohjakoulutusristiriita-rule
  [db]
  (run-rules db {:pohjakoulutusristiriita nil}))
