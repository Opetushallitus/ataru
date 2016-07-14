(ns ataru.schema.soresu-schema
  (:require [schema.core :as s]))

(defn create-form-schema [custom-wrapper-element-types custom-form-element-types custom-info-element-types]

  (let [default-form-element-types ["textField"
                                    "textArea"
                                    "nameField"
                                    "emailField"
                                    "moneyField"
                                    "finnishBusinessIdField"
                                    "iban"
                                    "bic"
                                    "dropdown"
                                    "radioButton"
                                    "checkboxButton"
                                    "namedAttachment"
                                    "koodistoField"]
        form-element-types (into custom-form-element-types default-form-element-types)
        default-wrapper-element-types ["theme" "fieldset" "growingFieldset" "growingFieldsetChild" ]
        wrapper-element-types (into custom-wrapper-element-types default-wrapper-element-types)
        all-answer-element-types (into form-element-types wrapper-element-types)
        default-info-element-types ["h1"
                                    "h3"
                                    "link"
                                    "p"
                                    "bulletList"
                                    "dateRange"
                                    "endOfDateRange"]
        info-element-types (into custom-info-element-types default-info-element-types)]))
