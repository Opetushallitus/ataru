
(ns ataru.virkailija.application.excel-download.excel-handlers
  (:require [ajax.core]
            [ajax.protocols :as pr]
            [ataru.cljs-util :as cljs-util]
            [ataru.excel-common :refer [assoc-in-excel get-in-excel]]
            [ataru.util :as util :refer [assoc?]]
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
         parent-id (:parent-id filter)
         parent-filter (get-in-excel db [:filters (:parent-id filter)])
         child-ids (:child-ids filter)
         new-checked (not (:checked filter))]
     (as-> db db-result
       (assoc-in-excel db-result [:filters id :checked] new-checked)
       ; if checked, then select all children
       (loop [cids child-ids acc db-result]
         (if (not-empty cids)
           (recur (rest cids) (assoc-in-excel acc [:filters (first cids) :checked] new-checked))
           acc))
       (if parent-filter (let [sibling-checkeds (map
                                                 (fn [sibling-id] (boolean (get-in-excel db-result [:filters sibling-id :checked])))
                                                 (:child-ids parent-filter))]
                           (cond (every? true? sibling-checkeds) (assoc-in-excel db-result [:filters parent-id :checked] true)
                                 :else (assoc-in-excel db-result [:filters parent-id :checked] false)))
           db-result)))))

(reg-event-db
 :application/excel-request-filters-set-all
 (fn [db [_ checked]]
   (let [ids (map (fn [[_ v]] (:id v)) (get-in-excel db [:filters]))]
     (loop [rest-ids ids acc db]
       (if (not-empty rest-ids)
         (recur (rest rest-ids) (assoc-in-excel acc [:filters (first rest-ids) :checked] (boolean checked)))
         acc)))))

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

(reg-event-fx
 :application/start-excel-download
 (fn [{:keys [db]} [_ params]]
   (when (not (get-in-excel db :fetching?))
     (let [application-keys (map :key (get-in db [:application :applications]))
           selected-mode (get-in-excel db :selected-mode)
           written-ids (as-> (get-in-excel db :included-ids) $
                         (clj-string/split $ #"\s+")
                         (remove clj-string/blank? $)
                         (not-empty $))
           filtered-ids (->> (get-in-excel db :filters)
                             (vals)
                             (filter :checked)
                             (filter #(empty? (:child-ids %))) ;Sisällytetään vain "lehti"-id:t, koska ylemmät tasot on ryhmittelyä
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
                                                       :read (fn [response] {:headers (ajax.protocols/-get-all-headers response)
                                                                             :body (ajax.protocols/-body response)})}
                                     :error-handler #(do (dispatch [:add-toast-message (str "Excelin muodostaminen epäonnistui, status: " (:status %))])
                                                         (dispatch [:application/handle-excel-download-error %]))}}}))))

(reg-event-db
 :application/excel-request-filters-init
 (fn [db [_ filter-defs]]
   (let [old-filters (get-in-excel db :filters)]
     (assoc-in-excel db :filters (merge-with merge old-filters filter-defs)))))

(reg-event-db
 :application/excel-request-toggle-accordion-open
 (fn [db [_ id]]
   (update-in db [:application :excel-request :filters id :open?] not)))