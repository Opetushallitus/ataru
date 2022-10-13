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

(defn update-options-while-keeping-existing-followups [newest-options existing-options]
  (let [options (if (empty? existing-options)
                  newest-options
                  (let [existing-values (set (map :value existing-options))
                        options-that-didnt-exist-before (filter #(not (contains? existing-values (:value %))) newest-options)]
                    (vec (concat options-that-didnt-exist-before
                                 (map (fn [existing-option]
                                        (if-let [new-option (first (filter #(= (:value %) (:value existing-option)) newest-options))]
                                          (assoc existing-option :label (:label new-option))
                                          existing-option))
                                      existing-options)))))
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
