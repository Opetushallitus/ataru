(ns ataru.applications.attachment-review-synchroniser
  (:require [ataru.applications.application-store :as application-store]
            [ataru.component-data.kk-application-payment-module :as payment-module]
            [taoensso.timbre :as log]))

(defn- inheritable-attachment-key?
  [attachment-key]
  (contains? payment-module/sync-triggering-kk-application-payment-attachment-keys attachment-key))

(defn- attachment-reviews-by-key-and-hakukohde
  [existing-reviews]
  (reduce
    (fn [acc {:keys [hakukohde attachment-key state]}]
      (assoc-in acc [attachment-key hakukohde] state))
    {}
    existing-reviews))

(defn- get-attachment-review-changes
  [existing-by-key-and-hakukohde attachment-reviews]
  (let [actually-changed (->> attachment-reviews
                              (mapcat (fn [[hakukohde review]]
                                        (map (fn [[attachment-key state]]
                                               {:hakukohde      (name hakukohde)
                                                :attachment-key (name attachment-key)
                                                :state          state})
                                             review)))
                              (filter #(inheritable-attachment-key? (:attachment-key %)))
                              (remove #(= (:state %)
                                          (get-in existing-by-key-and-hakukohde
                                                  [(:attachment-key %) (:hakukohde %)]))))]
    (->> actually-changed
         (group-by :attachment-key)
         (keep (fn [[attachment-key changes]]
                 (let [distinct-values (distinct (map :state changes))]
                   (if (not= 1 (count distinct-values))
                     (log/warn "Skipping attachment review sync for" attachment-key
                               "- conflicting simultaneous values in the same request:" changes)
                     (let [new-state-value (first distinct-values)
                           hakukohde-oids-in-edit (set (map :hakukohde changes))
                           existing-hakukohteet (set (keys (get existing-by-key-and-hakukohde attachment-key)))
                           applicable-hakukohteet (into hakukohde-oids-in-edit existing-hakukohteet)
                           other-hakukohteet (remove hakukohde-oids-in-edit applicable-hakukohteet)]
                       (when (seq other-hakukohteet)
                         {:attachment-key    attachment-key
                          :new-state-value   new-state-value
                          :other-hakukohteet other-hakukohteet})))))))))

(defn- sync-attachment-reviews
  [application-key attachment-reviews changes]
  (log/info "Synchronizing attachment review states" changes "in application" application-key)
  (reduce
    (fn [reviews {:keys [attachment-key new-state-value other-hakukohteet]}]
      (reduce
        (fn [reviews hakukohde]
          (assoc-in reviews [(keyword hakukohde) (keyword attachment-key)] new-state-value))
        reviews
        other-hakukohteet))
    attachment-reviews
    changes))

(defn save-attachment-hakukohde-reviews
  [application-key attachment-reviews session audit-logger]
  (let [existing-by-key-and-hakukohde (attachment-reviews-by-key-and-hakukohde
                                        (application-store/get-application-attachment-reviews application-key))
        changes (get-attachment-review-changes existing-by-key-and-hakukohde attachment-reviews)
        reviews-to-save (if (seq changes)
                          (sync-attachment-reviews application-key attachment-reviews changes)
                          attachment-reviews)]
    (doseq [[hakukohde review] reviews-to-save
            [attachment-key review-state] review
            :let [hakukohde-str (name hakukohde)
                  attachment-key-str (name attachment-key)]
            :when (not= review-state
                        (get-in existing-by-key-and-hakukohde [attachment-key-str hakukohde-str]))]
      (application-store/save-attachment-hakukohde-review
        application-key
        hakukohde-str
        attachment-key-str
        review-state
        session
        audit-logger))
    (boolean (seq changes))))
