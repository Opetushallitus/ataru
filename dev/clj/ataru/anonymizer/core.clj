(ns ataru.anonymizer.core
  (:require [ataru.anonymizer.anonymizer-application-store :as application-store]
            [ataru.anonymizer.data :as data]
            [taoensso.timbre :as log]
            [ataru.anonymizer.ssn-generator :as ssn-gen]))

(def ssn-mappings (atom {}))

(defn- select-value [coll exclude-val]
  (loop [val (rand-nth coll)]
    (if-not (= val exclude-val)
      val
      (recur (rand-nth coll)))))

(defn- alter-old-gender-values [value]
  (case value
    "Nainen" 2
    "Mies" 1
    value))

(defn- extract-value [application key]
  (->> (get-in application [:content :answers])
       (filter (comp (partial = key) :key))
       (first)
       (:value)))

;; There was issues with production genders (empty strings)
(defn- safely-convert-to-int [gender-str]
  (try
    (Integer/valueOf gender-str)
    (catch Throwable t 2)))

(defn- extract-gender [application]
  (let [gender-sign (-> (extract-value application "gender")
                        (alter-old-gender-values)
                        (safely-convert-to-int))]
    (if (= (mod gender-sign 2) 0)
      :female
      :male)))

(defn- randomize-address [address]
  (let [street-name (first (clojure.string/split address #"\s+"))]
    (str (select-value data/street-names street-name) " "
         (rand-int 20) " "
         (rand-nth ["A" "B" "C" "D"]) " "
         (rand-int 40))))

(defn- randomize-first-name [gender exclude-val]
  (select-value (case gender
                  :male data/male-names
                  :female data/female-names)
                exclude-val))

(defn- randomize-phone-number [exclude-val]
  (loop [number nil]
    (if (and (some? number)
             (not= number exclude-val))
      number
      (recur (apply str "+35850" (take 7 (repeatedly #(rand-int 9))))))))

(defn ssn-for-answer [actual-ssn]
  (if-let [already-anonymized (get @ssn-mappings actual-ssn)]
    already-anonymized
    (let [new-anonymized-ssn (ssn-gen/generate-ssn)]
      (swap! ssn-mappings assoc actual-ssn new-anonymized-ssn)
      new-anonymized-ssn)))

(defn- anonymize [applications {:keys [key preferred_name last_name content] :as application}]
  (let [actual-ssn       (or (get-in applications [key :actual-ssn])
                             (extract-value application "ssn"))
        fake-ssn         (or (get-in applications [key :fake-ssn])
                             (when actual-ssn (ssn-for-answer actual-ssn)))
        gender           (or (get-in applications [key :gender])
                             (extract-gender application))
        first-name       (or (get-in applications [key :first-name])
                             (randomize-first-name gender preferred_name))
        last-name        (or (get-in applications [key :last-name])
                             (select-value data/last-names last_name))
        address          (or (get-in applications [key :address])
                             (randomize-address (extract-value application "address")))
        phone            (or (get-in applications [key :phone])
                             (randomize-phone-number (extract-value application "phone")))
        email            (or (get-in applications [key :email])
                             (str first-name "." last-name "@devnull.com"))
        postal-code      (or (get-in applications [key :postal-code])
                             (apply str (take 5 (repeatedly #(rand-int 9)))))
        anonymize-answer (fn [{:keys [key value] :as answer}]
                           (let [value (case key
                                         "first-name"     first-name
                                         "preferred-name" first-name
                                         "last-name"      last-name
                                         "address"        address
                                         "ssn"            fake-ssn
                                         "phone"          phone
                                         "email"          email
                                         "postal-code"    postal-code
                                         "postal-office"  "Helsinki"
                                         "home-town"      "Äkäslompolo"
                                         value)]
                             (assoc answer :value value)))
        content          (clojure.walk/prewalk (fn [x]
                                                 (cond-> x
                                                   (map? x)
                                                   (anonymize-answer)))
                                               content)
        application      (merge application {:preferred_name first-name
                                             :last_name      last-name
                                             :ssn            fake-ssn
                                             :content        content})]
    (cond-> (update applications :applications conj application)
      (not (contains? applications key))
      (update key merge {:gender      gender
                         :actual-ssn  actual-ssn
                         :fake-ssn    fake-ssn
                         :first-name  first-name
                         :last-name   last-name
                         :address     address
                         :phone       phone
                         :email       email
                         :postal-code postal-code}))))

(defn anonymize-data []
  (doseq [application (->> (application-store/get-all-applications)
                           (reduce anonymize {})
                           :applications)]
    (application-store/update-application application)
    (log/info (str "Anonymized application " (:id application) " with key " (:key application)))))
