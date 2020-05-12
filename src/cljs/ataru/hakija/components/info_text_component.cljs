(ns ataru.hakija.components.info-text-component
  (:require [ataru.application-common.application-field-common :as application-field]
            [ataru.util :as util]
            [re-frame.core :as re-frame]))

(defn info-text []
  (let [languages              (re-frame/subscribe [:application/default-languages])
        application-identifier (re-frame/subscribe [:application/application-identifier])]
    (fn [field-descriptor]
      (when-let [info (util/non-blank-val (-> field-descriptor :params :info-text :label) @languages)]
        [application-field/markdown-paragraph info (-> field-descriptor :params :info-text-collapse) @application-identifier]))))
