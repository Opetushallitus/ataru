
(ns ataru.virkailija.application.excel-download.excel-handlers
  (:require [ajax.protocols :as pr]
            [ataru.cljs-util :as cljs-util]
            [ataru.util :as util :refer [assoc?]]
            [ataru.virkailija.application.mass-review.virkailija-mass-review-handlers]
            [clojure.string :as clj-string]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]))
(defn- assoc-in-excel [db k v]
  (assoc-in db (concat [:application :excel-request] (if (vector? k) k [k])) v))

(reg-event-db
 :application/set-excel-popup-visibility
 (fn [db [_ visible?]]
   (assoc-in-excel db :visible? visible?)))

(reg-event-db
 :application/excel-request-filter-changed
 (fn [db [_ id]]
   (let [filter (get-in db [:application :excel-request :filters id])
         parent-id (:parent-id filter)
         parent-filter (get-in db [:application :excel-request :filters (:parent-id filter)])
         child-ids (:child-ids filter)
         new-checked (not (:checked filter))]
     (as-> db x
       (assoc-in-excel x [:filters id :checked] new-checked)
       ; if checked, then select all children
       (loop [cids child-ids acc x]
         (if (not-empty cids)
           (recur (rest cids) (assoc-in-excel acc [:filters (first cids) :checked] new-checked))
           acc))
       (if parent-filter (let [sibling-checkeds (map (fn [sibling-id] (boolean (get-in x [:application :excel-request :filters sibling-id :checked]))) (:child-ids parent-filter))]
                           (cond (every? true? sibling-checkeds) (assoc-in-excel x [:filters parent-id :checked] true)
                                 (every? false? sibling-checkeds) (assoc-in-excel x [:filters parent-id :checked] false)
                                 :else (assoc-in-excel x [:filters parent-id :checked] false)))
           x)))))

(reg-event-db
 :application/excel-request-filters-set-all
 (fn [db [_ checked]]
   (let [ids (map (fn [[_ v]] (:id v)) (get-in db [:application :excel-request :filters]))]
     (loop [rest-ids ids acc db]
       (if (not-empty rest-ids)
         (recur (rest rest-ids) (assoc-in-excel acc [:filters (first rest-ids) :checked] (boolean checked)))
         acc)))))

(reg-event-db
 :application/change-excel-download-mode
 (fn [db [_ selected-mode]]
   (assoc-in-excel db :selected-mode selected-mode)))

(defn download-blob [file-name blob]
  (let [object-url (js/URL.createObjectURL blob)
        anchor-element
        (doto (js/document.createElement "a")
          (-> .-href (set! object-url))
          (-> .-download (set! file-name)))]
    (.appendChild (.-body js/document) anchor-element)
    (.click anchor-element)
    (try
      (.removeChild (.-body js/document) anchor-element)
      (js/URL.revokeObjectURL object-url)
      (catch js/Error e (println e)))))

(reg-event-db
 :application/handle-excel-download-success
 (fn [db [_ response {filename :filename}]]
   (let [new-db (assoc-in-excel db :fetching? false)]
     (try (download-blob filename response)
          (assoc-in-excel new-db :error nil)
          (catch js/Error e (assoc-in-excel new-db :error e))))))

(reg-event-db
 :application/handle-excel-download-error
 (fn [db [_ error]]
   (-> db
       (assoc-in-excel :fetching? false)
       (assoc-in-excel :error error))))

(reg-event-fx
 :application/start-excel-download
 (fn [{:keys [db]} [_ params]]
   (when (not (get-in db [:application :excel-request :fetching?]))
     (let [application-keys (map :key (get-in db [:application :applications]))
           selected-mode (get-in db [:application :excel-request :selected-mode])
           written-ids (clj-string/split #"\s+" (get-in db [:application :excel-request :included-ids]))
           filtered-ids (->> (get-in db [:application :excel-request :filters])
                             (map second)
                             (filter :checked)
                             (map :id))]
       {:db   (-> db
                  (assoc-in-excel :error nil)
                  (assoc-in-excel :fetching? true))
        :http {:id                  :excel-download
               :method              :post
               :path                "/lomake-editori/api/applications/excel"
               :params              (-> params
                                        (assoc? :application-keys application-keys)
                                        (assoc? :CSRF (cljs-util/csrf-token))
                                        (assoc? :included-ids (->> (case selected-mode
                                                                     "kirjoita-tunnisteet" written-ids
                                                                     "valitse-tiedot" filtered-ids
                                                                     :else "")))
                                        (assoc? :include-default-columns (= selected-mode "kirjoita-tunnisteet")))
               :skip-parse-times?   true
               :skip-flasher?       true
               :handler-or-dispatch :application/handle-excel-download-success
               :handler-args        {:filename (:filename params)}
               :override-args       {:response-format {:type :blob
                                                       :read pr/-body}
                                     :error-handler #(do (dispatch [:add-toast-message (str "Excelin muodostaminen epäonnistui, status: " (:status %))])
                                                         (dispatch [:application/handle-excel-download-error %]))}}}))))

(reg-event-db
 :application/excel-request-filters-init
 (fn [db [_ filter-defs]]
   (let [old-filters (get-in db [:application :excel-request :filters])]
     (assoc-in db [:application :excel-request :filters] (merge-with merge old-filters filter-defs)))))