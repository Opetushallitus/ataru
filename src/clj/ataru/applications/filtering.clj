(ns ataru.applications.filtering
  (:require [clojure.core.match :refer [match]]
            [ataru.application.application-states :as application-states]
            [ataru.application.review-states :as review-states]))

(defn- filter-by-attachment-review
  [application selected-hakukohteet states-to-include]
  (or (empty? (:hakukohde application))
      (let [states (->> (application-states/attachment-reviews-with-no-requirements application)
                        (filter #(or (nil? selected-hakukohteet) (contains? selected-hakukohteet (:hakukohde %))))
                        (map :state))]
        ;(println states-to-include "x" states "x" selected-hakukohteet "x" (application-states/attachment-reviews-with-no-requirements application))
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

(defn- state-filter
  [states states-to-include default-state-name hakukohteet]
  (or
    (not (empty? (clojure.set/intersection
                   states-to-include
                   (set states))))
    (and
      (contains? states-to-include default-state-name)
      (or
        (empty? states)
        (< (count states)
           (count hakukohteet))))))


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

(defn person-info-needed-to-filter?
  [filters]
  (not
    (and
      (-> filters :only-ssn :with-ssn)
      (-> filters :only-ssn :without-ssn)
      (-> filters :only-identified :identified)
      (-> filters :only-identified :unidentified))))

(defn filter-applications
  [applications {:keys [selected-hakukohteet attachment-states-to-include processing-states-to-include
                        selection-states-to-include filters]}]
  (let [selected-hakukohteet-set         (when selected-hakukohteet (set selected-hakukohteet))
        processing-states-to-include-set (set processing-states-to-include)
        selection-states-to-include-set  (set selection-states-to-include)
        attachment-states-to-include-set (set attachment-states-to-include)
        with-ssn?                        (-> filters :only-ssn :with-ssn)
        without-ssn?                     (-> filters :only-ssn :without-ssn)
        identified?                      (-> filters :only-identified :identified)
        unidentified?                    (-> filters :only-identified :unidentified)
        active?                          (-> filters :active-status :active)
        passive?                         (-> filters :active-status :passive)
        all-base-educations-enabled?     (->> (-> filters :base-education)
                                              (vals)
                                              (every? true?))]
    (filter
      (fn [application]
        (and
          (filter-by-ssn application with-ssn? without-ssn?)
          (filter-by-yksiloity application identified? unidentified?)
          (filter-by-active application active? passive?)
          (or
            all-base-educations-enabled?
            (filter-by-base-education application (:base-education filters)))
          (filter-by-hakukohde-review application selected-hakukohteet-set "processing-state" processing-states-to-include-set)
          (filter-by-hakukohde-review application selected-hakukohteet-set "selection-state" selection-states-to-include-set)
          (filter-by-hakukohde-review application selected-hakukohteet-set "language-requirement" (parse-enabled-filters filters :language-requirement))
          (filter-by-hakukohde-review application selected-hakukohteet-set "degree-requirement" (parse-enabled-filters filters :degree-requirement))
          (filter-by-hakukohde-review application selected-hakukohteet-set "eligibility-state" (parse-enabled-filters filters :eligibility-state))
          (filter-by-hakukohde-review application selected-hakukohteet-set "payment-obligation" (parse-enabled-filters filters :payment-obligation))
          (filter-by-attachment-review application selected-hakukohteet-set attachment-states-to-include-set)))
      applications)))
