(ns ataru.virkailija.application.application-list.virkailija-application-list-handlers
  (:require [ataru.cljs-util :as cljs-util]
            [ataru.virkailija.db :as initial-db]
            [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
  :application/toggle-filter
  (fn [db [_ filter-id state]]
    (update-in db [:application :filters-checkboxes filter-id state] not)))

(reg-event-fx
  :application/apply-filters
  (fn [{:keys [db]} _]
    {:db       (-> db
                   (assoc-in [:application :filters] (get-in db [:application :filters-checkboxes]))
                   (assoc-in [:application :school-filter] (get-in db [:application :school-filter-pending-value]))
                   (assoc-in [:application :classes-of-school] (get-in db [:application :classes-of-school-pending-value]))
                   (assoc-in [:application :ensisijaisesti?] (get-in db [:application :ensisijaisesti?-checkbox]))
                   (assoc-in [:application :rajaus-hakukohteella] (get-in db [:application :rajaus-hakukohteella-value])))
     :dispatch [:application/reload-applications true]}))

(reg-event-fx
  :application/remove-filters
  (fn [{:keys [db]} _]
    {:db       (-> db
                   (assoc-in [:application :filters] initial-db/default-filters)
                   (assoc-in [:application :filters-checkboxes] initial-db/default-filters)
                   (assoc-in [:application :school-filter] nil)
                   (assoc-in [:application :school-filter-pending-value] nil)
                   (assoc-in [:application :classes-of-school] nil)
                   (assoc-in [:application :classes-of-school-pending-value] nil)
                   (assoc-in [:application :ensisijaisesti?] false)
                   (assoc-in [:application :ensisijaisesti?-checkbox] false)
                   (assoc-in [:application :rajaus-hakukohteella] nil)
                   (assoc-in [:application :rajaus-hakukohteella-value] nil))
     :dispatch [:application/reload-applications]}))

(defn- set-rajaus-hakukohteella
  [db hakukohde-oid]
  (cljs-util/update-url-with-query-params {:rajaus-hakukohteella hakukohde-oid})
  (assoc-in db [:application :rajaus-hakukohteella-value] hakukohde-oid))

(defn- set-ensisijaisesti
  [db ensisijaisesti?]
  (cljs-util/update-url-with-query-params {:ensisijaisesti ensisijaisesti?})
  (cond-> (assoc-in db [:application :ensisijaisesti?-checkbox] ensisijaisesti?)
          (not ensisijaisesti?)
          (set-rajaus-hakukohteella nil)))

(reg-event-db
  :application/set-ensisijaisesti
  (fn [db [_ ensisijaisesti?]] (set-ensisijaisesti db ensisijaisesti?)))

(reg-event-db
  :application/set-rajaus-hakukohteella
  (fn [db [_ hakukohde-oid]] (set-rajaus-hakukohteella db hakukohde-oid)))

(defn undo-filters
  [db]
  (-> db
      (assoc-in [:application :filters-checkboxes] (get-in db [:application :filters]))
      (assoc-in [:application :school-filter-pending-value] (get-in db [:application :school-filter]))
      (set-ensisijaisesti (get-in db [:application :ensisijaisesti?]))
      (set-rajaus-hakukohteella (get-in db [:application :rajaus-hakukohteella]))
      (assoc-in [:application :classes-of-school-pending-value] (get-in db [:application :classes-of-school]))))

(reg-event-db
  :application/undo-filters
  (fn [db _] (undo-filters db)))

(reg-event-db
  :application/toggle-shown-time-column
  (fn [db _]
    (update-in db [:application :selected-time-column] #(if (= "created-time" %)
                                                          "submitted"
                                                          "created-time"))))

(reg-event-fx
  :application/set-school-filter
  (fn [{:keys [db]} [_ oid]]
    (letfn
      [(set-school-filter-pending-value [db]
         (assoc-in db [:application :school-filter-pending-value] oid))
       (set-classes-of-school-pending-value [db]
         (if (= 1 (count (get-in db [:editor :organizations :schools-of-departure])))
           db
           (assoc-in db [:application :classes-of-school-pending-value] nil)))]
      {:db       (-> db
                   set-school-filter-pending-value
                   set-classes-of-school-pending-value)
       :dispatch [:application/fetch-classes-of-school oid]})))

(reg-event-db
  :application/remove-selected-school-pending
  (fn [db _]
    (-> db
      (assoc-in [:application :school-filter-pending-value] nil)
      (assoc-in [:application :classes-of-school-pending-value] nil))))

(reg-event-fx
  :application/update-sort
  (fn [{:keys [db]} [_ column-id]]
    {:db       (update-in db [:application :sort]
                          #(if (= column-id (:order-by %))
                             (update % :order {"desc" "asc" "asc" "desc"})
                             (assoc % :order-by column-id)))
     :dispatch [:application/reload-applications]}))

(defn- init-question-answer-filtering-options [form field]
  (let [use-original-question? (boolean (:per-hakukohde field))
        use-original-followup? (boolean
                                 (and (not use-original-question?)
                                      (:followup-of field)
                                      (:per-hakukohde (get-in form [:form-fields-by-id (keyword (:followup-of field))]))))
        base-answer {:use-original-question use-original-question?
                     :use-original-followup use-original-followup?}
        options     (->> (:options field)
                         (map :value)
                         (mapv (fn [v] [v false]))
                         (into {}))]
       (merge base-answer {:options options})))

(reg-event-db
  :application/add-question-filter
  (fn [db [_ form-key field]]
    (let [field-id (:id field)]
      (if (= (:fieldType field) "attachment")
        (assoc-in db [:application :filters-checkboxes :attachment-review-states field-id] initial-db/default-attachment-review-states)
        (assoc-in db [:application :filters-checkboxes :question-answer-filtering-options field-id]
                  (init-question-answer-filtering-options (get-in db [:forms form-key]) field))))))

(reg-event-db
  :application/remove-question-filter
  (fn [db [_ field]]
    (let [field-id (:id field)]
      (if (= (:fieldType field) "attachment")
        (update-in db [:application :filters-checkboxes :attachment-review-states] dissoc field-id)
        (update-in db [:application :filters-checkboxes :question-answer-filtering-options] dissoc field-id)))))

(reg-event-db
  :application/set-filter-attachment-state
  (fn [db [_ field-id state value]]
    (assoc-in db [:application :filters-checkboxes :attachment-review-states field-id state] value)))

(reg-event-db
  :application/set-question-answer-filtering-options
  (fn [db [_ field-id option value]]
    (assoc-in db [:application :filters-checkboxes :question-answer-filtering-options field-id :options option] value)))

(reg-event-db
  :application/set-pending-classes-of-school
  (fn [db [_ luokka checked]]
    (let [path [:application :classes-of-school-pending-value]
          classes (get-in db path)
          new-classes (if checked
                        (conj classes luokka)
                        (remove #(= luokka %) classes))]
      (assoc-in db path (vec new-classes)))))
