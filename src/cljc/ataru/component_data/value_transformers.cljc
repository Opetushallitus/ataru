(ns ataru.component-data.value-transformers
  #?(:cljs (:require [goog.string :as gstring])))

(def ^:private dob-pattern #"^(\d{1,2})\.(\d{1,2})\.(\d{4})$")

(defn birth-date [dob]
  (when-let [[_ day month year] (re-matches dob-pattern dob)]
    (let [f #?(:clj format :cljs gstring/format)]
      (f "%02d.%02d.%d" #?@(:clj  [(Integer/valueOf day)
                                   (Integer/valueOf month)
                                   (Integer/valueOf year)]
                            :cljs [day month year])))))

(def ^:private cas-oppija-dob-pattern #"^(\d{4})-(\d{1,2})-(\d{1,2})$")

;1981-02-04 to 04.02.1981
(defn cas-oppija-dob-to-ataru-dob [dob]
  (when-let [[_ year month day] (re-matches cas-oppija-dob-pattern dob)]
    (let [f #?(:clj format :cljs gstring/format)]
      (f "%02d.%02d.%d" #?@(:clj  [(Integer/valueOf day)
                                   (Integer/valueOf month)
                                   (Integer/valueOf year)]
                            :cljs [day month year])))))

(defn merge-options-keeping-existing
  [newest-options existing-options]
  (let [existing-values (set (map :value existing-options))
        options-that-didnt-exist-before (filter #(not (contains? existing-values (:value %))) newest-options)]
    (vec (concat options-that-didnt-exist-before
                 (map (fn [existing-option]
                        (if-let [new-option (first (filter #(= (:value %) (:value existing-option)) newest-options))]
                          (assoc existing-option :label (:label new-option))
                          existing-option))
                      existing-options)))))

; Returns the set of new options with possible old followups added.
(defn merge-options-removing-existing
  [newest-options existing-options]
  (vec (map (fn [new-option]
              (let [existing-option (first (filter #(= (:value %) (:value new-option)) existing-options))]
                (if (and existing-option (:followups existing-option))
                  (assoc new-option :followups (:followups existing-option))
                  new-option)))
            newest-options)))

(defn update-options-while-keeping-existing-followups
  ([newest-options existing-options remove-existing]
   (let [options (if (empty? existing-options)
                   newest-options
                   (if remove-existing
                     (merge-options-removing-existing newest-options existing-options)
                     (merge-options-keeping-existing newest-options existing-options)))
         identical (fn [{:keys [value]}]
                     (keep-indexed (fn [index option]
                                     (when (= value (:value option))
                                       [index option]))
                                   options))
         remove-hidden? (fn [{:keys [hidden] :as koodi}]
                          (if (or (nil? koodi) hidden)
                            true
                            false))]
     (vec (remove remove-hidden? (map-indexed (fn [index option]
                                                (let [last-option (first (last (identical option)))]
                                                  (when (= index last-option)
                                                    option)))
                                              options)))))
  ([newest-options existing-options]
   (update-options-while-keeping-existing-followups newest-options existing-options false)))
