(ns ataru.hakija.components.question-hakukohde-names-component
  (:require [ataru.translations.translation-util :as tu]
            [ataru.util :as util]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defn question-hakukohde-names
  ([field-descriptor]
   [question-hakukohde-names field-descriptor :question-for-hakukohde])
  ([_ _]
   (let [auto-expand-hakukohteet @(re-frame/subscribe [:application/auto-expand-hakukohteet])
         show-hakukohde-list? (reagent/atom (boolean auto-expand-hakukohteet))]
     (fn [field-descriptor translation-key]
       (let [lang                           @(re-frame/subscribe [:application/form-language])
             selected-hakukohteet-for-field @(re-frame/subscribe [:application/selected-hakukohteet-for-field field-descriptor])]
         [:div.application__question_hakukohde_names_container
          [:div.application__question_hakukohde_names_belongs-to (str (tu/get-hakija-translation translation-key lang) " ")]
          (when @show-hakukohde-list?
            [:ul.application__question_hakukohde_names
             (for [hakukohde selected-hakukohteet-for-field
                   :let [name          (util/non-blank-val (:name hakukohde) [lang :fi :sv :en])
                         tarjoaja-name (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])]]
               [:li {:key (str (:id field-descriptor) "-" (:oid hakukohde))}
                name " - " tarjoaja-name])])
          [:a.application__question_hakukohde_names_info
           {:role         "button"
            :aria-pressed (str (boolean @show-hakukohde-list?))
            :on-click     #(swap! show-hakukohde-list? not)}
           (str (tu/get-hakija-translation
                  (if @show-hakukohde-list? :hide-application-options :show-application-options)
                  lang)
                " (" (count selected-hakukohteet-for-field) ")")]])))))
