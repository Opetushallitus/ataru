(ns ataru.virkailija.application.view.virkailija-application-version-history
  (:require [ataru.util :as util]
            [ataru.virkailija.temporal :as temporal]
            [ataru.virkailija.views.modal :as modal]
            [goog.string :as gstring]
            [re-frame.core :refer [subscribe dispatch]]))

(defn- application-version-history-header [changes-amount]
  (let [event (subscribe [:application/selected-event])]
    (fn []
      (let [changed-by (if (= (:event-type @event) "updated-by-applicant")
                         (.toLowerCase @(subscribe [:editor/virkailija-translation :applicant]))
                         (str (:first-name @event) " " (:last-name @event)))]
        [:div.application-handling__version-history-header
         [:div.application-handling__version-history-header-text
          (str @(subscribe [:editor/virkailija-translation :diff-from-changes])
               " "
               (temporal/time->short-str (or (:time @event) (:created-time @event))))]
         [:div.application-handling__version-history-header-sub-text
          [:span.application-handling__version-history-header-virkailija
           changed-by]
          [:span
           (gstring/format " %s %s %s"
                           @(subscribe [:editor/virkailija-translation :changed])
                           changes-amount
                           @(subscribe [:editor/virkailija-translation :answers]))]]]))))

(defn- application-version-history-list-value [values]
  [:ol.application-handling__version-history-list-value
   (map-indexed
     (fn [index value]
       ^{:key index}
       [:li.application-handling__version-history-list-value-item
        value])
     values)])

(defn- application-version-history-value [value-or-values]
  (cond (util/is-question-group-answer? value-or-values)
        [:ol.application-handling__version-history-question-group-value
         (map-indexed
           (fn [index values]
             ^{:key index}
             [:li.application-handling__version-history-question-group-value-item
              (application-version-history-list-value values)])
           value-or-values)]

        (vector? value-or-values)
        (application-version-history-list-value value-or-values)

        :else
        [:span (str value-or-values)]))

(defn- application-version-history-sub-row
  [left right]
  [:div.application-handling__version-history-sub-row
   [:div.application-handling__version-history-sub-row__left
    left]
   [:div.application-handling__version-history-sub-row__right
    right]])

(defn- breadcrumb-label
  [history-item]
  [:span.application-handling__version-history-row-breadcrumb
   (->> (:label history-item)
        (map-indexed (fn [i [label value]]
                       ^{:key (str "breadcrumb-" i "-" label)}
                       [^{:key (str "breadcrumb-" i "-" label "-label")}
                        [:span.application-handling__version-history-row-breadcrumb-label
                         label ": "]
                        (when (some? value)
                          ^{:key (str "breadcrumb-" i "-" label "-value")}
                          [:span.application-handling__version-history-row-breadcrumb-value
                           "\"" value "\" > "])]))
        (mapcat identity))])

(defn- application-version-history-row [key history-item]
  ^{:key (str "application-change-history-" key)}
  [:div.application-handling__version-history-row
   [application-version-history-sub-row
    nil
    (breadcrumb-label history-item)]
   [application-version-history-sub-row
    [:span.application-handling__version-history-value-label
     @(subscribe [:editor/virkailija-translation :diff-removed])]
    [:div.application-handling__version-history-value.application-handling__version-history-value__old
     (application-version-history-value (:old history-item))]]
   [application-version-history-sub-row
    [:span.application-handling__version-history-value-label
     @(subscribe [:editor/virkailija-translation :diff-added])]
    [:div.application-handling__version-history-value.application-handling__version-history-value__new
     (application-version-history-value (:new history-item))]]])

(defn application-version-changes []
  (let [history-items (subscribe [:application/current-history-items])]
    (when @history-items
      [modal/modal
       #(dispatch [:application/close-application-version-history])
       [:div.application-handling__version-history
        [application-version-history-header (count @history-items)]
        (for [[key item] @history-items]
          ^{:key (str "application-history-row-for-" key)}
          [application-version-history-row key item])]])))

