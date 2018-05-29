(ns ataru.hakija.pohjakoulutusristiriita
  (:require [ataru.application-common.application-field-common :as common]
            [ataru.cljs-util :as cljs-util :refer [get-translation]]
            [ataru.util :as util]
            [re-frame.core :as re-frame]))

(defn hakukohteet-wo-applicable-base-education
  [db]
  (let [hakukohteet              (get-in db [:form :tarjonta :hakukohteet])
        selected-hakukohteet     (->> (get-in db [:application :answers :hakukohteet :values])
                                      (map :value)
                                      (map (fn [oid]
                                             (some #(when (= oid (:oid %)) %)
                                                   hakukohteet))))
        selected-base-educations (set (get-in db [:application :answers :higher-completed-base-education :value]))]
    (when-not (empty? selected-base-educations)
      (->> selected-hakukohteet
           (filter #(empty? (clojure.set/intersection
                             (set (:applicable-base-educations %))
                             selected-base-educations)))
           (map :oid)))))

(re-frame/reg-sub
  :application/hakukohteet-wo-applicable-base-education
  (fn [db _] (hakukohteet-wo-applicable-base-education db)))

(re-frame/reg-sub
  :application/applicable-base-educations
  (fn [db [_ oid]]
    (let [lang    (get-in db [:form :selected-language] :fi)
          options (->> (get-in db [:form :content])
                       util/flatten-form-fields
                       (filter #(= "higher-completed-base-education" (:id %)))
                       first
                       :options)]
      (->> (get-in db [:form :tarjonta :hakukohteet])
           (some #(when (= oid (:oid %)) %))
           :applicable-base-educations
           (map (fn [v]
                  (some #(when (= v (:value %))
                           (get-in % [:label lang]))
                        options)))))))

(defn pohjakoulutusristiriita [field-descriptor]
  [:div.application__wrapper-element
   [:div.application__wrapper-heading
    [:div.application__pohjakoulutusristiriita-alert
     [:i.zmdi.zmdi-alert-circle]]
    [common/scroll-to-anchor field-descriptor]]
   [:div.application__wrapper-contents
    [:div.application__form-field
     [:div.application__pohjakoulutusristiriita-text
      [common/markdown-paragraph
       (get-in field-descriptor [:text @(re-frame/subscribe [:application/form-language])])]]
     [:ul.application__pohjakoulutusristiriita-hakukohde-list
      (doall
       (for [oid @(re-frame/subscribe [:application/hakukohteet-wo-applicable-base-education])]
         ^{:key (str "hakukohde-" oid)}
         [:li.application__pohjakoulutusristiriita-hakukohde
          [:p.application__pohjakoulutusristiriita-hakukohde-label
           @(re-frame/subscribe [:application/hakukohde-label oid])]
          [:p.application__pohjakoulutusristiriita-hakukohde-description
           @(re-frame/subscribe [:application/hakukohde-description oid])]
          [:p.application__pohjakoulutusristiriita-hakukohde-applicable-base-education
           [:span.application__pohjakoulutusristiriita-hakukohde-applicable-base-education-label
            (get-translation :pohjakoulutusvaatimus) ": "]
           (clojure.string/join
            ", "
            @(re-frame/subscribe [:application/applicable-base-educations oid]))]]))]
     [:div.application__pohjakoulutusristiriita-hakukohteet-link
      [:a {:href "#scroll-to-hakukohteet"}
       (get-translation :muokkaa-hakukohteita)]]]]])
