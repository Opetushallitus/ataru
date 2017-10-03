(ns ataru.hakija.application-validators
  #?(:cljs (:require-macros [cljs.core.async.macros :as asyncm]))
  (:require [clojure.string]
            [ataru.email :as email]
            [ataru.ssn :as ssn]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            #?(:clj  [clojure.core.async :as async]
               :cljs [cljs.core.async :as async])
            #?(:clj  [clojure.core.async :as asyncm])
            #?(:clj  [clj-time.core :as c]
               :cljs [cljs-time.core :as c])
            #?(:clj  [clj-time.format :as f]
               :cljs [cljs-time.format :as f])
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])))

(defn- email-applied-error
  [email preferred-name]
  [{:fi [:div
         [:p (if (not (clojure.string/blank? preferred-name))
               (str "Hei " preferred-name "!")
               "Hei!")]
         [:p "Huomasimme, että "
          [:strong "olet jo lähettänyt hakemuksen"]
          " tähän hakuun ja siksi et voi lähettää toista hakemusta."]
         [:p "Jos haluat "
          [:strong "muuttaa hakemustasi"]
          " niin löydät muokkauslinkin sähköpostiviestistä jonka sait
         jättäessäsi edellisen hakemuksen."]
         [:p "Tarkista myös, että syöttämäsi sähköpostiosoite "
          [:strong email]
          " on varmasti oikein."]
         [:p "Ongelmatilanteissa ole yhteydessä hakemaasi oppilaitokseen."]]}])

(defn- email-applied-error-when-modifying
  [email preferred-name]
  [{:fi [:div
         [:p (if (not (clojure.string/blank? preferred-name))
               (str "Hei " preferred-name "!")
               "Hei!")]
         [:p "Antamallasi sähköpostiosoitteella "
          [:strong email]
          " on jo jätetty hakemus. Tarkista, että syöttämäsi sähköpostiosoite
          on varmasti oikein."]]}])

(defn- ssn-applied-error
  [preferred-name]
  [{:fi [:div
          [:p (if (not (clojure.string/blank? preferred-name))
                (str "Hei " preferred-name "!")
                "Hei!")]
          [:p "Huomasimme, että "
           [:strong "olet jo lähettänyt hakemuksen"]
           " tähän hakuun ja siksi et voi lähettää toista hakemusta."]
          [:p "Jos haluat "
           [:strong "muuttaa hakemustasi"]
           " niin löydät muokkauslinkin sähköpostiviestistä jonka sait
         jättäessäsi edellisen hakemuksen. Ongelmatilanteissa ole yhteydessä
         hakemaasi oppilaitokseen."]]}])

(defn ^:private required?
  [value _ _]
  (if (or (seq? value) (vector? value))
    (not (empty? value))
    (not (clojure.string/blank? value))))

(defn- residence-in-finland?
  [answers-by-key]
  (= (str finland-country-code)
     (str (-> answers-by-key :country-of-residence :value))))

(defn- have-finnish-ssn?
  [answers-by-key]
  (or (= "true" (get-in answers-by-key [:have-finnish-ssn :value]))
      (ssn/ssn? (get-in answers-by-key [:ssn :value]))))

(defn- ssn?
     [has-applied value answers-by-key field-descriptor]
     (let [multiple? (get-in field-descriptor
                             [:params :can-submit-multiple-applications]
                             true)
           haku-oid (get-in field-descriptor
                            [:params :haku-oid])
           preferred-name (:preferred-name answers-by-key)]
       (asyncm/go
         (cond (not (ssn/ssn? value))
               [false []]
               (and (not multiple?)
                    (async/<! (has-applied haku-oid {:ssn value})))
               [false (ssn-applied-error (when (:valid preferred-name)
                                           (:value preferred-name)))]
               :else
               [true []]))))

(defn- email?
     [has-applied value answers-by-key field-descriptor]
     (let [multiple? (get-in field-descriptor
                             [:params :can-submit-multiple-applications]
                             true)
           haku-oid (get-in field-descriptor
                            [:params :haku-oid])
           preferred-name (:preferred-name answers-by-key)
           original-value (get-in answers-by-key [(keyword (:id field-descriptor)) :original-value])
           modifying? (some? original-value)]
       (asyncm/go
         (cond (not (email/email? value))
               [false []]
               (and (not (have-finnish-ssn? answers-by-key))
                    (not multiple?)
                    (not (and modifying? (= value original-value)))
                    (async/<! (has-applied haku-oid {:email value})))
               [false
                ((if modifying?
                   email-applied-error-when-modifying
                   email-applied-error) value (when (:valid preferred-name)
                                                (:value preferred-name)))]
               :else
               [true []]))))

(def ^:private postal-code-pattern #"^\d{5}$")

(defn ^:private postal-code?
  [value answers-by-key _]
  (if (residence-in-finland? answers-by-key)
    (and (not (nil? value))
         (not (nil? (re-matches postal-code-pattern value))))
    (not (clojure.string/blank? value))))

(def ^:private whitespace-pattern #"\s*")
(def ^:private phone-pattern #"^\+?\d{4,}$")
(def ^:private finnish-date-pattern #"^\d{1,2}\.\d{1,2}\.\d{4}$")

(defn ^:private phone?
  [value _ _]
  (if-not (nil? value)
    (let [parsed (clojure.string/replace value whitespace-pattern "")]
      (not (nil? (re-matches phone-pattern parsed))))
    false))

#?(:clj
   (def parse-date
     (let [formatter (f/formatter "dd.MM.YYYY" (c/time-zone-for-id "Europe/Helsinki"))]
       (fn [d]
         (try
           (f/parse formatter d)
           (catch Exception _ nil)))))
   :cljs
   (def parse-date
     (let [formatter (f/formatter "d.M.YYYY")]
       (fn [d]
         ;; Unfortunately cljs-time.format allows
         ;; garbage data (e.g. XXXXX11Y11Z1997BLAH),
         ;; so we have to protect against that with a regexp
         (if (re-matches finnish-date-pattern d)
           (try (f/parse formatter d)
                (catch :default _ nil))
           nil)))))

(defn ^:private date?
  [value _ _]
  (boolean
    (some->>
      value
      parse-date)))

(defn ^:private past-date?
  [value _ _]
  (boolean
    (and (date? value _ _)
         (some-> (parse-date value)
                 (c/before? (c/today-at-midnight))))))

(defn- postal-office?
  [value answers-by-key _]
  (if (residence-in-finland? answers-by-key)
    (not (clojure.string/blank? value))
    true))

(defn- main-first-name?
  [value answers-by-key _]
  (let [first-names     (clojure.string/split (-> answers-by-key :first-name :value) #"[\s-]+")
        num-first-names (count first-names)
        possible-names  (set
                          (for [sub-length (range 1 (inc num-first-names))
                                start-idx  (range 0 num-first-names)
                                :when (<= (+ sub-length start-idx) num-first-names)]
                            (clojure.string/join " " (subvec first-names start-idx (+ start-idx sub-length)))))]
    (contains? possible-names (clojure.string/replace value "-" " "))))

(defn- birthplace?
  [value answers-by-key _]
  (if (have-finnish-ssn? answers-by-key)
    (clojure.string/blank? value)
    (not (clojure.string/blank? value))))

(defn- home-town?
  [value answers-by-key _]
  (if (residence-in-finland? answers-by-key)
    (not (clojure.string/blank? value))
    true))

(defn- city?
  [value answers-by-key _]
  (if (residence-in-finland? answers-by-key)
    true
    (not (clojure.string/blank? value))))

(defn- parse-value
  [value]
  "Values in answers are a flat string collection when submitted, but a collection of maps beforehand (in front-end db) :("
  (cond
    (every? string? value) value
    (every? map? value) (map :value value)))

(defn- hakukohteet?
  [value _ field-descriptor]
  (let [hakukohde-options          (:options field-descriptor)
        num-answers                (count value)
        answers-subset-of-options? (clojure.set/subset? (set (parse-value value)) (set (map :value hakukohde-options)))]
    (if (pos? (count hakukohde-options))
      (if-let [max-hakukohteet (-> field-descriptor :params :max-hakukohteet)]
        (and (< 0 num-answers (inc max-hakukohteet)) answers-subset-of-options?)
        (and (pos? num-answers) answers-subset-of-options?))
      true)))

(def pure-validators {:required        required?
                      :postal-code     postal-code?
                      :postal-office   postal-office?
                      :phone           phone?
                      :past-date       past-date?
                      :main-first-name main-first-name?
                      :birthplace      birthplace?
                      :home-town       home-town?
                      :city            city?
                      :hakukohteet     hakukohteet?})

(def async-validators {:ssn ssn?
                       :email email?})

(defn validate
  [has-applied validator value answers-by-key field-descriptor]
  (if-let [pure-validator ((keyword validator) pure-validators)]
    (let [valid? (pure-validator value answers-by-key field-descriptor)]
      (asyncm/go [valid? []]))
    (if-let [async-validator ((keyword validator) async-validators)]
      (async-validator has-applied value answers-by-key field-descriptor)
      (asyncm/go [false []]))))
