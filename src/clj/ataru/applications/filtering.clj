(ns ataru.applications.filtering
  (:require [clojure.core.match :refer [match]]
            [clojure.set]
            [ataru.application.application-states :as application-states]
            [ataru.application.review-states :as review-states]))

(defn- filter-by-kk-payment-states
  [application states]
  (let [not-checked? (contains? states "not-checked")]
    (cond
      (nil? states)
      true

      (and not-checked? (nil? (:kk-payment-state application)))
      true

      :else
      (contains? states (:kk-payment-state application)))))

(defn- filter-by-attachment-review
  [application selected-hakukohteet states-to-include]
  (or (empty? (:hakukohde application))
      (let [states (->> (application-states/attachment-reviews-with-no-requirements application)
                        (filter #(or (empty? selected-hakukohteet) (contains? selected-hakukohteet (:hakukohde %))))
                        (map :state))]
        (not (empty? (clojure.set/intersection
                       states-to-include
                       (set states)))))))

(defn- parse-enabled-filters
  [filters kw]
  (->> (get filters kw)
       (filter second)
       (map first)
       (map name)
       (set)))

(defn- filter-by-ssn
  [application with-ssn? without-ssn?]
  (match [with-ssn? without-ssn?]
         [true true] true
         [false false] false
         [false true] (-> application :person :ssn (not))
         [true false] (-> application :person :ssn)))

(defn- filter-by-yksiloity
  [application identified? unidentified?]
  (match [identified? unidentified?]
         [true true] true
         [false false] false
         [false true] (-> application :person :yksiloity (not))
         [true false] (-> application :person :yksiloity)))

(defn- filter-by-active
  [application active? passive?]
  (match [active? passive?]
         [true true] true
         [false false] false
         [true false] (= (:state application) "active")
         [false true] (= (:state application) "inactivated")))

(defn- filter-by-hakukohde-review
  [application selected-hakukohteet requirement-name states-to-include]
  (let [all-states-count (-> review-states/hakukohde-review-types-map
                             (get (keyword requirement-name))
                             (last)
                             (count))
        selected-count   (count states-to-include)]
    (if (= all-states-count selected-count)
      true
      (let [relevant-states  (->> (:application-hakukohde-reviews application)
                                  (filter #(and (= requirement-name (:requirement %))
                                                (or (not selected-hakukohteet) (contains? selected-hakukohteet (:hakukohde %)))))
                                  (map :state)
                                  (set))]
        (not (empty? (clojure.set/intersection states-to-include relevant-states)))))))

(defn- filter-by-base-education
  [application base-education-filters]
  (let [selected-states    (->> base-education-filters
                                (filter (fn [[_ v]] (true? v)))
                                (map first)
                                (map name)
                                (set))
        application-states (-> application
                               :base-education
                               (set))]
    (not-empty (clojure.set/intersection selected-states application-states))))

(defn- filter-by-eligibility-set-automatically
  [application selected-hakukohteet-set yes? no?]
  (or (and yes? no?)
      (let [hakukohteet       (cond-> (set (:hakukohde application))
                                      (some? selected-hakukohteet-set)
                                      (clojure.set/intersection selected-hakukohteet-set))
            set-automatically (clojure.set/intersection
                               hakukohteet
                               (set (:eligibility-set-automatically application)))]
        (or (and yes? (not-empty set-automatically))
            (and no? (not= hakukohteet set-automatically))))))

(defn person-info-needed-to-filter?
  [filters]
  (not
    (and
      (-> filters :only-ssn :with-ssn)
      (-> filters :only-ssn :without-ssn)
      (-> filters :only-identified :identified)
      (-> filters :only-identified :unidentified))))

(defn filter-applications
  [applications {:keys [selected-hakukohteet attachment-states-to-include
                                         processing-states-to-include filters]}]
  (let [selected-hakukohteet-set         (when selected-hakukohteet (set selected-hakukohteet))
        applications-with-requirements   (map
                                           #(assoc % :application-hakukohde-reviews
                                                     (application-states/get-all-reviews-for-all-requirements %))
                                           applications)
        processing-states-to-include-set (set processing-states-to-include)
        attachment-states-to-include-set (set attachment-states-to-include)
        kk-payment-states-to-include-set (when (:kk-application-payment filters)
                                           (parse-enabled-filters filters :kk-application-payment))
        with-ssn?                        (-> filters :only-ssn :with-ssn)
        without-ssn?                     (-> filters :only-ssn :without-ssn)
        identified?                      (-> filters :only-identified :identified)
        unidentified?                    (-> filters :only-identified :unidentified)
        active?                          (-> filters :active-status :active)
        passive?                         (-> filters :active-status :passive)
        all-base-educations-enabled?     (->> (-> filters :base-education)
                                              vals
                                              (every? true?))]
    (filter
      (fn [application]
        (and
          (filter-by-ssn application with-ssn? without-ssn?)
          (filter-by-yksiloity application identified? unidentified?)
          (filter-by-active application active? passive?)
          (filter-by-kk-payment-states application kk-payment-states-to-include-set)
          (or
            all-base-educations-enabled?
            (filter-by-base-education application (:base-education filters)))
          (filter-by-hakukohde-review application selected-hakukohteet-set "processing-state" processing-states-to-include-set)
          (filter-by-hakukohde-review application selected-hakukohteet-set "language-requirement" (parse-enabled-filters filters :language-requirement))
          (filter-by-hakukohde-review application selected-hakukohteet-set "degree-requirement" (parse-enabled-filters filters :degree-requirement))
          (filter-by-hakukohde-review application selected-hakukohteet-set "eligibility-state" (parse-enabled-filters filters :eligibility-state))
          (filter-by-hakukohde-review application selected-hakukohteet-set "payment-obligation" (parse-enabled-filters filters :payment-obligation))
          (filter-by-attachment-review application selected-hakukohteet-set attachment-states-to-include-set)
          (filter-by-eligibility-set-automatically application
                                                   selected-hakukohteet-set
                                                   (get-in filters [:eligibility-set-automatically :yes])
                                                   (get-in filters [:eligibility-set-automatically :no]))))
      applications-with-requirements)))
