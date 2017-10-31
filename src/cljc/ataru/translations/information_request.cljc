(ns ataru.translations.information-request
  (:require [ataru.translations.common-translations :as t]))

(def translations (merge {:hello-text {:fi "Hei"
                                       :sv "Hej"
                                       :en "Hi"}}
                         (select-keys t/translations [:modify-link-text :do-not-share-warning-text])))

