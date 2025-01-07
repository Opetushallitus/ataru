
(ns ataru.virkailija.application.excel-download.excel-handlers
  (:require [ajax.core]
            [ajax.protocols :as pr]
            [ataru.cljs-util :as cljs-util]
            [ataru.excel-common :refer [common-field-labels
                                        form-field-belongs-to-hakukohde]]
            [ataru.util :as util :refer [assoc?]]
            [ataru.virkailija.application.excel-download.excel-utils :refer [assoc-in-excel
                                                                             download-blob
                                                                             get-excel-checkbox-filter-defs
                                                                             get-in-excel
                                                                             get-values-for-child-filters]]
            [ataru.virkailija.application.mass-review.virkailija-mass-review-handlers]
            [clojure.string :as clj-string]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]))

(reg-event-db
 :application/set-excel-popup-visibility
 (fn [db [_ visible?]]
   (assoc-in-excel db :visible? visible?)))

(reg-event-db
 :application/change-excel-download-mode
 (fn [db [_ selected-mode]]
   (assoc-in-excel db :selected-mode selected-mode)))

(reg-event-db
 :application/excel-request-filter-changed
 (fn [db [_ id]]
   (let [filter (get-in-excel db [:filters id])
         child-ids (:child-ids filter)
         new-checked (not (:checked filter))
         parent-id (:parent-id filter)]
     (as-> db $
       (assoc-in-excel $ [:filters id :checked] new-checked)
       ; set values for all children
       (loop [cids child-ids acc $]
         (if (not-empty cids)
           (recur (rest cids) (assoc-in-excel acc [:filters (first cids) :checked] new-checked))
           acc))
       ; set parent value according to changed children
       (if parent-id
         (cond (every? true? (get-values-for-child-filters $ parent-id)) (assoc-in-excel $ [:filters parent-id :checked] true)
               :else (assoc-in-excel $ [:filters parent-id :checked] false))
         $)))))

(reg-event-db
 :application/excel-request-filters-set-all
 (fn [db [_ checked]]
   (let [ids (map (fn [[_ v]] (:id v)) (get-in-excel db [:filters]))]
     (loop [rest-ids ids acc db]
       (if (not-empty rest-ids)
         (recur (rest rest-ids) (assoc-in-excel acc [:filters (first rest-ids) :checked] (boolean checked)))
         acc)))))

(reg-event-db
 :application/handle-excel-download-success
 (fn [db [_ response]]
   (let [new-db (assoc-in-excel db :fetching? false)
         filename (-> response
                      :headers
                      (get "content-disposition")
                      (clj-string/split #"filename=")
                      last)]
     (try (download-blob filename (:body response))
          (assoc-in-excel new-db :error nil)
          (catch js/Error e (assoc-in-excel new-db :error e))))))

(reg-event-db
 :application/handle-excel-download-error
 (fn [db [_ error]]
   (-> db
       (assoc-in-excel :fetching? false)
       (assoc-in-excel :error error))))

(defn- get-included-ids [db]
  (let [selected-mode (get-in-excel db :selected-mode)]
    (case selected-mode
      "with-defaults" (as-> (get-in-excel db :included-ids) $
                        (clj-string/split $ #"\s+")
                        (remove clj-string/blank? $)
                        (not-empty $))
      "ids-only" (->> (get-in-excel db :filters)
                      (vals)
                      (filter :checked)
                      (filter #(empty? (:child-ids %))) ;Sisällytetään vain "lehti"-id:t, koska ylemmät tasot on ryhmittelyä
                      (map :id))
      :else nil)))

(reg-event-fx
 :application/start-excel-download
 (fn [{:keys [db]} [_ params]]
   (when (not (get-in-excel db :fetching?))
     (let [application-keys (map :key (get-in db [:application :applications]))
           selected-mode (get-in-excel db :selected-mode)
           applications-sort (get-in db [:application :sort])]
       {:db   (-> db
                  (assoc-in-excel :error nil)
                  (assoc-in-excel :fetching? true))
        :http {:id                  :excel-download
               :method              :post
               :path                "/lomake-editori/api/applications/excel"
               :params              (-> params
                                        (assoc? :application-keys application-keys)
                                        (assoc? :CSRF (cljs-util/csrf-token))
                                        (assoc? :included-ids (get-included-ids db))
                                        (assoc? :export-mode selected-mode)
                                        (assoc? :sort-by-field (:order-by applications-sort))
                                        (assoc? :sort-order (:order applications-sort)))
               :skip-parse-times?   true
               :skip-flasher?       true
               :handler-or-dispatch :application/handle-excel-download-success
               :override-args       {:response-format {:type :blob
                                                       :read (fn [response] {:headers (ajax.protocols/-get-all-headers response)
                                                                             :body (ajax.protocols/-body response)})}
                                     :error-handler #(do (dispatch [:add-toast-message (str "Excelin muodostaminen epäonnistui, status: " (:status %))])
                                                         (dispatch [:application/handle-excel-download-error %]))}}}))))

(reg-event-db
 :application/excel-request-filters-init
 (fn [db [_ selected-form-key selected-hakukohde selected-hakukohderyhma]]
   (let [form-content (get-in db [:forms selected-form-key :content])
         form-properties (get-in db [:forms selected-form-key :properties])
         all-hakukohteet (get-in db [:hakukohteet])
         form-field-belongs-to (fn [form-field] (form-field-belongs-to-hakukohde form-field selected-hakukohde selected-hakukohderyhma (delay all-hakukohteet)))
         new-filters-init-params {:selected-form-key selected-form-key
                                  :selected-hakukohde selected-hakukohde
                                  :selected-hakukohderyhma selected-hakukohderyhma}]
     (-> db
         (assoc-in-excel :filters (get-excel-checkbox-filter-defs
                                   (concat common-field-labels form-content)
                                   form-field-belongs-to
                                   form-properties
                                   ))
         (assoc-in-excel :filters-init-params new-filters-init-params)))))

(reg-event-db
 :application/excel-request-toggle-accordion-open
 (fn [db [_ id]]
   (update-in db [:application :excel-request :filters id :open?] not)))