(ns ataru.anonymizer.core
  (:require [ataru.anonymizer.anonymizer-application-store :as application-store]
            [ataru.anonymizer.data :as data]
            [oph.soresu.common.db :as db]
            [taoensso.timbre :as log]))

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

(defn- extract-gender [application]
  (let [gender-sign (-> (extract-value application "gender")
                        (alter-old-gender-values)
                        (Integer/valueOf))]
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

(defn- anonymize [applications {:keys [key preferred_name last_name content] :as application}]
  (let [gender           (or (get-in applications [key :gender])
                             (extract-gender application))
        first-name       (or (get-in applications [key :first-name])
                             (randomize-first-name gender preferred_name))
        last-name        (or (get-in applications [key :last-name])
                             (select-value data/last-names last_name))
        address          (or (get-in applications [key :address])
                             (randomize-address (extract-value application "address")))
        phone            (or (get-in application [key :phone])
                             (randomize-phone-number (extract-value application "phone")))
        email            (or (get-in application [key :email])
                             (str first-name "." last-name "@devnull.com"))
        anonymize-answer (fn [{:keys [key value] :as answer}]
                           (let [value (case key
                                         "first-name" first-name
                                         "preferred-name" first-name
                                         "last-name" last-name
                                         "address" address
                                         "ssn" (case gender
                                                 :male "020202A0213"
                                                 :female "020202A0202")
                                         "phone" phone
                                         "email" email
                                         value)]
                             (assoc answer :value value)))
        content          (clojure.walk/prewalk (fn [x]
                                                 (cond-> x
                                                   (map? x)
                                                   (anonymize-answer)))
                                               content)
        application      (merge application {:preferred_name first-name
                                             :last_name      last-name
                                             :content        content})]
    (cond-> (update applications :applications conj application)
      (not (contains? applications key))
      (update key merge {:gender     gender
                         :first-name first-name
                         :last-name  last-name
                         :address    address
                         :phone      phone
                         :email      email}))))

(defn anonymize-data []
  (doseq [application (->> (application-store/get-all-applications)
                           (reduce anonymize {})
                           :applications)]
    (application-store/update-application application)
    (log/info (str "Anonymized application " (:id application) " with key " (:key application)))))
