(ns ataru.hakija.arvosanat.components.lisaa-valinnaisaine-linkki
  (:require [ataru.schema.lang-schema :as lang-schema]
            [ataru.translations.translation-util :as translations]
            [ataru.application-common.components.link-component :as link-component]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [schema.core :as s]))

(def ^:private max-valinnaisaine-amount 3)

(defn- answered? [{:keys [value valid]}]
  (and valid
       (-> value string/blank? not)))

(s/defn lisaa-valinnaisaine-linkki
  [{:keys [valinnaisaine-rivi?
           arvosana-column
           oppimaara-column
           lang
           arvosana-idx
           field-descriptor
           row-count
           data-test-id]} :- {:valinnaisaine-rivi? s/Bool
                              :arvosana-column     s/Any
                              :oppimaara-column    (s/maybe s/Any)
                              :lang                lang-schema/Lang
                              :arvosana-idx        s/Int
                              :field-descriptor    s/Any
                              :row-count           s/Int
                              :data-test-id        s/Str}]
  (let [label               (if valinnaisaine-rivi?
                              (translations/get-hakija-translation :poista lang)
                              (translations/get-hakija-translation :lisaa-valinnaisaine lang))
        arvosana-answered?  (some-> (when arvosana-column
                                      @(re-frame/subscribe [:application/answer (:id arvosana-column) arvosana-idx]))
                                    answered?)
        oppimaara-answered? (or (nil? oppimaara-column)
                                (-> @(re-frame/subscribe [:application/answer (:id oppimaara-column) arvosana-idx])
                                    answered?))
        disabled?           (and (not valinnaisaine-rivi?)
                                 (or (not (< (dec row-count) max-valinnaisaine-amount))
                                     (not arvosana-answered?)
                                     (not oppimaara-answered?)))]
    [link-component/link
     (cond-> {:on-click  (fn add-or-remove-oppiaineen-valinnaisaine-row []
                           (if valinnaisaine-rivi?
                             (re-frame/dispatch [:application/remove-question-group-row field-descriptor arvosana-idx])
                             (re-frame/dispatch [:application/add-question-group-row field-descriptor])))
              :disabled? disabled?}
             data-test-id
             (assoc :data-test-id data-test-id))
     (if valinnaisaine-rivi?
       [:span label]
       [:<>
        [:i.zmdi.zmdi-plus-circle-o.arvosana__lisaa-valinnaisaine--ikoni]
        [:span.arvosana__lisaa-valinnaisaine--linkki label]])]))
